package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class StandaloneBMinHashOptimized extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.StandaloneBMinHashOptimized");
    private static final int N_ROWS = 5;
    private static final int MASK = 6; // 2^6
    private static final int REMAINDER = 58;
    private static final long BIT = 1;

    /**
     * Creates a new BooleanMinHash instance with default values
     */
    StandaloneBMinHashOptimized() throws DirectionNotSetException, SeedsException, IOException {
        super();

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
    }

    /**
     * Execution of the StandaloneBMinHash algorithm
     *
     * @return Computed metrics of the algorithm
     */
    public GraphMeasure runAlgorithm() {
        // seed as rows, hop as columns - cell values are collissions for each hash function at hop
        int[][] collisionsMatrix = new int[mNumSeeds][N_ROWS];
        //for each hash function, the last hop executed
        int[] lastHops = new int[mNumSeeds];
        double[] mHopTableArray;

        int lowerBound = 0;

        for (int i = 0; i < this.mNumSeeds; i++) {

            logger.info("Starting computation on seed {}", i);

            int collisions = 0;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            long[] mutable = new long[lengthBitsArray(mGraph.numNodes())];
            long[] immutable = new long[lengthBitsArray(mGraph.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = mMinHashNodeIDs[i];

            // initialization of the collision "collisions" for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            int h = 0;
            boolean signatureIsChanged = true;

            while (signatureIsChanged) {
                logger.debug("(seed {}) Starting computation on hop {}", i, h);


                if (collisionsMatrix[i] != null && collisionsMatrix[i].length - 1 > h) {
                } else {
                    int[][] copy = new int[mNumSeeds][collisionsMatrix[i].length + N_ROWS];
                    for (int height = 0; height < mNumSeeds; height++) {
                        copy[height] = new int[collisionsMatrix[height].length + N_ROWS];
                        System.arraycopy(collisionsMatrix[height], 0, copy[height], 0, collisionsMatrix[height].length);
                        collisionsMatrix[height] = copy[height];
                    }
                }

                //first hop - initialization
                if (h == 0) {

                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = ((randomNode << REMAINDER) >>> REMAINDER);
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> MASK] |= (BIT) << remainderPositionRandomNode;
                    signatureIsChanged = true;
                } else {   //next hops
                    signatureIsChanged = false;

                    // copy all the actual nodes hash in a new structure
                    System.arraycopy(mutable, 0, immutable, 0, mutable.length);

                    long remainderPositionNode;
                    int quotientNode;
                    for (int n = 0; n < mGraph.numNodes(); n++) {

                        final int node = n;

                        final int d = mGraph.outdegree(node);
                        final int[] successors = mGraph.successorArray(node);

                        // update the node hash iterating over all its neighbors
                        // and computing the OR between the node signature and
                        // the neighbor signature.
                        // store the new signature as the current one
                        remainderPositionNode = (node << REMAINDER) >>> REMAINDER;
                        quotientNode = node >>> MASK;

                        long value = immutable[quotientNode];
                        long bitNeigh;
                        long nodeMask = (1L << remainderPositionNode);

                        if (((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                            for (int l = 0; l < d; l++) {
                                final int neighbour = successors[l];
                                int quotientNeigh = neighbour >>> MASK;
                                long remainderPositionNeigh = (neighbour << REMAINDER) >>> REMAINDER;

                                bitNeigh = (((1L << remainderPositionNeigh) & immutable[quotientNeigh]) >>> remainderPositionNeigh) << remainderPositionNode;
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

                if (signatureIsChanged) {
                    // count the collision between the node signature and the graph signature
                    collisions = 0;
                    for (long aMutable : mutable) {
                        collisions += Long.bitCount(aMutable);
                    }

                    collisionsMatrix[i][h] = collisions;

                    logger.debug("Number of collisions: {}", collisions);
                    lastHops[i] = h;
                    if (h > lowerBound)
                        lowerBound = h;
                    h += 1;
                }
            }

            logger.info("Total number of collisions for seed n.{} : {}", i, collisions);
            logger.debug("Ended computation on seed {}", i);
        }


        //normalize collisionsTable
        normalizeCollisionsTable(collisionsMatrix, lowerBound, lastHops);

        logger.info("Starting computation of the hop table from collision table");
        mHopTableArray = hopTable(collisionsMatrix, lowerBound);
        logger.info("Computation of the hop table completed");

        GraphMeasure graphMeasure = new GraphMeasure(mHopTableArray, lowerBound);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        graphMeasure.setLastHops(lastHops);

        String minHashNodeIDsString = "";
        String separator = ",";
        for (int i = 0; i < mNumSeeds; i++) {
            minHashNodeIDsString += (mMinHashNodeIDs[i] + separator);
        }
        graphMeasure.setMinHashNodeIDs(minHashNodeIDsString);
        return graphMeasure;
    }

    private int lengthBitsArray(int numberOfNodes) {
        return (int) Math.ceil(numberOfNodes / (double) Long.SIZE);
    }


    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    private double[] hopTable(int[][] collisionsMatrix, int lowerBound) {
        Int2DoubleSortedMap hopTable = new Int2DoubleLinkedOpenHashMap();
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


    /***
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    private void normalizeCollisionsTable(int[][] collisionsMatrix, int lowerBound, int[] last) {
        logger.debug("Diameter: " + lowerBound);

        for (int i = 0; i < last.length; i++) { // check last hop of each seed
            // if last hop is not the lower bound
            // replace the 0 values from last hop + 1 until lower bound
            // with the value of the previous hop for the same seed
            if (last[i] < lowerBound) {
                for (int j = last[i] + 1; j <= lowerBound; j++) {
                    collisionsMatrix[i][j] = collisionsMatrix[i][j - 1];
                }
            }
        }

    }

}
