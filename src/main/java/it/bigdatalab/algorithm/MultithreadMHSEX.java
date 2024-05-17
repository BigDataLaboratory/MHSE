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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Implementation of MHSE X (MinHash Signature Estimation X version) algorithm
 */
public class MultithreadMHSEX extends MinHash {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadMHSEX");

    private final int mNumberOfThreads;
    private int[] mPosition;
    private int[] mRemainder;
    private int[] mTrackerMutable;
    private int[] mTrackerImmutable;
    private int[][] mSignMutable;
    private int[][] mSignImmutable;
    private CyclicBarrier mCyclicBarrier;
    private boolean barrierFree;
    //private boolean []  waiting;
    //private boolean []  waiting_outside;

    private int h;
    private long[] mCollisionsVector;
    private volatile int mSignatureIsChanged;
    private volatile boolean [] mSetSignaturesChanged;
    private ReentrantLock mLock;

    private boolean mDoCentrality;

    private boolean[] saturated;

    private int[] mToVisit;

    private long[][] mHopForNodes;

    private double [][] mHarmonic;


    /**
     * Creates a new MHSE X instance with default values
     */
    public MultithreadMHSEX(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes, int threads, boolean centrality) throws SeedsException {
        super(g, numSeeds, threshold, nodes);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mDoCentrality = centrality;

        h = 0;
        mPosition = new int[mNumSeeds];
        mRemainder = new int[mNumSeeds];
        mTrackerMutable = new int[lengthBitsArray(mGraph.numNodes())];
        mTrackerImmutable = new int[lengthBitsArray(mGraph.numNodes())];
        mSignMutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        mSignImmutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        saturated = new boolean[mGraph.numNodes()];
        Arrays.fill(saturated, Boolean.FALSE);
    }

