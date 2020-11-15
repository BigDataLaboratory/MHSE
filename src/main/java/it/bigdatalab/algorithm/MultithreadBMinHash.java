package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class MultithreadBMinHash extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHash");

    private int mNumberOfThreads;
    private double[] mSeedTime;

    /**
     * Creates a new MultithreadBMinHash instance with default values
     */
    public MultithreadBMinHash(String inputFilePath, boolean isSeedsRandom, boolean isolatedVertices, String direction, int numSeeds, double threshold) throws DirectionNotSetException, SeedsException, IOException {
        super(inputFilePath, isolatedVertices, direction, numSeeds, threshold);

        mSeedTime = new double[mNumSeeds];
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("minhash.suggestedNumberOfThreads"));
        logger.info("Number of threads selected {}", suggestedNumberOfThreads);

        if (isSeedsRandom) {
            for (int i = 0; i < mNumSeeds; i++) {
                mMinHashNodeIDs[i] = ThreadLocalRandom.current().nextInt(0, mGraph.numNodes());
            }
        } else {
            //todo move reading property in MinHashMain
            //Load minHash node IDs from properties file
            String propertyNodeIDRange = "minhash.nodeIDRange";
            String minHashNodeIDRangeString = PropertiesManager.getProperty(propertyNodeIDRange);
            if (!minHashNodeIDRangeString.equals("")) {
                int[] minHashNodeIDRange = Arrays.stream(minHashNodeIDRangeString.split(",")).mapToInt(Integer::parseInt).toArray();
                mMinHashNodeIDs = IntStream.rangeClosed(minHashNodeIDRange[0], minHashNodeIDRange[1]).toArray();
            }
            if (mNumSeeds != mMinHashNodeIDs.length) {
                String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + mNumSeeds + " and length is " + mMinHashNodeIDs.length;
                throw new SeedsException(message);
            }
        }

        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
        this.mNumberOfThreads = getNumberOfMaxThreads(suggestedNumberOfThreads);
    }


    /**
     * Execution of the MultithreadBMinHash algorithm
     * @return Computed metrics of the algorithm
     */

    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;

        Int2ObjectOpenHashMap<int[]> collisionsTable = new Int2ObjectOpenHashMap<>();
        int[] lastHops = new int[mNumSeeds];

        logger.debug("Number of threads to be used {}", mNumberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<>(this.mNumSeeds);

        for (int i = 0; i < this.mNumSeeds; i++) {
            todo.add(new IterationThread(mGraph, i));
        }

        try {
            List<Future<Int2LongLinkedOpenHashMap>> futures = executor.invokeAll(todo);
            for (int i = 0; i < this.mNumSeeds; i++) {
                Future<Int2LongLinkedOpenHashMap> future = futures.get(i);
                if (!future.isCancelled()) {
                    try {
                        Int2LongLinkedOpenHashMap actualCollisionTable = future.get();
                        int[] hopCollision;

                        for (int h : actualCollisionTable.keySet()) {
                            if (!collisionsTable.containsKey(h)) {
                                hopCollision = new int[mNumSeeds];
                                hopCollision[i] = (int) actualCollisionTable.get(h);
                                collisionsTable.put(h, hopCollision);
                            } else {
                                hopCollision = collisionsTable.get(h);
                                hopCollision[i] = (int) actualCollisionTable.get(h);
                                collisionsTable.put(h, hopCollision);
                            }
                        }

                        lastHops[i] = actualCollisionTable.size() - 1;

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

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);


        collisionsTable = normalizeCollisionsTable(collisionsTable);

        Int2DoubleLinkedOpenHashMap hopTable = hopTable(collisionsTable);
        logger.debug("Hop table derived from collision table: {}", hopTable);

        GraphMeasure graphMeasure = new GraphMeasure(hopTable, mThreshold);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsTable(collisionsTable);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setSeedsTime(mSeedTime);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(getNodes());
        graphMeasure.setDirection(mDirection);

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

    /***
     * TODO Optimizable?
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    private Int2ObjectOpenHashMap<int[]> normalizeCollisionsTable(Int2ObjectOpenHashMap<int[]> ct) {
        int lowerBoundDiameter = ct.size() - 1;
        logger.debug("Diameter: " + lowerBoundDiameter);

        //Start with hop 1
        //There is no check for hop 0 because at hop 0 there is always (at least) 1 collision, never 0.
        for (int i = 1; i <= lowerBoundDiameter; i++) {
            int[] previousHopCollisions = ct.get(i - 1);
            int[] hopCollisions = ct.get(i);
            //TODO first if is better for performance?
            if (Arrays.stream(hopCollisions).anyMatch(coll -> coll == 0)) {
                for (int j = 0; j < hopCollisions.length; j++) {
                    if (hopCollisions[j] == 0) {
                        hopCollisions[j] = previousHopCollisions[j];
                    }
                }
            }
            ct.put(i, hopCollisions);
        }
        return ct;
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    private Int2DoubleLinkedOpenHashMap hopTable(Int2ObjectOpenHashMap<int[]> ct) {
        Int2DoubleLinkedOpenHashMap hopTable = new Int2DoubleLinkedOpenHashMap();
        int lastHop = ct.size() - 1;
        long sumCollisions = 0;

        for (int hop = 0; hop <= lastHop; hop++) {
            int[] collisions = ct.get(hop);
            sumCollisions = Arrays.stream(collisions).sum();
            double couples = (double) (sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hopTable.put(hop, couples);
            logger.info("hop " + hop + " total collisions " + Arrays.stream(collisions).sum() + " couples: " + couples);
        }
        return hopTable;
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
            int collisions;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(g.numNodes())];
            int[] immutable = new int[lengthBitsArray(g.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = mMinHashNodeIDs[index];

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            hopCollision = new Int2LongLinkedOpenHashMap();
            int h = 0;
            boolean signatureIsChanged = true;

            while (signatureIsChanged) {

                logger.debug("(seed {}) Starting computation on hop {}", index, h);

                //first hop - initialization
                if (h == 0) {
                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = ((randomNode << Constants.MULTITHREAD_REMAINDER) >>> Constants.MULTITHREAD_REMAINDER);
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> Constants.MULTITHREAD_MASK] |= (Constants.BIT) << remainderPositionRandomNode;
                    signatureIsChanged = true;
                } else {
                    signatureIsChanged = false;

                    // copy all the actual nodes hash in a new structure
                    System.arraycopy(mutable, 0, immutable, 0, mutable.length);
                    int remainderPositionNode;
                    int quotientNode;
                    for (int n = 0; n < g.numNodes(); n++) {

                        final int node = n;
                        final int d = g.outdegree(node);
                        final int[] successors = g.successorArray(node);

                        // update the node hash iterating over all its neighbors
                        // and computing the OR between the node signature and
                        // the neighbor signature.
                        // store the new signature as the current one
                        remainderPositionNode = (node << Constants.MULTITHREAD_REMAINDER) >>> Constants.MULTITHREAD_REMAINDER;
                        quotientNode = node >>> Constants.MULTITHREAD_MASK;
                        int value = immutable[quotientNode];
                        int bitNeigh;
                        int nodeMask = (1 << remainderPositionNode);

                        if (((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                            for (int l = 0; l < d; l++) {
                                final int neighbour = successors[l];
                                int quotientNeigh = neighbour >>> Constants.MULTITHREAD_MASK;
                                long remainderPositionNeigh = (neighbour << Constants.MULTITHREAD_REMAINDER) >>> Constants.MULTITHREAD_REMAINDER;
                                bitNeigh = (((1 << remainderPositionNeigh) & immutable[quotientNeigh]) >>> remainderPositionNeigh) << remainderPositionNode;
                                value = bitNeigh | nodeMask & immutable[quotientNode];
                                if ((value >>> remainderPositionNode) == 1) {
                                    signatureIsChanged = true;
                                    break;
                                }
                            }
                        }
                        mutable[quotientNode] = mutable[quotientNode] | value;

                    }
                }

                if (signatureIsChanged) {
                    // count the collision between the node signature and the graph signature
                    collisions = 0;
                    for (int aMutable : mutable) {
                        collisions += Integer.bitCount(aMutable);
                    }
                    hopCollision.put(h, collisions);
                    h += 1;
                }

                logger.debug("(seed {}) Hop Collision {}", index, hopCollision);
            }

            double durationSeed = (System.nanoTime() - startSeedTime) / 1000000.0;
            MultithreadBMinHash.this.mSeedTime[index] = durationSeed;
            logger.debug("Seed # {} - Time {}", index, durationSeed);
            return hopCollision;
        }


        private int lengthBitsArray(int numberOfNodes) {
            return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
        }

    }

}


