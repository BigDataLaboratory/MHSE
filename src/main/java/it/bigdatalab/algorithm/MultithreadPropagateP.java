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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Implementation of Propagate-P algorithm
 */
public class MultithreadPropagateP extends MinHash {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadPropagateP");

    private final int mNumberOfThreads;
    private int[] mPosition;
    private int[] mRemainder;
    private int[] mTrackerMutable;
    private int[] mTrackerImmutable;
    private int[][] mSignMutable;
    private int[][] mSignImmutable;
    private CyclicBarrier mCyclicBarrier;
    private int h;

    private long[] mCollisionsVector;
    private int mSignatureIsChanged;
    private ReentrantLock mLock;

    /**
     * Creates a new Propagate-P instance with default values
     */
    public MultithreadPropagateP(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes, int threads) throws SeedsException {
        super(g, numSeeds, threshold, nodes);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);

        h = 0;
        mPosition = new int[mNumSeeds];
        mRemainder = new int[mNumSeeds];
        mTrackerMutable = new int[lengthBitsArray(mGraph.numNodes())];
        mTrackerImmutable = new int[lengthBitsArray(mGraph.numNodes())];
        mSignMutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        mSignImmutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
    }

    /**
     * Creates a new Propagate-P instance with default values
     */
    public MultithreadPropagateP(final ImmutableGraph g, int numSeeds, double threshold, int threads) throws SeedsException {
        super(g, numSeeds, threshold);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);

        h = 0;
        mPosition = new int[mNumSeeds];
        mRemainder = new int[mNumSeeds];
        mTrackerMutable = new int[lengthBitsArray(mGraph.numNodes())];
        mTrackerImmutable = new int[lengthBitsArray(mGraph.numNodes())];
        mSignMutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        mSignImmutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
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

    public int groupNodesByThread(int value) {
        return (int) Math.ceil(value / mNumberOfThreads);
    }

    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;

        mCollisionsVector = new long[1];
        mLock = new ReentrantLock();
        int numberOfNodes4Group = groupNodesByThread(mGraph.numNodes());

        logger.debug("Number of threads to be used {}", mNumberOfThreads);
        logger.debug("Number of nodes for each group {}", numberOfNodes4Group);

        for (int s = 0; s < mNumSeeds; s++) {
            mPosition[s] = s >>> Constants.MASK;
            mRemainder[s] = (s << Constants.REMAINDER) >>> Constants.REMAINDER;
            mSignMutable[mMinHashNodeIDs[s]][mPosition[s]] |= (Constants.BIT) << mRemainder[s];
            mSignImmutable[mMinHashNodeIDs[s]][mPosition[s]] |= (Constants.BIT) << mRemainder[s];
            mTrackerImmutable[mMinHashNodeIDs[s] >>> Constants.MASK] |= (Constants.BIT) << ((mMinHashNodeIDs[s] << Constants.REMAINDER) >>> Constants.REMAINDER);
        }

        // count the collisions of the signatures
        long collisions = 0;
        for (int r = 0; r < mGraph.numNodes(); r++) {
            for (int c = 0; c < mSignMutable[r].length; c++) {
                collisions += Integer.bitCount(mSignMutable[r][c]);
            }
        }

        mCollisionsVector[h] = collisions;
        h += 1;

        mCyclicBarrier = new CyclicBarrier(mNumberOfThreads, new AggregatorThread(mGraph.copy()));
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads

        int start = 0;
        int end = start + numberOfNodes4Group;
        List<IterationThread> todo = new ArrayList<>(mNumberOfThreads);

        for (int nt = 0; nt < mNumberOfThreads; nt++) {
            mSignatureIsChanged = (mSignatureIsChanged & ~(1 << nt)) | ((1 << nt));

            if (nt == mNumberOfThreads - 1) {
                logger.debug("start {} end {} index {}", start, mGraph.numNodes() - 1, nt);
                todo.add(new IterationThread(mGraph.copy(), start, mGraph.numNodes() - 1, nt));
            } else {
                logger.debug("start {} end {} index {}", start, end, nt);
                todo.add(new IterationThread(mGraph.copy(), start, end, nt));
            }
            start = end + 1;
            end = start + numberOfNodes4Group;
        }
        try {
            executor.invokeAll(todo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        double[] hopTable = hopTable(mCollisionsVector);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setHopTable(hopTable);
        graphMeasure.setLowerBoundDiameter(mCollisionsVector.length - 1);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setSeedsList(mSeeds);
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTable));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTable, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTable));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTable, mThreshold));

        return graphMeasure;
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    public double[] hopTable(long[] collisionsVector) {
        double[] hopTable = new double[collisionsVector.length];
        for (int i = 0; i < hopTable.length; i++) {
            hopTable[i] = ((double) collisionsVector[i] * mGraph.numNodes()) / this.mNumSeeds;
        }
        return hopTable;
    }

    class AggregatorThread implements Runnable {

        private final ImmutableGraph g;

        public AggregatorThread(ImmutableGraph g) {
            this.g = g;
        }

        @Override
        public void run() {
            logger.debug("barrier, mSignatureIsChanged {}", mSignatureIsChanged);
            if (mSignatureIsChanged != 0) {
                System.arraycopy(mTrackerMutable, 0, mTrackerImmutable, 0, mTrackerMutable.length);
                mTrackerMutable = new int[lengthBitsArray(mGraph.numNodes())];

                // count the collisions of the signatures
                long collisions = 0;
                for (int r = 0; r < g.numNodes(); r++) {
                    System.arraycopy(mSignMutable[r], 0, mSignImmutable[r], 0, mSignMutable[r].length);
                    for (int c = 0; c < mSignMutable[r].length; c++) {
                        collisions += Integer.bitCount(mSignMutable[r][c]);
                    }
                }

                long[] copy = new long[h + 1];
                System.arraycopy(mCollisionsVector, 0, copy, 0, mCollisionsVector.length);
                mCollisionsVector = copy;

                mCollisionsVector[h] = collisions;
                h += 1;
            }
        }

    }

    class IterationThread implements Callable<Integer> {

        private final ImmutableGraph g;
        private final int start;
        private final int end;
        private final int index;

        public IterationThread(ImmutableGraph g, int start, int end, int index) {
            this.g = g;
            this.start = start;
            this.end = end;
            this.index = index;
        }

        @Override
        public Integer call() {
            long startHopTime = System.currentTimeMillis();
            long lastLogTime = startHopTime;
            long logTime;

            boolean signatureIsChanged;

            int nPosition, nRemainder, neighPosition, neighRemainder, neighMask;
            while (mSignatureIsChanged != 0) {
                signatureIsChanged = false;

                // update node signature
                for (int n = start; n < end + 1; n++) {
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

                        if (((neighMask & mTrackerImmutable[neighPosition]) >>> neighRemainder) == 1) {
                            // for each element of the signature of the node n
                            int sMask;
                            for (int s = 0; s < mNumSeeds; s++) {
                                sMask = (Constants.BIT << mRemainder[s]);

                                // check if the s-th element of the node n signature
                                // it's 0, else jump to the next s-th element of the signature
                                if (((sMask & mSignMutable[n][mPosition[s]]) >>> mRemainder[s]) == 0) {
                                    int bitNeigh;
                                    int value;
                                    // change the s-th element of the node n signature
                                    // only if the s-th element of the neigh signature is 1
                                    if (((sMask & mSignImmutable[successors[l]][mPosition[s]]) >>> mRemainder[s]) == 1) {
                                        bitNeigh = (((1 << mRemainder[s]) & mSignImmutable[successors[l]][mPosition[s]]) >>> mRemainder[s]) << mRemainder[s];
                                        value = bitNeigh | sMask & mSignImmutable[successors[l]][mPosition[s]];
                                        signatureIsChanged = true; // track the signature changes, to run the next hop
                                        mTrackerMutable[nPosition] |= (Constants.BIT) << nRemainder;
                                        mSignMutable[n][mPosition[s]] = mSignMutable[n][mPosition[s]] | value;
                                    }
                                }
                            }
                        }
                    }

/*                    logTime = System.currentTimeMillis();

                    if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                        logger.info("(hop # {}) # nodes analyzed {} / {}, estimated time remaining {} ms",
                                h,
                                (n-start), end-start,
                                (((end-start)-(n-start)) * (logTime - startHopTime)) / n);
                        lastLogTime = logTime;
                    }*/
                }

                int b = signatureIsChanged ? 1 : 0;
                logger.debug("thread {} hop {} signatureIsChanged {}", index, h, signatureIsChanged);
                mLock.lock();
                mSignatureIsChanged = (mSignatureIsChanged & ~(1 << index)) | ((b << index) & (1 << index));
                mLock.unlock();

                try {
                    mCyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    }
}
