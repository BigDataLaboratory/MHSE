package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class MultithreadBMinHash extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHash");

    private static final int MASK = 6; // 2^6
    private static final int REMAINDER = 58;
    private static final long BIT = 1;

    private Int2LongSortedMap mTotalCollisions;
    private int mNumberOfThreads;
    private final Object mLock = new Object();
    private Int2ObjectLinkedOpenHashMap<Int2LongLinkedOpenHashMap> finalTotalCollision = new Int2ObjectLinkedOpenHashMap<Int2LongLinkedOpenHashMap>(numSeeds);

    /**
     * Creates a new BooleanMinHash instance with default values
     */
    public MultithreadBMinHash() throws DirectionNotSetException, SeedsException, IOException {
        super();
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("minhash.suggestedNumberOfThreads"));
        logger.info("Number of threads selected {}", suggestedNumberOfThreads);

        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
        this.mNumberOfThreads = getNumberOfMaxThreads(suggestedNumberOfThreads);
        mTotalCollisions = new Int2LongLinkedOpenHashMap();
    }


    public GraphMeasure runAlgorithm() {
        logger.debug("Number of threads to be used {}", mNumberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<Callable<Object>> todo = new ArrayList<>(this.numSeeds);

        for(int i = 0; i < this.numSeeds; i++) {
            todo.add(Executors.callable(new IterationThread(mGraph.copy(), i)));
        }
        try {
            executor.invokeAll(todo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executor.shutdown();

        //Recreating mTotalCollisions starting from tables of each seed
        for(int i=0;i<finalTotalCollision.size();i++){
            long lastElement = mTotalCollisions.get(mTotalCollisions.size()-1);
            logger.debug("lastElement is: " + lastElement);
            Int2LongLinkedOpenHashMap actualCollisionTable = finalTotalCollision.get(i);
            if(actualCollisionTable.size() <= mTotalCollisions.size()) {
                for(int k = 0; k < actualCollisionTable.size(); k++) {
                    long sumCollisions = mTotalCollisions.get(k) + actualCollisionTable.get(k);
                    mTotalCollisions.put(k, sumCollisions);
                }
                for(int k = actualCollisionTable.size(); k < mTotalCollisions.size(); k++) {
                    long sumCollisions = mTotalCollisions.get(k) + actualCollisionTable.get(actualCollisionTable.size()-1);
                    mTotalCollisions.put(k, sumCollisions);
                }
            } else {
                for(int k = 0; k < mTotalCollisions.size(); k++) {
                    long sumCollisions = mTotalCollisions.get(k) + actualCollisionTable.get(k);
                    mTotalCollisions.put(k, sumCollisions);
                }
                for(int k = mTotalCollisions.size(); k < actualCollisionTable.size(); k++) {
                    long sumCollisions = lastElement + actualCollisionTable.get(k);
                    mTotalCollisions.put(k, sumCollisions);
                }
            }

        }

        hopTable = hopTable();
        logger.debug("Hop table derived from collision table: {}", hopTable);

        GraphMeasure graphMeasure = new GraphMeasure(hopTable);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        return graphMeasure;
    }


    /**
     * Number of max threads to use for the computation
     * @param suggestedNumberOfThreads if not equal to zero return the number of threads
     *                                 passed as parameter, else the number of max threads available
     * @return number of threads to use for the computation
     */
    private final int getNumberOfMaxThreads(int suggestedNumberOfThreads) {
        if(suggestedNumberOfThreads != 0) return suggestedNumberOfThreads;
        return Runtime.getRuntime().availableProcessors();
    }

    class IterationThread implements Runnable {

        private ImmutableGraph g;
        private int index;

        public IterationThread(ImmutableGraph g, int index) {
            this.g = g;
            this.index = index;
        }

        @Override
        public void run() {
            logger.info("Starting computation on seed {}", index);

            Int2LongLinkedOpenHashMap hopCollision;
            int counter;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            long[] mutable = new long[lengthBitsArray(g.numNodes())];
            long[] immutable = new long[lengthBitsArray(g.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            int randomNode = ThreadLocalRandom.current().nextInt(0, g.numNodes());
//            int randomNode = 0;

            // take a long number, if we divide it to a number power of 2, quotient is in the first 6 bit, remainder
            // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
            // This is equal to logical and operation.
            int remainderPositionRandomNode = ((randomNode << REMAINDER) >>> REMAINDER);
            // quotient: randomNode >>> MASK
            mutable[randomNode >>> MASK] |= (BIT) << remainderPositionRandomNode;

            int h = 0;

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            hopCollision = new Int2LongLinkedOpenHashMap();
            hopCollision.put(h, 1);

            while(hopCollision.get(h) != hopCollision.getOrDefault(h-1,0)) {
                h += 1;
                logger.debug("(seed {}) Starting computation on hop {}", index, h);

                // copy all the actual nodes hash in a new structure
                System.arraycopy(mutable, 0, immutable, 0, mutable.length);

                hopCollision.put(h, 0);

                long remainderPositionNode;
                int quotientNode;
                for(int n = 0; n < g.numNodes(); n++) {

                    final int node = n;

                    final int d = g.outdegree(node);
                    final int[] successors = g.successorArray(node);

                    // update the node hash iterating over all its neighbors
                    // and computing the OR between the node signature and
                    // the neighbor signature.
                    // store the new signature as the current one
                    remainderPositionNode = (node << REMAINDER) >>> REMAINDER;
                    quotientNode = node >>> MASK;

                    long value = immutable[quotientNode];
                    long bitNeigh;
                    long nodeMask = (1L << remainderPositionNode);

                    if(((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                        for (int l = 0; l < d; l++) {
                            final int neighbour = successors[l];


                            int quotientNeigh = neighbour >>> MASK;
                            long remainderPositionNeigh = (neighbour << REMAINDER) >>> REMAINDER;

                            bitNeigh = (((1L << remainderPositionNeigh) & immutable[quotientNeigh]) >>> remainderPositionNeigh) << remainderPositionNode;
                            value = bitNeigh | nodeMask & immutable[quotientNode];
                            if ((value >>> remainderPositionNode) == 1) {
                                break;
                            }
                        }
                    }
                    mutable[quotientNode] = mutable[quotientNode] | value;
                }

                // count the collision between the node signature and the graph signature
                counter = 0;
                for(int c = 0; c < mutable.length; c++) {
                    counter += Long.bitCount(mutable[c]);
                }
                hopCollision.put(h, counter);
                logger.debug("(seed {}) Hop Collision {}", index, hopCollision);

            }
            finalTotalCollision.put(index,hopCollision);
        }


        private int lengthBitsArray(int numberOfNodes) {
            return (int) Math.ceil(numberOfNodes/ (double) Long.SIZE);
        }

    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */

    private Int2DoubleSortedMap hopTable() {
        Int2DoubleSortedMap hopTable = new Int2DoubleLinkedOpenHashMap();
        mTotalCollisions.forEach((key, value) -> {
            Double r = ((double) (value * mGraph.numNodes()) / this.numSeeds);
            hopTable.put(key, r);
        });
        return hopTable;
    }

}


