package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MultithreadBMinHashOptimized extends BMinHashOpt {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHashOptimized");

    private final int mNumberOfThreads;
    private final double[] mSeedTime;

    /**
     * Creates a new MultithreadBMinHashOptimized instance with default values
     */
    public MultithreadBMinHashOptimized(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes, int threads) {
        super(g, numSeeds, threshold, nodes);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        mSeedTime = new double[mNumSeeds];
    }

    /**
     * Creates a new MultithreadBMinHashOptimized instance with default values
     */
    public MultithreadBMinHashOptimized(final ImmutableGraph g, int numSeeds, double threshold, int threads) {
        super(g, numSeeds, threshold);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        mSeedTime = new double[mNumSeeds];
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
    }

    /**
     * Number of max threads to use for the computation
     *
     * @param suggestedNumberOfThreads if not equal to zero return the number of threads
     *                                 passed as parameter, else the number of max threads available
     * @return number of threads to use for the computation
     */
    private static int getNumberOfMaxThreads(int suggestedNumberOfThreads) {
        if (suggestedNumberOfThreads > 0) return suggestedNumberOfThreads;
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Execution of the MultithreadBMinHash algorithm
     *
     * @return Computed metrics of the algorithm
     */

    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;

        logger.debug("Number of threads to be used {}", mNumberOfThreads);

        int[][] collisionsMatrix = new int[mNumSeeds][];
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;

        int lowerboundDiameter = 0;

        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<>(this.mNumSeeds);

        for (int i = 0; i < this.mNumSeeds; i++) {
            todo.add(new IterationThread(mGraph.copy(), i));
        }

        try {
            List<Future<int[]>> futures = executor.invokeAll(todo);
            for (int i = 0; i < this.mNumSeeds; i++) {
                Future<int[]> future = futures.get(i);
                if (!future.isCancelled()) {
                    try {
                        logger.debug("Starting computation of collision table on seed {}", i);
                        int[] hopCollisions = future.get();
                        collisionsMatrix[i] = hopCollisions;
                        int lastHop = hopCollisions.length - 1;
                        lastHops[i] = lastHop;
                        logger.debug("lastHop {} lastHops[{}] {} lowerbound {}", lastHop, i, lastHops[i], lowerboundDiameter);
                        if (lastHop > lowerboundDiameter) {
                            lowerboundDiameter = lastHop;
                        }
                        logger.debug("Collision table computation completed on seed {}!", i);

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

        normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter);

        logger.info("Starting computation of the hop table from collision table");
        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);
        logger.debug("Hop table derived from collision table: {}", hopTableArray);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt(hopTableArray, lowerboundDiameter, mThreshold);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setSeedsTime(mSeedTime);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTableArray));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTableArray, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTableArray));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTableArray, mThreshold));

        return graphMeasure;
    }

    class IterationThread implements Callable<int[]> {

        private final ImmutableGraph g;
        private final int index;

        public IterationThread(ImmutableGraph g, int index) {
            this.g = g;
            this.index = index;
        }

        @Override
        public int[] call() {
            logger.info("Starting computation on seed {}", index);
            long startSeedTime = System.nanoTime();

            int collisions = 0;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(g.numNodes())];
            int[] immutable = new int[lengthBitsArray(g.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = mMinHashNodeIDs[index];

            int h = 0;
            boolean signatureIsChanged = true;

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            int[] hopTable = new int[1];

            while (signatureIsChanged) {
                logger.debug("(seed {}) Starting computation on hop {}", index, h);

                //first hop - initialization
                if (h == 0) {

                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
                    signatureIsChanged = true;

                } else { // next hops
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
                        remainderPositionNode = (node << Constants.REMAINDER) >>> Constants.REMAINDER;
                        quotientNode = node >>> Constants.MASK;
                        int value = immutable[quotientNode];
                        int bitNeigh;
                        int nodeMask = (1 << remainderPositionNode);
                        if (((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                            for (int l = 0; l < d; l++) {
                                final int neighbour = successors[l];
                                int quotientNeigh = neighbour >>> Constants.MASK;
                                int remainderPositionNeigh = (neighbour << Constants.REMAINDER) >>> Constants.REMAINDER;

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


                // count the collision between the node signature and the graph signature
                if (signatureIsChanged) {
                    collisions = 0;
                    for (int aMutable : mutable) {
                        collisions += Integer.bitCount(aMutable);
                    }

                    int[] copy = new int[h + 1];
                    System.arraycopy(hopTable, 0, copy, 0, hopTable.length);
                    hopTable = copy;

                    hopTable[h] = collisions;

                    logger.debug("Number of collisions: {}", collisions);
                    h += 1;
                }
            }
            logger.info("Total number of collisions for seed n.{} : {}", index, collisions);

            double durationSeed = (System.nanoTime() - startSeedTime) / 1000000.0;
            MultithreadBMinHashOptimized.this.mSeedTime[index] = durationSeed;

            logger.debug("Seed # {} - Time {}", index, durationSeed);
            return hopTable;
        }


        private int lengthBitsArray(int numberOfNodes) {
            return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
        }

    }

}


