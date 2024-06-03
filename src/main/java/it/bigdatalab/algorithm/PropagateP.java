package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class PropagateP extends BMinHashOpt {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.PropagateP");

    private final int mNumberOfThreads;
    private final boolean mDoCentrality;
    private int mTasksSize;
    private long[][] mHopForNodes;

    private double[][] mHarmonic;

    /**
     * Creates a new PropagateP instance with default values
     */
    public PropagateP(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes, int threads, boolean centrality) throws SeedsException {
        super(g, numSeeds, threshold, nodes);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mDoCentrality = centrality;
    }

    /**
     * Creates a new PropagateP instance with default values
     */
    public PropagateP(final ImmutableGraph g, int numSeeds, double threshold, int threads, boolean centrality) throws SeedsException {
        super(g, numSeeds, threshold);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mDoCentrality = centrality;
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

    public int lengthBitsArray(int value) {
        return (int) Math.ceil(value / (double) Integer.SIZE);
    }

    public int groupSeedsByThread(int t) {
        return Math.min((t + 1) * mTasksSize, mNumSeeds);
    }

    @Override
    public Measure runAlgorithm() throws IOException {
        long startTime = System.currentTimeMillis();
        int d = mNumSeeds / mNumberOfThreads;
        int r = mNumSeeds % mNumberOfThreads;
        int ntasks = (d == 0) ? r : mNumberOfThreads;
        mTasksSize = (int) Math.ceil((double) mNumSeeds / ntasks);
        long totalTime;
        long[][] collisions = new long[ntasks][];
        mHopForNodes = new long[ntasks][];
        mHarmonic = new double[ntasks][];
        double[] hopTableArray;
        int lowerboundDiameter = 0;
        // Centrality measures for each thread
        if (mDoCentrality) {
            mHopForNodes = new long[ntasks][mGraph.numNodes()];
            mHarmonic = new double[ntasks][mGraph.numNodes()];
        }

        ExecutorService executor = Executors.newFixedThreadPool(ntasks); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<>(ntasks);
        for (int t = 0; t < ntasks; t++) {
            int start = t * mTasksSize;
            int end = groupSeedsByThread(t);

            todo.add(new IterationThread(mGraph.copy(), start, end, t));
        }


        try {
            List<Future<long[]>> futures = executor.invokeAll(todo);
            for (int i = 0; i < ntasks; i++) {
                Future<long[]> future = futures.get(i);
                if (!future.isCancelled()) {
                    try {
                        long[] hopCollisions = future.get();
                        collisions[i] = hopCollisions;
                        int lastHop = hopCollisions.length - 1;
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

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        // Reduction phase
        double[] harmonic = new double[0];
        double[] farness = new double[0];
        if (mDoCentrality) {
            harmonic = new double[mGraph.numNodes()];
            farness = new double[mGraph.numNodes()];
        }
        for (int i = 0; i < mGraph.numNodes(); i++) {
            for (int j = 0; j < ntasks; j++) {
                if (mDoCentrality) {
                    harmonic[i] += mHarmonic[j][i];
                    farness[i] += mHopForNodes[j][i];
                }

            }
            if (mDoCentrality) {
                farness[i] = farness[i] * ((double) mGraph.numNodes() / mNumSeeds);
                harmonic[i] = harmonic[i] * ((double) mGraph.numNodes() / (mGraph.numNodes() - 1) / mNumSeeds);
            }
        }

        normalizeCollisionsTable(collisions, lowerboundDiameter);
        hopTableArray = hopTable(collisions, lowerboundDiameter);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setLowerBoundDiameter(lowerboundDiameter);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setSeedsList(mSeeds);
        if (mDoCentrality) {
            graphMeasure.setFarness(farness);
            graphMeasure.setInverseFarness(harmonic);
        }
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTableArray));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTableArray, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTableArray));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTableArray, mThreshold));
        return graphMeasure;
    }


    public void normalizeCollisionsTable(long[][] collisionsMatrix, int lowerBound) {

        for (int i = 0; i < collisionsMatrix.length; i++) { // check last hop of each seed
            // if last hop is not the lower bound
            // replace the 0 values from last hop + 1 until lower bound
            // with the value of the previous hop for the same seed
            if (collisionsMatrix[i].length - 1 < lowerBound) {
                int oldLen = collisionsMatrix[i].length;
                long[] copy = new long[lowerBound + 1];
                System.arraycopy(collisionsMatrix[i], 0, copy, 0, collisionsMatrix[i].length);
                collisionsMatrix[i] = copy;
                for (int j = oldLen; j <= lowerBound; j++) {
                    collisionsMatrix[i][j] = collisionsMatrix[i][j - 1];
                }
            }
        }
    }

    public double[] hopTable(long[][] collisionsMatrix, int lowerBound) {
        long sumCollisions;
        double couples;
        double[] hoptable = new double[lowerBound + 1];
        // lower bound is the max size of inner array
        for (int hop = 0; hop < lowerBound + 1; hop++) {
            sumCollisions = 0;
            for (long[] matrix : collisionsMatrix) {
                sumCollisions += matrix[hop];
            }
            couples = ((double) sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hoptable[hop] = couples;

        }
        return hoptable;
    }

    class IterationThread implements Callable<long[]> {
        private final ImmutableGraph g;
        private final int end;
        private final int tStart;
        private final int index;
        private final boolean[] saturated;
        private final int[] trackerImmutable;
        private final int[][] signMutable;
        private final int[][] signImmutable;
        private int[] trackerMutable;

        public IterationThread(ImmutableGraph g, int start, int end, int index) {
            this.g = g;
            this.tStart = start;
            this.index = index;
            this.end = end - start;
            this.saturated = new boolean[g.numNodes()];
            this.trackerMutable = new int[lengthBitsArray(g.numNodes())];
            this.trackerImmutable = new int[lengthBitsArray(g.numNodes())];
            this.signMutable = new int[g.numNodes()][lengthBitsArray(this.end + 1)];
            this.signImmutable = new int[g.numNodes()][lengthBitsArray(this.end + 1)];

            Arrays.fill(saturated, Boolean.FALSE);
        }


        @Override
        public long[] call() throws Exception {
            long startTime = System.currentTimeMillis();
            long totalTime;

            long[] collisionsVector = new long[1];
            int[] position = new int[this.end];
            int[] remainder = new int[this.end];
            int nPosition, nRemainder, neighPosition, neighRemainder, neighMask;

            boolean signatureIsChanged = true;
            int h = 0;
            while (signatureIsChanged) {

                if (h == 0) {
                    for (int s = 0; s < this.end; s++) {
                        int seedOriginal = s + this.tStart;
                        position[s] = s >>> Constants.MASK;
                        remainder[s] = (s << Constants.REMAINDER) >>> Constants.REMAINDER;
                        signMutable[mMinHashNodeIDs[seedOriginal]][position[s]] |= (Constants.BIT) << remainder[s];
                        trackerMutable[mMinHashNodeIDs[seedOriginal] >>> Constants.MASK] |= (Constants.BIT) << ((mMinHashNodeIDs[seedOriginal] << Constants.REMAINDER) >>> Constants.REMAINDER);
                    }
                } else {
                    signatureIsChanged = false;

                    // update node signature
                    for (int n = 0; n < g.numNodes(); n++) {
                        if (!saturated[n]) {
                            final int d = g.outdegree(n);
                            final int[] successors = g.successorArray(n);

                            nPosition = n >>> Constants.MASK;
                            nRemainder = (n << Constants.REMAINDER) >>> Constants.REMAINDER;
                            // for each neigh of the node n
                            for (int l = d; l-- != 0; ) {
                                // check if the neigh has been modified
                                // in the previous hop. If true, it can modify
                                // the node n
                                neighPosition = successors[l] >>> Constants.MASK;
                                neighRemainder = (successors[l] << Constants.REMAINDER) >>> Constants.REMAINDER;
                                neighMask = (Constants.BIT << neighRemainder);

                                if (((neighMask & trackerImmutable[neighPosition]) >>> neighRemainder) == 1) {
                                    // for each element of the signature of the node n
                                    int sMask;
                                    boolean tmp_saturated = true;

                                    for (int s = 0; s < this.end; s++) {
                                        sMask = (Constants.BIT << remainder[s]);

                                        // check if the s-th element of the node n signature
                                        // it's 0, else jump to the next s-th element of the signature
                                        if (((sMask & signMutable[n][position[s]]) >>> remainder[s]) == 0) {
                                            int bitNeigh;
                                            int value;
                                            // change the s-th element of the node n signature
                                            // only if the s-th element of the neigh signature is 1
                                            if (((sMask & signImmutable[successors[l]][position[s]]) >>> remainder[s]) == 1) {
                                                bitNeigh = (((1 << remainder[s]) & signImmutable[successors[l]][position[s]]) >>> remainder[s]) << remainder[s];
                                                value = bitNeigh | sMask & signImmutable[successors[l]][position[s]];
                                                signatureIsChanged = true; // track the signature changes, to run the next hop
                                                trackerMutable[nPosition] |= (Constants.BIT) << nRemainder;
                                                signMutable[n][position[s]] = signMutable[n][position[s]] | value;
                                                if (signatureIsChanged) {
                                                    if (mDoCentrality) {
                                                        mHopForNodes[this.index][n] += h;
                                                        mHarmonic[this.index][n] += 1.0 / h;
                                                    }
                                                }
                                            }
                                            if (((sMask & signMutable[n][position[s]]) >>> remainder[s]) == 0) {
                                                tmp_saturated = false;
                                            }

                                        } // else is already 1

                                    }
                                    saturated[n] = tmp_saturated;


                                }
                            }
                        }

                    }
                }
                if (signatureIsChanged) {
                    System.arraycopy(trackerMutable, 0, trackerImmutable, 0, trackerMutable.length);
                    trackerMutable = new int[lengthBitsArray(g.numNodes())];

                    // count the collisions of the signatures
                    long collisions = 0;
                    for (int r = 0; r < g.numNodes(); r++) {
                        System.arraycopy(signMutable[r], 0, signImmutable[r], 0, signMutable[r].length);
                        for (int c = 0; c < signMutable[r].length; c++) {
                            collisions += Integer.bitCount(signMutable[r][c]);
                        }
                    }
                    long[] copy = new long[h + 1];
                    System.arraycopy(collisionsVector, 0, copy, 0, collisionsVector.length);
                    collisionsVector = copy;
                    collisionsVector[h] = collisions;
                    h += 1;
                }
            }

            totalTime = System.currentTimeMillis() - startTime;
            logger.info("Thread # {} completed. Time elapsed to complete computation {} s.",
                    this.index,
                    (totalTime) / (double) 1000);

            return collisionsVector;
        }
    }
}
