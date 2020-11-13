package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class MultithreadBMinHashOptimized extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHashOptimized");

    private static final int N = 5;
    private static final int MASK = 5; // 2^6
    private static final int REMAINDER = 27;
    private static final int BIT = 1;

    private int mNumberOfThreads;
    private double[] mSeedTime;

    /**
     * Creates a new MultithreadBMinHashOptimized instance with default values
     */
    public MultithreadBMinHashOptimized() throws DirectionNotSetException, SeedsException, IOException {
        super();
        mSeedTime = new double[mNumSeeds];

        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("minhash.suggestedNumberOfThreads"));
        logger.info("Number of threads selected {}", suggestedNumberOfThreads);

        if (mIsSeedsRandom) {
            for (int i = 0; i < mNumSeeds; i++) {
                mMinHashNodeIDs[i] = ThreadLocalRandom.current().nextInt(0, mGraph.numNodes());
            }
        } else {
            //Load minHash node IDs from properties file
            String propertyNodeIDRange = "minhash.nodeIDRange";
            String propertyName = "minhash.nodeIDs";
            String minHashNodeIDsString = PropertiesManager.getProperty(propertyName);
            String minHashNodeIDRangeString = PropertiesManager.getProperty(propertyNodeIDRange);
            if (!minHashNodeIDRangeString.equals("")) {
                int[] minHashNodeIDRange = Arrays.stream(minHashNodeIDRangeString.split(",")).mapToInt(Integer::parseInt).toArray();
                mMinHashNodeIDs = IntStream.rangeClosed(minHashNodeIDRange[0], minHashNodeIDRange[1]).toArray();
            } else {
                mMinHashNodeIDs = Arrays.stream(minHashNodeIDsString.split(",")).mapToInt(Integer::parseInt).toArray();

            }
            if (mNumSeeds != mMinHashNodeIDs.length) {
                String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + mNumSeeds + " and \"" + propertyName + "\" length is " + mMinHashNodeIDs.length;
                throw new SeedsException(message);
            }
        }

        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
        this.mNumberOfThreads = getNumberOfMaxThreads(suggestedNumberOfThreads);
    }


    /**
     * Execution of the MultithreadBMinHash algorithm
     *
     * @return Computed metrics of the algorithm
     */

    public Measure runAlgorithm() {
        logger.debug("Number of threads to be used {}", mNumberOfThreads);
        int[][] collisionsMatrix = new int[mNumSeeds][];
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;

        int lowerboundDiameter = 0;

        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<IterationThread>(this.mNumSeeds);

        for (int i = 0; i < this.mNumSeeds; i++) {
            todo.add(new IterationThread(mGraph, i));
        }

        try {
            List<Future<int[]>> futures = executor.invokeAll(todo);
            int count = 0;
            for (int i = 0; i < this.mNumSeeds; i++) {
                Future<int[]> future = futures.get(i);
                if (!future.isCancelled()) {
                    try {
                        logger.debug("Starting computation of collision table on seed {}", count);
                        int[] hopCollisions = future.get();
                        collisionsMatrix[i] = hopCollisions;

                        for (int h = hopCollisions.length - 1; h >= 0; h--) {
                            if (hopCollisions[h] == 0 && hopCollisions[h - 1] > 0) {
                                lastHops[i] = h - 1;
                                if ((h - 1) > lowerboundDiameter)
                                    lowerboundDiameter = h - 1;
                                break;
                            }
                        }
                        logger.debug("Collision table computation completed on seed {}!", count);
                        count++;

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

        normalizeCollisionsTable(collisionsMatrix, lowerboundDiameter, lastHops);

        logger.info("Starting computation of the hop table from collision table");
        hopTableArray = hopTable(collisionsMatrix, lowerboundDiameter);
        logger.debug("Hop table derived from collision table: {}", hopTableArray);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt(hopTableArray, lowerboundDiameter);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setSeedsTime(mSeedTime);

        String minHashNodeIDsString = "";
        String separator = ",";
        for (int i = 0; i < mNumSeeds; i++) {
            minHashNodeIDsString += (mMinHashNodeIDs[i] + separator);
        }
        graphMeasure.setMinHashNodeIDs(minHashNodeIDsString);
        return graphMeasure;
    }


    /**
     * Number of max threads to use for the computation
     *
     * @param suggestedNumberOfThreads if not equal to zero return the number of threads
     *                                 passed as parameter, else the number of max threads available
     * @return number of threads to use for the computation
     */
    private int getNumberOfMaxThreads(int suggestedNumberOfThreads) {
        if (suggestedNumberOfThreads != 0) return suggestedNumberOfThreads;
        return Runtime.getRuntime().availableProcessors();
    }


    /***
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    private void normalizeCollisionsTable(int[][] collisionsMatrix, int lowerBound, int[] last) {
        for (int i = 0; i < last.length; i++) { // check last hop of each seed
            // if last hop is not the lower bound
            // replace the 0 values from last hop + 1 until lower bound
            // with the value of the previous hop for the same seed
            if (last[i] < lowerBound) {

                int l = lowerBound + 1 < collisionsMatrix[i].length ? collisionsMatrix[i].length : lowerBound + 1;
                int copy[] = new int[l];
                System.arraycopy(collisionsMatrix[i], 0, copy, 0, collisionsMatrix[i].length);
                collisionsMatrix[i] = copy;

                for (int j = last[i] + 1; j <= lowerBound; j++) {
                    collisionsMatrix[i][j] = collisionsMatrix[i][j - 1];
                }
            }
        }
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    private double[] hopTable(int[][] collisionsMatrix, int lowerBound) {
        long sumCollisions;
        double couples;
        double[] hoptable = new double[lowerBound + 1];
        // lower bound is the max size of inner array
        for (int hop = 0; hop < lowerBound + 1; hop++) {
            sumCollisions = 0;
            for (int seed = 0; seed < collisionsMatrix.length; seed++) {
                sumCollisions += collisionsMatrix[seed][hop];
            }
            couples = (double) (sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hoptable[hop] = couples;
            logger.info("hop " + hop + " total collisions " + sumCollisions + " couples: " + couples);
        }
        return hoptable;
    }

    class IterationThread implements Callable<int[]> {

        private ImmutableGraph g;
        private int index;

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
            int[] hopTable = new int[N];


            while (signatureIsChanged) {
                logger.debug("(seed {}) Starting computation on hop {}", index, h);

                if (!(hopTable.length - 1 > h)) {
                    int[] copy = new int[hopTable.length + N];
                    System.arraycopy(hopTable, 0, copy, 0, hopTable.length);
                    hopTable = copy;
                }

                //first hop - initialization
                if (h == 0) {

                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = (randomNode << REMAINDER) >>> REMAINDER;
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> MASK] |= (BIT) << remainderPositionRandomNode;
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
                        remainderPositionNode = (node << REMAINDER) >>> REMAINDER;
                        quotientNode = node >>> MASK;
                        int value = immutable[quotientNode];
                        int bitNeigh;
                        int nodeMask = (1 << remainderPositionNode);

                        if (((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                            for (int l = 0; l < d; l++) {
                                final int neighbour = successors[l];
                                int quotientNeigh = neighbour >>> MASK;
                                long remainderPositionNeigh = (neighbour << REMAINDER) >>> REMAINDER;
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