    /**
     * Creates a new MHSE X instance with default values
     */
    public MultithreadMHSEX(final ImmutableGraph g, int numSeeds, double threshold, int threads, boolean centrality) throws SeedsException {
        super(g, numSeeds, threshold);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mDoCentrality = centrality;

        h = 0;
        mPosition = new int[mNumSeeds];
        mRemainder = new int[mNumSeeds];
        mTrackerMutable = new int[lengthBitsArray(mGraph.numNodes())];
        mTrackerImmutable = new int[lengthBitsArray(mGraph.numNodes())];
        mSignMutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        mSignImmutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        saturated = new boolean[mGraph.numNodes()];
        Arrays.fill(saturated, Boolean.FALSE);
        mToVisit = new int[lengthBitsArray(mGraph.numNodes())];

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
    private void iteration_thread(){}
    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;

        mCollisionsVector = new long[1];
        mLock = new ReentrantLock();
        int numberOfNodes4Group = groupNodesByThread(mGraph.numNodes());
        int d = mNumSeeds / mNumberOfThreads;
        int y = mNumSeeds % mNumberOfThreads;
        int ntasks = (d== 0) ? y:mNumberOfThreads;
        if (ntasks != mNumberOfThreads){
            logger.debug("Number of designed workers Threads {}/{}",ntasks,mNumberOfThreads);
        }
        if (mDoCentrality) {
            //mHopForNodes = new short[mGraph.numNodes()][mNumSeeds];
            //mHarmonic = new double[mGraph.numNodes()][mNumSeeds];

            mHopForNodes = new long[mGraph.numNodes()][ntasks];
            mHarmonic = new double[mGraph.numNodes()][ntasks];
        }

        logger.debug("Number of threads to be used {}", mNumberOfThreads);
        logger.debug("Number of nodes for each group {}", numberOfNodes4Group);
        logger.debug("Number of tasks {}",ntasks);

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
        barrierFree = false;
        //waiting = new boolean[mNumberOfThreads];
        //waiting_outside = new boolean[mNumberOfThreads];

        int start = 0;
        int end = start + numberOfNodes4Group;
        //logger.debug(" number of threads for node {}",numberOfNodes4Group);

        mSetSignaturesChanged = new boolean[ntasks];

        List<IterationThread> todo = new ArrayList<>(ntasks);

        logger.debug("number of nodes {} number of seeds {}",mGraph.numNodes(),mNumSeeds);
       // for (int nt = 0; nt < mNumberOfThreads; nt++) {

        int task_size = (int) Math.ceil((double) mNumSeeds / mNumberOfThreads);
        task_size = numberOfNodes4Group;
        for (int nt = 0; nt < ntasks; nt++) {
            start = nt * task_size;
            end = Math.min((nt + 1) * task_size, mNumSeeds);

            // waiting[nt] = false;
            // waiting_outside[nt] = true;
            mSetSignaturesChanged[nt] = true;
            mSignatureIsChanged = (mSignatureIsChanged & ~(1 << nt)) | ((1 << nt));
            /*
            if (nt == mNumberOfThreads - 1) {
                logger.debug("start {} end {} index {}", start, mGraph.numNodes() - 1, nt);
                todo.add(new IterationThread(mGraph.copy(), start, mGraph.numNodes() - 1, nt));
            } else {
                logger.debug("start {} end {} index {}", start, end, nt);
                todo.add(new IterationThread(mGraph.copy(), start, end, nt));
            }



            */
            //if (start < mNumSeeds){
                //logger.debug("start {} end {} index {}", start, end, nt);
               // todo.add(new IterationThread(mGraph.copy(), start, end, nt));
            //}
            if (nt == ntasks - 1) {
                logger.debug("start {} end {} index {}", start, mGraph.numNodes() - 1, nt);
                todo.add(new IterationThread(mGraph.copy(), start, mGraph.numNodes(), nt));
            }else{
                logger.debug("start {} end {} index {}", start, end, nt);
                todo.add(new IterationThread(mGraph.copy(), start, end, nt));
            }
            //start = end + 1;
           // end = start + numberOfNodes4Group;
        }


        try{
            executor.invokeAll(todo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        executor.shutdown();
        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        double [] inverseFarness = new double[1];
        double [] farness = new double[1];
        if (mDoCentrality){
            inverseFarness = new double [mGraph.numNodes()];
            farness = new double[mGraph.numNodes()];
            for (int i = 0; i < mGraph.numNodes(); i++) {
                for (int j = 0; j < ntasks; j++) {
                    inverseFarness[i] += mHarmonic[i][j];

                    farness[i] += (double) mHopForNodes[i][j];
                }
                //logger.debug("{} ",inverseFarness[i]);
                //double prima = inverseFarness[i];
                //inverseFarness[i] = inverseFarness[i] * mGraph.numNodes()/(mNumSeeds*(mGraph.numNodes()-1));
                inverseFarness[i] = inverseFarness[i] * ((double) mGraph.numNodes()/(mGraph.numNodes()-1)/mNumSeeds);

                farness[i] = (double) farness[i]   * ((double) mGraph.numNodes() /mNumSeeds);

            }

        }
        double[] hopTable = hopTable(mCollisionsVector);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setHopTable(hopTable);
        graphMeasure.setLowerBoundDiameter(mCollisionsVector.length - 1);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setSeedsList(mSeeds);
        if(mDoCentrality){
            //int[] farness = farnessArray(mHopForNodes);
            graphMeasure.setFarness(farness);
            graphMeasure.setInverseFarness(inverseFarness);
            //graphMeasure.setClosenessCentrality(Stats.ClosenessCentrality(mGraph.numNodes(),mNumSeeds,farness,true));
            graphMeasure.setHarmonicCentrality(inverseFarness);
            //graphMeasure.setLinnCentrality(Stats.LinnCentrality(mGraph.numNodes(),mNumSeeds,farness,hopTable));

        }
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
            barrierFree = true;
            for (int i = 0; i < mNumberOfThreads; i++) barrierFree = barrierFree & !mSetSignaturesChanged[i];
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
            }else{
                logger.debug("Barrier free");
                barrierFree = true;
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
        public Integer call() throws InterruptedException {
            long startHopTime = System.currentTimeMillis();
            long lastLogTime = startHopTime;
            long logTime;

            boolean signatureIsChanged,awaitCall;
            int nPosition, nRemainder, neighPosition, neighRemainder, neighMask;
            awaitCall = false;
            boolean allConverged = false;
            //while (mSignatureIsChanged != 0 ) {
            while (mSignatureIsChanged != 0  || !barrierFree) {
                //logger.debug("SIGNATURE CHANGE {}",mSignatureIsChanged);
                awaitCall = false;
                signatureIsChanged = false;
                if (mSetSignaturesChanged[index]) {
                    // update node signature
                    for (int n = start; n < end ; n++) {
                        //nPosition = n >>> Constants.MASK;
                        //nRemainder = (n << Constants.REMAINDER) >>> Constants.REMAINDER;
                        if (!saturated[n]) {// todo cambiare in array di int - trick
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
                                    boolean tmp_saturated = true;
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
                                                tmp_saturated = tmp_saturated && (mSignMutable[n][mPosition[s]] == 1);

                                                //if ((value >>> nRemainder) == 1) {
                                                if (signatureIsChanged) {
                                                    if (mDoCentrality) {

                                                        mHopForNodes[n][index] += (double) h;

                                                        mHarmonic[n][index] += (double) 1.0 / h;


                                                    }
                                                }
                                                //if (((sMask & mSignImmutable[successors[l]][mPosition[s]]) >>> mRemainder[s]) == 0) {
                                                //    tmp_saturated = false;
                                                //}

                                            }

                                        }
                                        saturated[n] = tmp_saturated;
                                    }
                                }
                            }
                        }

                    }
                    int b = signatureIsChanged ? 1 : 0;
                    mLock.lock();
                    try {
                        mSetSignaturesChanged[index] = signatureIsChanged;
                        mSignatureIsChanged = (mSignatureIsChanged & ~(1 << index)) | ((b << index) & (1 << index));
                    } finally {
                        mLock.unlock();
                    }
                    //Else for the signature change
                }

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