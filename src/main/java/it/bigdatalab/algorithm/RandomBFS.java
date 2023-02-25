package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class RandomBFS {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.RandomBFS");

    private final int mNumberOfThreads;
    private final double[] mSeedTime;
    private long startTime;

    protected int mNumSeeds;
    protected double mThreshold;

    protected ImmutableGraph mGraph;
    protected int[] mMinHashNodeIDs;
    private boolean doCentrality;

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
        this.doCentrality = centrality;
    }

    /**
     * Creates a new RandomBFS instance with default values
     */
    public RandomBFS(final ImmutableGraph g, int numSeeds, double threshold, int threads, boolean centrality) {

        this.mNumSeeds = numSeeds;
        this.mThreshold = threshold;
        this.mGraph = g;
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        mSeedTime = new double[mNumSeeds];
        doCentrality = centrality;
    }

    /**
     * Execution of the RandomBFS algorithm
     *
     * @return Computed metrics of the algorithm
     */
    public Measure runAlgorithm() {
        startTime = System.currentTimeMillis();
        long totalTime;

        int[][] collisionsMatrix = new int[mNumSeeds][];
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;

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

        normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter);
        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setLowerBoundDiameter(lowerboundDiameter);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setSeedsTime(mSeedTime);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTableArray));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTableArray, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTableArray));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTableArray, mThreshold));

        return new GraphMeasureOpt();
    }

    /***
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    // todo remove it from here and move it in a superclass
    public void normalizeCollisionsTable(int[][] collisionsMatrix, int lowerBound) {

        for (int i = 0; i < collisionsMatrix.length; i++) { // check last hop of each seed
            // if last hop is not the lower bound
            // replace the 0 values from last hop + 1 until lower bound
            // with the value of the previous hop for the same seed
            if (collisionsMatrix[i].length - 1 < lowerBound) {
                int oldLen = collisionsMatrix[i].length;
                int[] copy = new int[lowerBound + 1];
                System.arraycopy(collisionsMatrix[i], 0, copy, 0, collisionsMatrix[i].length);
                collisionsMatrix[i] = copy;
                for (int j = oldLen; j <= lowerBound; j++) {
                    collisionsMatrix[i][j] = collisionsMatrix[i][j - 1];
                }
            }
        }
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    // todo remove it from here and move it in a superclass
    public double[] hopTable(int[][] collisionsMatrix, int lowerBound) {
        long sumCollisions;
        double couples;
        double[] hoptable = new double[lowerBound + 1];
        // lower bound is the max size of inner array
        for (int hop = 0; hop < lowerBound + 1; hop++) {
            sumCollisions = 0;
            for (int[] matrix : collisionsMatrix) {
                sumCollisions += matrix[hop];
            }
            couples = ((double) sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hoptable[hop] = couples;
        }
        return hoptable;
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
            long lastLogTime = startSeedTime;
            long logTime;

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            int[] hopTable = new int[1];
            hopTable[0] = 0;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(g.numNodes())];

            int randomNode = mMinHashNodeIDs[s];

            int[] ball = new int[1];
            ball[0] = randomNode;

            logger.debug("First node is {}", ball[0]);

            // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
            // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
            // This is equal to logical and operation.
            // remaremainderPositionRandomNode contains the bit index of the node
            int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
            // quotient is randomNode >>> MASK and give us the position of the node in the array
            // i.e if the actual node is 16 and we use an array of int (32 bit lenght for each cell) then
            // the node is at index 0 of the array of the first int from 0 to 31
            mutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;

            int h = 0;
            int level = 1;

            int nodesAtDistanceHNext = 0;
            while(ball.length != 0) {

                // remove the first element from the head
                int node = ball[0];
                int[] cBall = new int[ball.length - 1];
                System.arraycopy(ball, 1, cBall, 0, ball.length-1);
                ball = cBall;
                logger.debug("Ball is {}", ball);

                final int d = g.outdegree(node);
                final int[] successors = g.successorArray(node);

                int bitNeigh;
                for (int l = 0; l < d; l++) { // for each neighbour of the node
                    final int neighbour = successors[l];
                    logger.debug("Neigh is {}", neighbour);

                    int quotientNeigh = neighbour >>> Constants.MASK;
                    int remainderPositionNeigh = (neighbour << Constants.REMAINDER) >>> Constants.REMAINDER;

                    bitNeigh = (((1 << remainderPositionNeigh) & mutable[quotientNeigh]) >>> remainderPositionNeigh);
                    logger.debug("bit neigh is {}", bitNeigh);

                    if(bitNeigh == 0) { // neighbour is not yet been visited
                        nodesAtDistanceHNext += 1;

                        // add the neighbour node to the ball
                        logger.debug("ball lenght is {}", ball.length);
                        int[] copy = new int[ball.length + 1];
                        logger.debug("copy lenght is {}", copy.length);
                        System.arraycopy(ball, 0, copy, 0, ball.length);
                        logger.debug("copy is now {}", ball);
                        ball = copy;
                        ball[ball.length - 1] = neighbour;
                        logger.debug("ball is now {}", ball);
                        mutable[quotientNeigh] |= (Constants.BIT) << remainderPositionNeigh;
                    }
                }

                level -= 1;
                if(level == 0) {
                    h += 1;
                    level = ball.length;
                    int[] cHopTable = new int[h+1];
                    System.arraycopy(hopTable, 0, cHopTable, 0, hopTable.length);
                    hopTable = cHopTable;
                    hopTable[h] = nodesAtDistanceHNext;
                    logger.debug("hop table is {}", hopTable);

                }
            }
            return hopTable;
        }

        private int lengthBitsArray(int numberOfNodes) {
            return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
        }
    }
}