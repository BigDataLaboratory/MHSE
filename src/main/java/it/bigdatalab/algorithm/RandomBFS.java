package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class RandomBFS  {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.RandomBFS");

    private final int mNumberOfThreads;
    private final double[] mSeedTime;

    protected int mNumSeeds;
    protected double mThreshold;

    protected ImmutableGraph mGraph;
    protected int[] mMinHashNodeIDs;
    private boolean mDoCentrality;
    private int[][] mHopForNodes;


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
     * Creates a new RandomBFS instance with default values
     */
    public RandomBFS(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes, int threads, boolean centrality) {

        if (numSeeds != (nodes != null ? nodes.length : 0)) {
            assert nodes != null;
            throw new MinHash.SeedsException("Specified different number of seeds in properties. \"randomBFS.numSeeds\" is " + mNumSeeds + " and length of seeds list is " + nodes.length);
        }
        this.mNumSeeds = numSeeds;
        this.mThreshold = threshold;
        this.mGraph = g;
        this.mMinHashNodeIDs = nodes;
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mSeedTime = new double[mNumSeeds];
        this.mDoCentrality = centrality;
    }

    /**
     * Creates a new RandomBFS instance with default values
     */
    public RandomBFS(final ImmutableGraph g, int numSeeds, double threshold, int threads, boolean centrality) {
        this.mNumSeeds = numSeeds;
        this.mThreshold = threshold;
        this.mGraph = g;
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mSeedTime = new double[mNumSeeds];
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
        this.mDoCentrality = centrality;
    }

    /**
     * Execution of the RandomBFS algorithm
     *
     * @return Computed metrics of the algorithm
     */
    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;

        int[][] collisionsMatrix = new int[mNumSeeds][];
        int[][] distancesMatrix = new int[0][];
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;
        if (mDoCentrality) {
            mHopForNodes = new int[mGraph.numNodes()][mNumSeeds];
        }
        int lowerboundDiameter = 0;

        logger.debug("Number of threads to be used {}", mNumberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<RandomBFS.IterationThread> todo = new ArrayList<>(this.mNumSeeds);

        for (int i = 0; i < this.mNumSeeds; i++) {
            todo.add(new RandomBFS.IterationThread(mGraph.copy(), i));
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
                    }catch (ExecutionException e) {
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

        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setLowerBoundDiameter(lowerboundDiameter);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setSeedsTime(mSeedTime);
        graphMeasure.setTime(totalTime);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTableArray));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTableArray, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTableArray));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTableArray, mThreshold));
        if(mDoCentrality){
            double [] farness = farnessArray(mHopForNodes);
            graphMeasure.setClosenessCentrality(Stats.ClosenessCentrality(mGraph.numNodes(),mNumSeeds,farness));
            graphMeasure.setHarmonicCentrality(Stats.HarmonicCentrality(mGraph.numNodes(),mNumSeeds,farness));
            graphMeasure.setLinnCentrality(Stats.LinnCentrality(mGraph.numNodes(),mNumSeeds,farness,hopTableArray));
        }
        return graphMeasure;
    }

    /***
     * Compute the hop table for reachable pairs within h hops
     * @return hop table
     */
    // todo remove it from here and move it in a superclass
    public double[] hopTable(int[][] collisionsMatrix, int lowerBound) {
        long accumulator = 0;
        double couples;

        double[] hoptable = new double[lowerBound + 1];
        for(int hop = 0; hop < lowerBound + 1; hop ++) {
            for (int[] matrix : collisionsMatrix) {
                if (matrix.length > hop)
                    accumulator += matrix[hop];
            }
            couples = ((double) accumulator * mGraph.numNodes()) / this.mNumSeeds;
            hoptable[hop] = couples;
        }
        return hoptable;
    }

    public double[] farnessArray(int [][] hopMatrix ){
        int i,j;
        double [] fareness = new double[mGraph.numNodes()];
        Arrays.fill(fareness,0);
        for (i = 0; i < mGraph.numNodes(); i++){
            for (j = 0; j < this.mNumSeeds; j++){
                fareness[i] += hopMatrix[i][j];
            }
        }
        return fareness;

    }
    class IterationThread implements Callable<int[]> {

        private final ImmutableGraph g;
        private final int s;

        public IterationThread(ImmutableGraph g, int s) {
            this.g = g;
            this.s = s;
        }

        @Override
        public int[] call() throws Exception {
            long startSeedTime = System.currentTimeMillis();

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            int[] hopTable = new int[1];
            hopTable[0] = 1;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] distances = new int[g.numNodes()];
            Arrays.fill(distances, -1);

            int randomNode = mMinHashNodeIDs[s];
            distances[randomNode] = 0;
            Queue<Integer> ball = new LinkedList<>();
            ball.add(randomNode);

            int h = 0;
            int level = 1;

            int nodesAtDistanceHNext = 0;
            while(ball.size() != 0) {

                // remove the first element from the head
                int node = ball.remove();

                final int d = g.outdegree(node);
                final int[] successors = g.successorArray(node);

                for (int l = 0; l < d; l++) { // for each neighbour of the node
                    final int neighbour = successors[l];
                    if(distances[neighbour] == -1) {
                        distances[neighbour] = distances[node] + 1;
                        ball.add(neighbour);
                        nodesAtDistanceHNext += 1;
                        if (mDoCentrality){
                            mHopForNodes[neighbour][s] = distances[neighbour];
                        }
                    }
                }

                level -= 1;
                if(level == 0 && ball.size() != 0) {
                    h += 1;
                    level = ball.size();
                    int[] cHopTable = new int[h+1];
                    System.arraycopy(hopTable, 0, cHopTable, 0, hopTable.length);
                    hopTable = cHopTable;
                    hopTable[h] = nodesAtDistanceHNext;
                    nodesAtDistanceHNext = 0;
                }
            }

            RandomBFS.this.mSeedTime[s] = System.currentTimeMillis() - startSeedTime;
            return hopTable;
        }

        private int lengthBitsArray(int numberOfNodes) {
            return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
        }
    }
}