package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Implementation of MultithreadBMinHash (MinHash Signature Estimation multithread boolean optimized version) algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class MultithreadBMinHashOpt2 extends BMinHashOpt {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHashOpt2");

    private final int mNumberOfThreads;
    private final double[] mSeedTime;
    private long startTime;

    /**
     * Creates a new MultithreadBMinHashOptimized instance with default values
     */
    public MultithreadBMinHashOpt2(final CompressedGraph g, int numSeeds, double threshold, int[] nodes, int threads) {
        super(g, numSeeds, threshold, nodes);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        mSeedTime = new double[mNumSeeds];
    }

    /**
     * Creates a new MultithreadBMinHashOptimized instance with default values
     */
    public MultithreadBMinHashOpt2(final CompressedGraph g, int numSeeds, double threshold, int threads) {
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
        startTime = System.currentTimeMillis();
        long totalTime;

        logger.debug("Number of threads to be used {}", mNumberOfThreads);

        int[][] collisionsMatrix = new int[mNumSeeds][];
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;

        int lowerboundDiameter = 0;

        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<IterationThread2> todo = new ArrayList<>(this.mNumSeeds);

        for (int i = 0; i < this.mNumSeeds; i++) {
            // Scommenta e implementa

            //todo.add(new IterationThread2(mGraph.copy(), i));
        }

        try {
            List<Future<int[]>> futures = executor.invokeAll(todo);
            for (int i = 0; i < this.mNumSeeds; i++) {
                Future<int[]> future = futures.get(i);
                if (!future.isCancelled()) {
                    try {
                        int[] hopCollisions = future.get();
                        collisionsMatrix[i] = hopCollisions;
                        int lastHop = hopCollisions.length - 1;
                        lastHops[i] = lastHop;
                        if (lastHop > lowerboundDiameter) {
                            lowerboundDiameter = lastHop;
                        }

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

        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setLowerBoundDiameter(lowerboundDiameter);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setSeedsTime(mSeedTime);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTableArray));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTableArray, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTableArray));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTableArray, mThreshold));

        return graphMeasure;
    }

    class IterationThread2 implements Callable<int[]> {

        private final ImmutableGraph g;
        private final int s;

        public IterationThread2(ImmutableGraph g, int s) {
            this.g = g;
            this.s = s;
        }

        @Override
        public int[] call() {
            long startSeedTime = System.currentTimeMillis();
            long lastLogTime = startSeedTime;
            long logTime;

            int collisions = 0;

            int[] trackerMutable = new int[lengthBitsArray(g.numNodes())];
            int[] trackerImmutable = new int[lengthBitsArray(g.numNodes())];

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(g.numNodes())];
            int[] immutable = new int[lengthBitsArray(g.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = mMinHashNodeIDs[s];

            int h = 0;
            boolean signatureIsChanged = true;

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            int[] hopTable = new int[1];
            int value, remainderPositionNode, quotientNode, nodeMask, quotientNeigh, remainderPositionNeigh, neighMask;
            while (signatureIsChanged) {
                //first hop - initialization
                if (h == 0) {

                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
                    signatureIsChanged = true;

                    trackerMutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << ((randomNode << Constants.REMAINDER) >>> Constants.REMAINDER);

                } else { // next hops
                    signatureIsChanged = false;

                    for (int n = 0; n < g.numNodes(); n++) {

                        // update the node hash iterating over all its neighbors
                        // and computing the OR between the node signature and
                        // the neighbor signature.
                        // store the new signature as the current one
                        remainderPositionNode = (n << Constants.REMAINDER) >>> Constants.REMAINDER;
                        quotientNode = n >>> Constants.MASK;
                        nodeMask = (1 << remainderPositionNode);
                        value = immutable[quotientNode];

                        if (((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                            final int d = g.outdegree(n);
                            final int[] successors = g.successorArray(n);
                            int bitNeigh;

                            for (int l = 0; l < d; l++) {
                                final int neighbour = successors[l];
                                quotientNeigh = neighbour >>> Constants.MASK;
                                remainderPositionNeigh = (neighbour << Constants.REMAINDER) >>> Constants.REMAINDER;
                                neighMask = (Constants.BIT << remainderPositionNeigh);

                                if (((neighMask & trackerImmutable[quotientNeigh]) >>> remainderPositionNeigh) == 1) {
                                    bitNeigh = (((1 << remainderPositionNeigh) & immutable[quotientNeigh]) >>> remainderPositionNeigh) << remainderPositionNode;
                                    value = bitNeigh | nodeMask & immutable[quotientNode];
                                    if ((value >>> remainderPositionNode) == 1) {
                                        signatureIsChanged = true;
                                        trackerMutable[quotientNode] |= (Constants.BIT) << remainderPositionNode;
                                        break;
                                    }
                                }
                            }
                        }
                        mutable[quotientNode] = mutable[quotientNode] | value;


                        logTime = System.currentTimeMillis();
                        if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                            logger.info("(seed # {}) # nodes analyzed {} / {} for hop {}, estimated time remaining {}",
                                    s,
                                    n, mGraph.numNodes(),
                                    h + 1,
                                    String.format("%d min, %d sec",
                                            TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - MultithreadBMinHashOpt2.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHashOpt2.this.startTime)),
                                            TimeUnit.MILLISECONDS.toSeconds(((mNumSeeds * (logTime - MultithreadBMinHashOpt2.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHashOpt2.this.startTime)) -
                                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - MultithreadBMinHashOpt2.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHashOpt2.this.startTime)))));
                            lastLogTime = logTime;
                        }
                    }
                }


                // count the collision between the node signature and the graph signature
                if (signatureIsChanged) {
                    System.arraycopy(trackerMutable, 0, trackerImmutable, 0, trackerMutable.length);
                    trackerMutable = new int[lengthBitsArray(mGraph.numNodes())];
                    // copy all the actual nodes hash in a new structure
                    System.arraycopy(mutable, 0, immutable, 0, mutable.length);

                    collisions = 0;
                    for (int aMutable : mutable) {
                        collisions += Integer.bitCount(aMutable);
                    }

                    int[] copy = new int[h + 1];
                    System.arraycopy(hopTable, 0, copy, 0, hopTable.length);
                    hopTable = copy;

                    hopTable[h] = collisions;

                    h += 1;
                }
            }

            MultithreadBMinHashOpt2.this.mSeedTime[s] = System.currentTimeMillis() - startSeedTime;

            return hopTable;
        }


        private int lengthBitsArray(int numberOfNodes) {
            return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
        }
    }
}


