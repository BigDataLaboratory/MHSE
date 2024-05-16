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

public class MultithreadExpansion extends BMinHashOpt {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadExpansion");

    private final int mNumberOfThreads;
    private final double[] mSeedTime;
    private final boolean mDoCentrality;
    private long startTime;
    private short[][] mHopForNodes;
    private double [][] mHarmonic;
    private double [][] mFareness;
    private int [] mLowerBoundDiameter;
    private int [][][] mHopTable;
    /**
     * Creates a new MultithreadExpansion instance with default values
     */
    public MultithreadExpansion(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes, int threads, boolean centrality) {
        super(g, numSeeds, threshold, nodes);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mSeedTime = new double[mNumSeeds];
        mDoCentrality = centrality;
    }

    /**
     * Creates a new MultithreadExpansion instance with default values
     */
    public MultithreadExpansion(final ImmutableGraph g, int numSeeds, double threshold, int threads, boolean centrality) {
        super(g, numSeeds, threshold);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mSeedTime = new double[mNumSeeds];
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
        mDoCentrality = centrality;
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

    private void iteration_thread(int s,int task_id,int[] local_lb_diameter,int [][] local_hop_table,int[] local_last_hops,int[] local_farness,float[] local_harmonic){
        int collisions;

        int[] p_prev = new int[lengthBitsArray(mGraph.numNodes())];
        int[] p_next = new int[lengthBitsArray(mGraph.numNodes())];
        int[] expanded = new int[lengthBitsArray(mGraph.numNodes())];
        int[] visited = new int[mGraph.numNodes()];

        // Choose a random node is equivalent to compute the minhash
        // It could be set in mhse.properties file with the "minhash.nodeIDs" property
        int randomNode = mMinHashNodeIDs[s];
        visited[randomNode] = 1;
        int remainderPositionNeigh;
        int quotientNeigh;

        int remainderPositionNode;
        int quotientNode;
        int node;
        int bit;

        int h = 0;
        boolean signatureIsChanged = true;

        // initialization of the collision counter for the hop
        // we use a dict because we want to iterate over the nodes until
        // the number of collisions in the actual hop
        // is different than the previous hop
        //int[] hopTable = new int[1];
        local_hop_table[task_id] = new int[1];

        while (signatureIsChanged) {
            //first hop - initialization
            if (h == 0) {

                // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                // This is equal to logical and operation.
                // remaremainderPositionRandomNode contains the bit index of the node
                int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
                // quotient is randomNode >>> MASK and give us the position of the node in the array
                // i.e if the actual node is 16 and we use an array of int (32 bit lenght for each cell) then
                // the node is at index 0 of the array of the first int from 0 to 31
                p_next[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
            } else { // next hops
                signatureIsChanged = false;

                // copy all the actual nodes hash in a new structure
                System.arraycopy(p_next, 0, p_prev, 0, p_next.length);

                for (int index = 0; index < p_prev.length; index++) {
                    if (p_prev[index] != 0) { // trick
                        for (int bitPosition = 0; bitPosition < Integer.SIZE; bitPosition++) {
                            bit = (p_prev[index] >> bitPosition) & Constants.BIT; // todo maybe there's another solution?
                            int bitExpanded = (expanded[index] >> bitPosition) & 1;

                            if (bit == 1 && bitExpanded != 1) {
                                node = (index * Integer.SIZE) + bitPosition;

                                quotientNode = node >>> Constants.MASK; // position into array
                                remainderPositionNode = (node << Constants.REMAINDER) >>> Constants.REMAINDER;
                                expanded[quotientNode] |= (Constants.BIT) << remainderPositionNode;

                                final int d = mGraph.outdegree(node);
                                final int[] successors = mGraph.successorArray(node);
                                for (int l = 0; l < d; l++) {
                                    final int neighbour = successors[l];
                                    quotientNeigh = neighbour >>> Constants.MASK; // position into array
                                    remainderPositionNeigh = (neighbour << Constants.REMAINDER) >>> Constants.REMAINDER;
                                    p_next[quotientNeigh] |= (Constants.BIT) << remainderPositionNeigh;
                                    if ((p_next[quotientNeigh] ^ p_prev[quotientNeigh]) != 0) {
                                        if (mDoCentrality) {
                                            // Questo non funziona in questa implementazione, dobbiamo capire perché
                                            // Bypass temporaneo con array di visited
                                            //int bit_neigh_next = (p_next[quotientNeigh] & ((Constants.BIT) << remainderPositionNeigh)) >>> remainderPositionNeigh;
                                            //int bit_neigh_prev = (p_prev[quotientNeigh] & ((Constants.BIT) << remainderPositionNeigh)) >>> remainderPositionNeigh;
                                            //if ((bit_neigh_next & bit_neigh_prev) != 1) {
                                                if (visited[neighbour] != 1){
                                                    visited[neighbour] = 1;
                                                    local_farness[neighbour] += (short) h;
                                                    local_harmonic[neighbour] += 1.0/(short) h;
                                                }
                                                //logger.debug("father {} local farness of {} = {}, hop to be added {}",node,neighbour,local_farness[neighbour],h);

                                            //}
                                        }
                                        signatureIsChanged = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // count the collision between the node signature and the graph signature
            if (signatureIsChanged) {
                collisions = 0;
                for (int aMutable : p_next) {
                    collisions += Integer.bitCount(aMutable);
                }

                int[] copy = new int[h + 1];
                System.arraycopy(local_hop_table[task_id], 0, copy, 0, local_hop_table[task_id].length);
                local_hop_table[task_id] = copy;
                local_hop_table[task_id][h] = collisions;
                if (local_lb_diameter[0] <= h) local_lb_diameter[0] = h;
                //hopTable[h] = collisions;

                h += 1;
            }
        }
        local_last_hops[task_id] = h-1;

    }

    public Measure runAlgorithm() throws IOException {
        startTime = System.currentTimeMillis();
        long totalTime;

        logger.debug("Number of threads to be used {}", mNumberOfThreads);

        int[][] collisionsMatrix = new int[mNumSeeds][];
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;
        int lowerboundDiameter = 0;
        int[] vs_active = new int[mNumSeeds];
        for (int i = 0; i < mNumSeeds; i++) {
            vs_active[i] = i;
        }
        int d = mNumSeeds / mNumberOfThreads;
        int r = mNumSeeds % mNumberOfThreads;
        int ntasks = (d== 0) ? r:mNumberOfThreads;
        int [][]  local_lb_diameter = new int[mNumberOfThreads][];
        int [][][] local_hop_table = new int[mNumberOfThreads][][];
        float [][] local_harmonic = new float[mNumberOfThreads][];
        int [][] local_farness = new int[mNumberOfThreads][];
        int [][] local_last_hops = new int[mNumberOfThreads][];
        int task_size = (int) Math.ceil((double) mNumSeeds / mNumberOfThreads);
        for (int i = 0; i < mNumberOfThreads; i++) {
            local_hop_table[i] = new int[task_size][];
            local_lb_diameter[i] = new int[1];
            local_last_hops[i] = new int[task_size];
            if (mDoCentrality) {
                local_harmonic[i] = new float[mGraph.numNodes()];
                local_farness[i] = new int[mGraph.numNodes()];
            }else{
                local_harmonic[i] = new float[0];
                local_farness[i]= new int[0];
            }

        }

        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        for (int t = 0; t < ntasks; t++) {
            int start = t * task_size;
            int end = Math.min((t + 1) * task_size, mNumSeeds);
            final int taskRangeStart = start;
            final int taskRangeEnd = end;
            final int taskIndex = t;
            // Here we could change lists with arrays of fixed sizes
            executor.execute(() -> {
                int task_id = 0;
                for (int s = taskRangeStart; s < taskRangeEnd; s++) {
                    iteration_thread(s, task_id, local_lb_diameter[taskIndex], local_hop_table[taskIndex], local_last_hops[taskIndex], local_farness[taskIndex], local_harmonic[taskIndex]);
                    task_id += 1;
                }
            });
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
        double [] farness = new double[0];

        if (mDoCentrality) {
            harmonic = new double[mGraph.numNodes()];
            farness = new double[mGraph.numNodes()];
        }

        int p = 0;
        for (int t = 0; t<mNumberOfThreads;t++){
            if (lowerboundDiameter < local_lb_diameter[t][0]) lowerboundDiameter = local_lb_diameter[t][0];
            for (int j = 0; j < local_hop_table[t].length; j++) {
                // this handles the case in which the number of seeds is less than the number of threads
                if (local_hop_table[t][j] != null) {
                    collisionsMatrix[p] = local_hop_table[t][j];
                    lastHops[p] = local_last_hops[t][j];
                    p += 1;
                }
            }
        }

        for (int i = 0; i < mGraph.numNodes(); i++) {
            for (int j = 0; j < mNumberOfThreads; j++) {
                if (mDoCentrality) {
                    harmonic[i] += local_harmonic[j][i];
                    farness[i] += local_farness[j][i];
                }
            }

            if (mDoCentrality) {
                farness[i] = (double) farness[i] * mGraph.numNodes() / mNumSeeds;
                harmonic[i] = (double) harmonic[i] * mGraph.numNodes() /(mGraph.numNodes()-1)/ mNumSeeds;
                //logger.debug(" Fareness node {} = {}",i,farness[i]);
            }


        }
        normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter);
        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);


        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        if (mDoCentrality) {

            graphMeasure.setFarness(farness);
            graphMeasure.setHarmonicCentrality(harmonic);

        }
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



   // @Override
    public Measure runAlgorithm_tmp() throws IOException {
        startTime = System.currentTimeMillis();
        long totalTime;

        logger.debug("Number of threads to be used {}", mNumberOfThreads);

        int[][] collisionsMatrix = new int[mNumSeeds][];
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;

        int lowerboundDiameter = 0;

        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<>(this.mNumSeeds);

        for (int i = 0; i < this.mNumSeeds; i++) {
            //todo.add(new IterationThread(mGraph.copy(), i));
        }
        /*

        !!! TODO (VERY IMPORTANT) !!
        This is not space efficient, we should opt for a different approach in which
        we have mHopForNodes = new short[mGraph.numNodes()][mNumberOfThreads]
        and each thread gets assigned mNumSeeds/mNumberOfThreads seeds. This reduces the space complexity
        to  n x ThreadNumber (now the space complexity is n x mNumberOfThreads (if we consider the theoretical bound on
        mNumberOfThreads, the space complexity becomes polynomial and not linear in n.

        */
        if (mDoCentrality) {
            mHopForNodes = new short[mGraph.numNodes()][mNumSeeds];
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
        if (mDoCentrality) {
            double[] farness = farnessArray(mHopForNodes);
            for (int i= 0; i<farness.length;i++) logger.debug(" Fareness node {} = {}",i,farness[i]);

            double[] inverseFarness = inverseFarnessArray(mHopForNodes);
            graphMeasure.setFarness(farness);
            graphMeasure.setInverseFarness(inverseFarness);
            //graphMeasure.setClosenessCentrality(Stats.ClosenessCentrality(mGraph.numNodes(), mNumSeeds, farness, true));
            graphMeasure.setHarmonicCentrality(Stats.HarmonicCentrality(mGraph.numNodes(), mNumSeeds, inverseFarness));
            //graphMeasure.setLinnCentrality(Stats.LinnCentrality(mGraph.numNodes(), mNumSeeds, farness, hopTableArray));

        }
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



    public Measure runAlgorithm_tmp3() throws IOException {
        startTime = System.currentTimeMillis();
        long totalTime;

        logger.debug("Number of threads to be used {}", mNumberOfThreads);

        //int[][] collisionsMatrix = new int[mNumSeeds][];
        int[][] collisionsMatrix = new int[mNumSeeds][];

        double[] hopTableArray;
        int lowerboundDiameter = 0;
        int[] vs_active = new int[mNumSeeds];
        for (int i = 0; i < mNumSeeds; i++) {
            vs_active[i] = i;
        }
        int d = mNumSeeds / mNumberOfThreads;
        int r = mNumSeeds % mNumberOfThreads;
        int ntasks = (d== 0) ? r:mNumberOfThreads;
        mHopTable = new int[mNumberOfThreads][][];

        int task_size = (int) Math.ceil((double) mNumSeeds / mNumberOfThreads);
        for (int i = 0; i < mNumberOfThreads; i++) {
            mHopTable[i] = new int[task_size][];
        }
        // TODO: same grouping as MHSEX
        if (mDoCentrality){
            mFareness = new double[mNumberOfThreads][mGraph.numNodes()];

            mHarmonic = new double[mNumberOfThreads][mGraph.numNodes()];

        }
        mLowerBoundDiameter = new int[mNumberOfThreads];
        //collisionsMatrix = new int[ntasks][];
        ExecutorService executor = Executors.newFixedThreadPool(ntasks); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<>(ntasks);
       logger.debug("Nseeds {} - Ntasks {} - Nodes {} - Assignnnment {}",mNumSeeds,ntasks,mGraph.numNodes(),mNumSeeds/ntasks);
        for (int t = 0; t < ntasks; t++) {
            int start = t * task_size;
            int end = Math.min((t + 1) * task_size, mNumSeeds);
            todo.add(new IterationThread(mGraph.copy(),start,end,t));
        }

        try {
            List<Future<int[]>> futures = executor.invokeAll(todo);
            for (int i = 0; i < this.mNumberOfThreads; i++) {
                if (i<futures.size()) {
                    Future<int[]> future = futures.get(i);
                    if (!future.isCancelled()) {
                        try {
                            int[] hopCollisions = future.get();
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
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        // Reduction phase
        lowerboundDiameter = 0;
        /*
        for (int i = 0;i <ntasks;i++){
            if (mLowerBoundDiameter[i] > lowerboundDiameter) lowerboundDiameter = mLowerBoundDiameter[i];
        }
        */
        int p = 0;
        for (int t = 0; t<mNumberOfThreads;t++){
            //if (t < ntasks) {
                if (mLowerBoundDiameter[t] > lowerboundDiameter) lowerboundDiameter = mLowerBoundDiameter[t];
            //}
            for (int j = 0; j < mHopTable[t].length; j++) {
                // this handles the case in which the number of seeds is less than the number of threads
                if (mHopTable[t][j] != null) {
                    collisionsMatrix[p] = mHopTable[t][j];
                    //lastHops[p] = local_last_hops[t][j];
                    p += 1;
                }
            }
        }



        double[] harmonic = new double[0];
        double [] farness = new double[0];

        if (mDoCentrality) {
            harmonic = new double[mGraph.numNodes()];
            farness = new double[mGraph.numNodes()];
        }

        for (int i = 0; i < mGraph.numNodes(); i++) {
            for (int j = 0; j < mNumberOfThreads; j++) {
                if (mDoCentrality) {
                    harmonic[i] += mHarmonic[j][i];
                    farness[i] += mFareness[j][i];
                }
            }

            if (mDoCentrality) {
                farness[i] =  farness[i] * ((double) mGraph.numNodes() / mNumSeeds);
                harmonic[i] = harmonic[i] * ((double) mGraph.numNodes() /(mGraph.numNodes()-1)/ mNumSeeds);
                //logger.debug(" Fareness node {} = {}",i,farness[i]);
            }
        }
        normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter);
        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);
        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        if (mDoCentrality) {
            graphMeasure.setFarness(farness);
            graphMeasure.setHarmonicCentrality(harmonic);

        }
        //graphMeasure.setLastHops(lastHops);
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
        //------------------
        /*
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        for (int t = 0; t < ntasks; t++) {
            int start = t * task_size;
            int end = Math.min((t + 1) * task_size, mNumSeeds);
            final int taskRangeStart = start;
            final int taskRangeEnd = end;
            final int taskIndex = t;
            // Here we could change lists with arrays of fixed sizes
            executor.execute(() -> {
                int task_id = 0;
                for (int s = taskRangeStart; s < taskRangeEnd; s++) {
                    iteration_thread(s, task_id, local_lb_diameter[taskIndex], local_hop_table[taskIndex], local_last_hops[taskIndex], local_farness[taskIndex], local_harmonic[taskIndex]);
                    task_id += 1;
                }
            });
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
        double [] farness = new double[0];

        if (mDoCentrality) {
            harmonic = new double[mGraph.numNodes()];
            farness = new double[mGraph.numNodes()];
        }

        int p = 0;
        for (int t = 0; t<mNumberOfThreads;t++){
            if (lowerboundDiameter < local_lb_diameter[t][0]) lowerboundDiameter = local_lb_diameter[t][0];
            for (int j = 0; j < local_hop_table[t].length; j++) {
                // this handles the case in which the number of seeds is less than the number of threads
                if (local_hop_table[t][j] != null) {
                    collisionsMatrix[p] = local_hop_table[t][j];
                    lastHops[p] = local_last_hops[t][j];
                    p += 1;
                }
            }
        }

        for (int i = 0; i < mGraph.numNodes(); i++) {
            for (int j = 0; j < mNumberOfThreads; j++) {
                if (mDoCentrality) {
                    harmonic[i] += local_harmonic[j][i];
                    farness[i] += local_farness[j][i];
                }
            }

            if (mDoCentrality) {
                farness[i] = (double) farness[i] * mGraph.numNodes() / mNumSeeds;
                harmonic[i] = (double) harmonic[i] * mGraph.numNodes() /(mGraph.numNodes()-1)/ mNumSeeds;
                //logger.debug(" Fareness node {} = {}",i,farness[i]);
            }


        }
        normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter);
        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);


        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        if (mDoCentrality) {

            graphMeasure.setFarness(farness);
            graphMeasure.setHarmonicCentrality(harmonic);

        }
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

         */

    }

    class IterationThread implements Callable<int[]> {
        private final ImmutableGraph g;
        private final int start;
        private final int end;
        private final int index;
        public IterationThread(ImmutableGraph g, int start, int end ,int index) {
            this.g = g;
            this.start = start;
            this.end = end;
            this.index = index;
        }
        @Override
        public int[] call() throws InterruptedException {
            long startHopTime = System.currentTimeMillis();
            long lastLogTime = startHopTime;
            long logTime;
            int collisions;

            int[] p_prev = new int[1];
            int[] p_next = new int[1];
            int[] expanded = new int[1];
            int[] visited =new int[g.numNodes()];
            int randomNode = -1;
            int task_id = 0;
            int remainderPositionNeigh;
            int quotientNeigh;

            int remainderPositionNode;
            int quotientNode;
            int node;
            int bit;
            int h = 0;
            boolean signatureIsChanged = true;
            //int[] hopTable = new int[g.numNodes()];
            int[] hopTable = new int[1];
            for (int s = start; s < end ; s++) {
                // Initializing variables
                p_prev = new int[lengthBitsArray(g.numNodes())];
                p_next = new int[lengthBitsArray(g.numNodes())];
                expanded = new int[lengthBitsArray(g.numNodes())];
                //visited = new int[g.numNodes()];
                Arrays.fill(visited,0);
                randomNode = mMinHashNodeIDs[s];
                visited[randomNode] = 1;

                remainderPositionNeigh = 0;
                quotientNeigh = 0;

                remainderPositionNode = 0;
                quotientNode = 0;
                node = 0;
                bit  = 0;

                h = 0;
                signatureIsChanged = true;
                //hopTable = new int[1];
                mHopTable[index][task_id] = new int[1];

                while (signatureIsChanged) {
                    //first hop - initialization
                    if (h == 0) {

                        // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                        // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                        // This is equal to logical and operation.
                        // remaremainderPositionRandomNode contains the bit index of the node
                        int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
                        // quotient is randomNode >>> MASK and give us the position of the node in the array
                        // i.e if the actual node is 16 and we use an array of int (32 bit lenght for each cell) then
                        // the node is at index 0 of the array of the first int from 0 to 31
                        p_next[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
                    } else { // next hops
                        signatureIsChanged = false;

                        // copy all the actual nodes hash in a new structure
                        System.arraycopy(p_next, 0, p_prev, 0, p_next.length);

                        for (int index = 0; index < p_prev.length; index++) {
                            if (p_prev[index] != 0) { // trick
                                for (int bitPosition = 0; bitPosition < Integer.SIZE; bitPosition++) {
                                    bit = (p_prev[index] >> bitPosition) & Constants.BIT; // todo maybe there's another solution?
                                    int bitExpanded = (expanded[index] >> bitPosition) & 1;

                                    if (bit == 1 && bitExpanded != 1) {
                                        node = (index * Integer.SIZE) + bitPosition;

                                        quotientNode = node >>> Constants.MASK; // position into array
                                        remainderPositionNode = (node << Constants.REMAINDER) >>> Constants.REMAINDER;
                                        expanded[quotientNode] |= (Constants.BIT) << remainderPositionNode;

                                        final int d = g.outdegree(node);
                                        final int[] successors = g.successorArray(node);
                                        for (int l = 0; l < d; l++) {
                                            final int neighbour = successors[l];
                                            quotientNeigh = neighbour >>> Constants.MASK; // position into array
                                            remainderPositionNeigh = (neighbour << Constants.REMAINDER) >>> Constants.REMAINDER;
                                            p_next[quotientNeigh] |= (Constants.BIT) << remainderPositionNeigh;
                                            if ((p_next[quotientNeigh] ^ p_prev[quotientNeigh]) != 0) {
                                                if (mDoCentrality) {
                                                    /*
                                                    int bit_neigh_next = (p_next[quotientNeigh] & ((Constants.BIT) << remainderPositionNeigh)) >>> remainderPositionNeigh;
                                                    int bit_neigh_prev = (p_prev[quotientNeigh] & ((Constants.BIT) << remainderPositionNeigh)) >>> remainderPositionNeigh;
                                                    if ((bit_neigh_next & bit_neigh_prev) != 1) {
                                                        mHopForNodes[neighbour][s] = (short) h;
                                                    }
                                                    */
                                                    if (visited[neighbour] != 1){
                                                        visited[neighbour] = 1;
                                                        mFareness[index][neighbour] += (short) h;
                                                        mHarmonic[index][neighbour] += 1.0/(short) h;
                                                    }

                                                }
                                                signatureIsChanged = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // count the collision between the node signature and the graph signature
                    if (signatureIsChanged) {
                        collisions = 0;
                        for (int aMutable : p_next) {
                            collisions += Integer.bitCount(aMutable);
                        }


                        // this array copy becomes slow on big graphs with big diameter
                        int[] copy = new int[h + 1];
                        System.arraycopy(mHopTable[index][task_id], 0, copy, 0, mHopTable[index][task_id].length);
                        mHopTable[index][task_id] = copy;
                        // Check this accumulation, I believe we need to accumulate because we are processing start-end of threads
                        // Before returning the hopTable
                        mHopTable[index][task_id][h] = collisions;
                        // This keeps track of the diameter lowerbound for each thread
                        if (mLowerBoundDiameter[index] < h) mLowerBoundDiameter[index] = h;
                        h += 1;
                    }
                }
                task_id +=1;

            }

        return hopTable;

        }



    }

    class IterationThread_tmp implements Callable<int[]> {

        private  ImmutableGraph g;
        private  int s;

        public void IterationThread_tmp(ImmutableGraph g, int s) {
            this.g = g;
            this.s = s;
        }



        @Override
        public int[] call() throws Exception {

            int collisions;

            int[] p_prev = new int[lengthBitsArray(g.numNodes())];
            int[] p_next = new int[lengthBitsArray(g.numNodes())];
            int[] expanded = new int[lengthBitsArray(g.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            // It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = mMinHashNodeIDs[s];

            int remainderPositionNeigh;
            int quotientNeigh;

            int remainderPositionNode;
            int quotientNode;
            int node;
            int bit;
            int h = 0;
            boolean signatureIsChanged = true;

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            int[] hopTable = new int[1];

            while (signatureIsChanged) {
                //first hop - initialization
                if (h == 0) {

                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    // remaremainderPositionRandomNode contains the bit index of the node
                    int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
                    // quotient is randomNode >>> MASK and give us the position of the node in the array
                    // i.e if the actual node is 16 and we use an array of int (32 bit lenght for each cell) then
                    // the node is at index 0 of the array of the first int from 0 to 31
                    p_next[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
                } else { // next hops
                    signatureIsChanged = false;

                    // copy all the actual nodes hash in a new structure
                    System.arraycopy(p_next, 0, p_prev, 0, p_next.length);

                    for (int index = 0; index < p_prev.length; index++) {
                        if (p_prev[index] != 0) { // trick
                            for (int bitPosition = 0; bitPosition < Integer.SIZE; bitPosition++) {
                                bit = (p_prev[index] >> bitPosition) & Constants.BIT; // todo maybe there's another solution?
                                int bitExpanded = (expanded[index] >> bitPosition) & 1;

                                if (bit == 1 && bitExpanded != 1) {
                                    node = (index * Integer.SIZE) + bitPosition;

                                    quotientNode = node >>> Constants.MASK; // position into array
                                    remainderPositionNode = (node << Constants.REMAINDER) >>> Constants.REMAINDER;
                                    expanded[quotientNode] |= (Constants.BIT) << remainderPositionNode;

                                    final int d = g.outdegree(node);
                                    final int[] successors = g.successorArray(node);
                                    for (int l = 0; l < d; l++) {
                                        final int neighbour = successors[l];
                                        quotientNeigh = neighbour >>> Constants.MASK; // position into array
                                        remainderPositionNeigh = (neighbour << Constants.REMAINDER) >>> Constants.REMAINDER;
                                        p_next[quotientNeigh] |= (Constants.BIT) << remainderPositionNeigh;
                                        if ((p_next[quotientNeigh] ^ p_prev[quotientNeigh]) != 0) {
                                            if (mDoCentrality) {
                                                int bit_neigh_next = (p_next[quotientNeigh] & ((Constants.BIT) << remainderPositionNeigh)) >>> remainderPositionNeigh;
                                                int bit_neigh_prev = (p_prev[quotientNeigh] & ((Constants.BIT) << remainderPositionNeigh)) >>> remainderPositionNeigh;
                                                if ((bit_neigh_next & bit_neigh_prev) != 1) {
                                                    mHopForNodes[neighbour][s] = (short) h;
                                                }
                                            }
                                            signatureIsChanged = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // count the collision between the node signature and the graph signature
                if (signatureIsChanged) {
                    collisions = 0;
                    for (int aMutable : p_next) {
                        collisions += Integer.bitCount(aMutable);
                    }

                    int[] copy = new int[h + 1];
                    System.arraycopy(hopTable, 0, copy, 0, hopTable.length);
                    hopTable = copy;

                    hopTable[h] = collisions;

                    h += 1;
                }
            }
            return hopTable;
        }
    }
}
