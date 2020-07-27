package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;
import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongSortedMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class MultithreadBMinHash extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHash");

    private static final int MASK = 5; // 2^6
    private static final int REMAINDER = 27;
    private static final int BIT = 1;

    private Int2LongSortedMap mTotalCollisions;
    private int mNumberOfThreads;
    private HashMap<Integer, Double> mSeedTime;
    private final Object mLock = new Object();

    /**
     * Creates a new BooleanMinHash instance with default values
     */
    public MultithreadBMinHash() throws DirectionNotSetException, SeedsException, IOException {
        super();
        mSeedTime = new HashMap();
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("minhash.suggestedNumberOfThreads"));
        logger.info("Number of threads selected {}", suggestedNumberOfThreads);

        if(isSeedsRandom){
            for(int i = 0;i<numSeeds; i++){
                minHashNodeIDs[i] = ThreadLocalRandom.current().nextInt(0, mGraph.numNodes());
            }
        } else {
            //Load minHash node IDs from properties file
            String propertyName = "minhash.nodeIDs";
            String minHashNodeIDsString = PropertiesManager.getProperty(propertyName);
            minHashNodeIDs = Arrays.stream(minHashNodeIDsString.split(",")).mapToInt(Integer::parseInt).toArray();
            if (numSeeds != minHashNodeIDs.length) {
                String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + numSeeds + " and \"" + propertyName + "\" length is " + minHashNodeIDs.length;
                throw new SeedsException(message);
            }
        }

        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
        this.mNumberOfThreads = getNumberOfMaxThreads(suggestedNumberOfThreads);
        mTotalCollisions = new Int2LongLinkedOpenHashMap();
    }


    /**
     * Execution of the MultithreadBMinHash algorithm
     * @return Computed metrics of the algorithm
     */

    public GraphMeasure runAlgorithm() {
        logger.debug("Number of threads to be used {}", mNumberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<IterationThread>(this.numSeeds);

        for(int i = 0; i < this.numSeeds; i++) {
            todo.add(new IterationThread(mGraph, i));
        }

        try {
            List<Future<Int2LongLinkedOpenHashMap>> futures = executor.invokeAll(todo);
            int count = 0;
            int lowerboundDiameter = 0;
            for (Future<Int2LongLinkedOpenHashMap> future : futures) {
                if (!future.isCancelled()) {
                    try {
                        Int2LongLinkedOpenHashMap actualCollisionTable = future.get();
                        logger.debug("Starting computation of collision table on seed {}", count);

                        //Recreating mTotalCollisions starting from tables of each thread
                        int lastElementIndex = mTotalCollisions.size()-1;
                        long lastElement = mTotalCollisions.get(lastElementIndex);

                        //Last hop of the actualCollisionTable has NOT to be considered
                        //because has the same number of collisions of the previous one
                        //So the last element to be considered in actualCollisionTable is:
                        //actualCollisionTable.get(actualCollisionTable.size()-2)
                        int lastElementActualCollisionTableIndex = actualCollisionTable.size()-2;
                        long lastElementActualCollisionTable = actualCollisionTable.get(lastElementActualCollisionTableIndex);

                        if ((lastElementActualCollisionTableIndex) > lowerboundDiameter){
                            lowerboundDiameter = lastElementActualCollisionTableIndex;
                        }

                        if((actualCollisionTable.size() - 1) <= mTotalCollisions.size()) {
                            for(int k = 0; k < actualCollisionTable.size() - 1; k++) {
                                long sumCollisions = mTotalCollisions.get(k) + actualCollisionTable.get(k);
                                mTotalCollisions.put(k, sumCollisions);
                            }
                            for(int k = actualCollisionTable.size() - 1; k < mTotalCollisions.size(); k++) {
                                long sumCollisions = mTotalCollisions.get(k) + lastElementActualCollisionTable;
                                mTotalCollisions.put(k, sumCollisions);
                            }
                        } else {
                            for(int k = 0; k < mTotalCollisions.size(); k++) {
                                long sumCollisions = mTotalCollisions.get(k) + actualCollisionTable.get(k);
                                mTotalCollisions.put(k, sumCollisions);
                            }
                            for(int k = mTotalCollisions.size(); k < actualCollisionTable.size() - 1; k++) {
                                long sumCollisions = lastElement + actualCollisionTable.get(k);
                                mTotalCollisions.put(k, sumCollisions);
                            }
                        }
                        logger.debug("Collision table computation completed on seed {}!", count);
                        count++;

                    } catch (ExecutionException e) {
                        logger.error("Failed to get result", e);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                } else {
                    //TODO Implement better error management
                    logger.error("Future is cancelled!");
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        hopTable = hopTable();
        logger.debug("Hop table derived from collision table: {}", hopTable);

        GraphMeasure graphMeasure = new GraphMeasure(hopTable);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setNumSeeds(mSeeds.size());
        graphMeasure.setSeedsTime(mSeedTime);

        String minHashNodeIDsString = "";
        String separator = ",";
        for(int i=0;i<numSeeds;i++){
            minHashNodeIDsString += (minHashNodeIDs[i] + separator);
        }
        graphMeasure.setMinHashNodeIDs(minHashNodeIDsString);
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




    class IterationThread implements Callable<Int2LongLinkedOpenHashMap> {

        private ImmutableGraph g;
        private int index;

        public IterationThread(ImmutableGraph g, int index) {
            this.g = g;
            this.index = index;
        }

        @Override
        public Int2LongLinkedOpenHashMap call() {
            logger.info("Starting computation on seed {}", index);
            long startSeedTime = System.nanoTime();


            Int2LongLinkedOpenHashMap hopCollision;
            int counter;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(g.numNodes())];
            int[] immutable = new int[lengthBitsArray(g.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = minHashNodeIDs[index];

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

            while(hopCollision.getOrDefault(h, 0) != hopCollision.getOrDefault(h-1, 0)) {
                h += 1;
                logger.debug("(seed {}) Starting computation on hop {}", index, h);

                // copy all the actual nodes hash in a new structure
                System.arraycopy(mutable, 0, immutable, 0, mutable.length);

                hopCollision.put(h, 0);

                int remainderPositionNode;
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
                    int value = immutable[quotientNode];
                    int bitNeigh;
                    int nodeMask = (1 << remainderPositionNode);

                    if(((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                        for (int l = 0; l < d; l++) {
                            final int neighbour = successors[l];
                            int quotientNeigh = neighbour >>> MASK;
                            long remainderPositionNeigh = (neighbour << REMAINDER) >>> REMAINDER;
                            bitNeigh = (((1 << remainderPositionNeigh) & immutable[quotientNeigh]) >>> remainderPositionNeigh) << remainderPositionNode;
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
                    counter += Integer.bitCount(mutable[c]);
                }

                hopCollision.put(h, counter);
                logger.debug("(seed {}) Hop Collision {}", index, hopCollision);
            }
            double durationSeed = (System.nanoTime() - startSeedTime) / 1000000.0;
            MultithreadBMinHash.this.mSeedTime.put(index, durationSeed);
            logger.debug("Seed # {} - Time {}", index, durationSeed);
            return hopCollision;
        }


        private int lengthBitsArray(int numberOfNodes) {
            return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
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


