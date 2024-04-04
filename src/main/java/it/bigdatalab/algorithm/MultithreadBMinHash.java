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
import java.util.concurrent.TimeUnit;

/**
 * Implementation of MultithreadBMinHash (MinHash Signature Estimation multithread boolean optimized version) algorithm
 */
public class MultithreadBMinHash extends BMinHashOpt {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHashOptimized");

    private final int mNumberOfThreads;
    private final double[] mSeedTime;
    private long startTime;
    private boolean mDoCentrality;
    private short[][] mHopForNodes;


    /**
     * Creates a new MultithreadBMinHashOptimized instance with default values
     */
    public MultithreadBMinHash(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes, int threads, boolean centrality) {
        super(g, numSeeds, threshold, nodes);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mSeedTime = new double[mNumSeeds];
        mDoCentrality = centrality;
    }

    /**
     * Creates a new MultithreadBMinHashOptimized instance with default values
     */
    public MultithreadBMinHash(final ImmutableGraph g, int numSeeds, double threshold, int threads, boolean centrality) {
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
    private void compute_harmonic(int s,int[] local_lb_diameter,List<int []> local_hop_table,List<Integer> local_last_hops,int[] local_farness,float[] local_harmonic){
        long startSeedTime = System.currentTimeMillis();
        long lastLogTime = startSeedTime;
        long logTime;
        int collisions = 0;

        // Set false as signature of all graph nodes
        // used to computing the algorithm
        int[] mutable = new int[lengthBitsArray(mGraph.numNodes())];
        int[] immutable = new int[lengthBitsArray(mGraph.numNodes())];

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
                mutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
            } else { // next hops
                signatureIsChanged = false;

                // copy all the actual nodes hash in a new structure
                System.arraycopy(mutable, 0, immutable, 0, mutable.length);
                int remainderPositionNode;
                int quotientNode;
                for (int n = 0; n < mGraph.numNodes(); n++) {

                    final int node = n;
                    final int d = mGraph.outdegree(node);
                    final int[] successors = mGraph.successorArray(node);

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
                                if (mDoCentrality) {
                                    local_farness[n] += (short) h;
                                    local_harmonic[n] += 1.0/ (short) h;
                                }
                                signatureIsChanged = true;
                                break;
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
                                        TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)),
                                        TimeUnit.MILLISECONDS.toSeconds(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)) -
                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)))));
                        lastLogTime = logTime;
                    }
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
                //local_hop_table.add(h);

                //logger.debug(" random node {} hop {} collisions {} table {} ",randomNode,h,collisions,local_hop_table[h]);

                //int[] copy = new int[h + 1];
                //System.arraycopy(hopTable, 0, copy, 0, hopTable.length);
                //hopTable = copy;

                //hopTable[h] = collisions;
                if (local_lb_diameter[0] <= h) local_lb_diameter[0] = h;

                h += 1;
            }
        }

        local_last_hops.add(h-1);
        local_hop_table.add(hopTable);
    }
    public Measure runAlgorithm() {
        startTime = System.currentTimeMillis();
        long totalTime;

        //logger.debug("Number of threads to be used {}", mNumberOfThreads);

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
        List<int[]> local_lb_diameter = new ArrayList<>();
        List<List<int[]>> local_hop_table = new ArrayList<>();
        List<float[]> local_harmonic = new ArrayList<>();
        List<int[]> local_farness = new ArrayList<>();
        List<List<Integer>> local_last_hops = new ArrayList<>();
        for (int i = 0; i < mNumberOfThreads; i++) {
            local_hop_table.add(new ArrayList()); // Initialize local_hop_table
            local_lb_diameter.add(new int[1]);
            local_last_hops.add(new ArrayList());
            if (mDoCentrality) {
                local_harmonic.add(new float[mGraph.numNodes()]); // Initialize local_harmonic
                local_farness.add(new int[mGraph.numNodes()]); // Initialize local_farness
            }else{
                local_harmonic.add(new float[0]);
                local_farness.add(new int[0]);
            }

        }
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        int task_size = (int) Math.ceil((double) mNumSeeds / mNumberOfThreads);

        for (int t = 0; t < ntasks; t++) {
            int start = t * task_size;
            int end = Math.min((t + 1) * task_size,  mNumSeeds);
            final int taskRangeStart = start;
            final int taskRangeEnd = end;
            final int taskIndex = t;
            // Here we could change lists with arrays of fixed sizes
            executor.execute(() -> {
                for (int s = taskRangeStart; s < taskRangeEnd; s++) {
                    compute_harmonic(s,local_lb_diameter.get(taskIndex),local_hop_table.get(taskIndex),local_last_hops.get(taskIndex),local_farness.get(taskIndex),local_harmonic.get(taskIndex));
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
        float[] harmonic = new float[0];
        int[] farness = new int[0];
        /*
        for (int i = 0; i<mNumberOfThreads;i++){
            if (lowerboundDiameter < local_lb_diameter.get(i)[0]) lowerboundDiameter = local_lb_diameter.get(i)[0];
        }
        */
        if (mDoCentrality) {
            harmonic = new float[mGraph.numNodes()];
            farness = new int[mGraph.numNodes()];
        }
        //REDUCE PHASE
        int p = 0;
        for (int t = 0; t<mNumberOfThreads;t++){
            if (lowerboundDiameter < local_lb_diameter.get(t)[0]) lowerboundDiameter = local_lb_diameter.get(t)[0];
            for (int j = 0; j < local_hop_table.get(t).size(); j++) {
                collisionsMatrix[p] = local_hop_table.get(t).get(j);
                lastHops[p] = local_last_hops.get(t).get(j);
                p+=1;
            }
        }
        /*
        p = 0;

        for (int t = 0; t<mNumberOfThreads;t++){
            for (int j = 0; j < local_last_hops.get(t).size(); j++) {
                lastHops[p] = local_last_hops.get(t).get(j);
                p+=1;
            }
        }

         */
        for (int i = 0; i < mGraph.numNodes(); i++) {
            for (int j = 0; j < mNumberOfThreads; j++) {
                if (mDoCentrality) {
                    harmonic[i] += local_harmonic.get(j)[i];
                    farness[i] += local_farness.get(j)[i];
                }
            }
            if (mDoCentrality) {
                farness[i] = farness[i] * mGraph.numNodes() / mNumSeeds;
                harmonic[i] = harmonic[i] * mGraph.numNodes() / ((mGraph.numNodes() - 1) * mNumSeeds);
            }
        }
        //for (int i = 0; i < collisionsMatrix.length; i++)  logger.debug("collision matrix at index {} =  {} ",i,collisionsMatrix[i]);
        // The problem is here, we need to reduce the hop table in a different way
        //normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter);


        normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter);


        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);


        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        if(mDoCentrality){
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


    /**
     * Execution of the MultithreadBMinHash algorithm
     *
     * @return Computed metrics of the algorithm
     */
    public Measure runAlgorithm_tmp() {
        startTime = System.currentTimeMillis();
        long totalTime;

        //logger.debug("Number of threads to be used {}", mNumberOfThreads);

        int[][] collisionsMatrix = new int[mNumSeeds][];
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;

        int lowerboundDiameter = 0;

        ExecutorService executor = Executors.newFixedThreadPool(mNumSeeds); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<>(this.mNumSeeds);

        for (int i = 0; i < this.mNumSeeds; i++) {
            todo.add(new IterationThread(mGraph.copy(), i));
        }

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

        for (int i = 0; i < collisionsMatrix.length; i++)  logger.debug("collision matrix at index {} =  {} ",i,collisionsMatrix[i]);
        logger.debug("------------------");
        normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter);
        for (int i = 0; i < collisionsMatrix.length; i++)  logger.debug("collision matrix after norm at index {} =  {} ",i,collisionsMatrix[i]);
        //for(int i = 0;i<collisionsMatrix.length;i++) {
        //    logger.debug(" cm [{}] = {}",i,collisionsMatrix[i]);
        //}


        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);

        logger.debug("Hop table array is {}", hopTableArray);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        if(mDoCentrality){
            int[] farness = farnessArray(mHopForNodes);
            float[] inverseFarness = inverseFarnessArray(mHopForNodes);
            graphMeasure.setFarness(farness);
            graphMeasure.setInverseFarness(inverseFarness);
            //graphMeasure.setClosenessCentrality(Stats.ClosenessCentrality(mGraph.numNodes(),mNumSeeds, farness,true));
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

    class IterationThread implements Callable<int[]> {

        private final ImmutableGraph g;
        private final int s;

        public IterationThread(ImmutableGraph g, int s) {
            this.g = g;
            this.s = s;
        }

        @Override
        public int[] call() {
            long startSeedTime = System.currentTimeMillis();
            long lastLogTime = startSeedTime;
            long logTime;

            int collisions = 0;

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
                    mutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
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
                                    if (mDoCentrality) {
                                        mHopForNodes[n][s] = (short) h;
                                    }
                                    signatureIsChanged = true;
                                    break;
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
                                            TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)),
                                            TimeUnit.MILLISECONDS.toSeconds(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)) -
                                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)))));
                            lastLogTime = logTime;
                        }
                    }
                }


                // count the collision between the node signature and the graph signature
                if (signatureIsChanged) {
                    collisions = 0;
                    for (int aMutable : mutable) {
                        collisions += Integer.bitCount(aMutable);
                    }
                    logger.debug(" random node {} hop {} collisions {}",randomNode,h,collisions);

                    int[] copy = new int[h + 1];
                    System.arraycopy(hopTable, 0, copy, 0, hopTable.length);
                    hopTable = copy;

                    hopTable[h] = collisions;

                    h += 1;
                }
            }

            MultithreadBMinHash.this.mSeedTime[s] = System.currentTimeMillis() - startSeedTime;

            return hopTable;
        }


        private int lengthBitsArray(int numberOfNodes) {
            return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
        }

    }

}
