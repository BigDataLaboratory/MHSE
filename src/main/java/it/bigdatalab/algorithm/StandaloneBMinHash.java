package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class StandaloneBMinHash extends MinHash {

    private Int2LongSortedMap mTotalCollisions;
    private Int2ObjectOpenHashMap<int[]> collisionsTable;
    private int[] lastHops;
    private int[] totalCollisionsPerHashFunction;

    private static final int MASK = 6; // 2^6
    private static final int REMAINDER = 58;
    private static final long BIT = 1;

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.StandaloneBMinHash");


    /**
     * Creates a new BooleanMinHash instance with default values
     */
    public StandaloneBMinHash() throws DirectionNotSetException, SeedsException, IOException {
        super();
        mTotalCollisions = new Int2LongLinkedOpenHashMap();
        collisionsTable = new Int2ObjectOpenHashMap<int[]>();       //for each hop a list of collisions for each hash function
        lastHops = new int[numSeeds];                           //for each hash function, the last hop executed
        totalCollisionsPerHashFunction = new int[numSeeds];     //for each hash function get the number of total collisions


        if(isSeedsRandom){
            for(int i = 0;i<numSeeds; i++){
                minHashNodeIDs[i] = ThreadLocalRandom.current().nextInt(0, mGraph.numNodes());
            }
        } else {
            //Load minHash node IDs from properties file
            String propertyNodeIDRange = "minhash.nodeIDRange";
            String propertyName = "minhash.nodeIDs";
            String minHashNodeIDsString = PropertiesManager.getProperty(propertyName);
            String minHashNodeIDRangeString = PropertiesManager.getProperty(propertyNodeIDRange);

            if (!minHashNodeIDRangeString.equals("")) {
                int[] minHashNodeIDRange = Arrays.stream(minHashNodeIDRangeString.split(",")).mapToInt(Integer::parseInt).toArray();
                minHashNodeIDs = IntStream.rangeClosed(minHashNodeIDRange[0], minHashNodeIDRange[1]).toArray();
            } else {
                minHashNodeIDs = Arrays.stream(minHashNodeIDsString.split(",")).mapToInt(Integer::parseInt).toArray();
            }
            if (numSeeds != minHashNodeIDs.length) {
                String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + numSeeds + " and \"" + propertyName + "\" length is " + minHashNodeIDs.length;
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
        int lowerBoundDiameter = 0;
        int previousLowerBoundDiameter;

        for (int i = 0; i < this.numSeeds; i++) {

            logger.info("Starting computation on seed {}", i);
            Int2LongSortedMap hopCollision;
            int collisions = 0;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            long[] mutable = new long[lengthBitsArray(mGraph.numNodes())];
            long[] immutable = new long[lengthBitsArray(mGraph.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = minHashNodeIDs[i];

            // initialization of the collision "collisions" for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            hopCollision = new Int2LongLinkedOpenHashMap();
            int h = 0;
            boolean signatureIsChanged = true;

            while(signatureIsChanged) {
                logger.debug("(seed {}) Starting computation on hop {}", i, h);

                int[] hopCollisions;
                if (collisionsTable.containsKey(h)) {
                    hopCollisions = collisionsTable.get(h);
                } else {
                    hopCollisions = new int[numSeeds];
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
                    for (int c = 0; c < mutable.length; c++) {
                        collisions += Long.bitCount(mutable[c]);
                    }

                    hopCollisions[i] = collisions;      //relativa al seed i per l'hop h
                    collisionsTable.put(h, hopCollisions);
                    logger.debug("Number of collisions: {}", collisions);       //conteggio giusto
                    lastHops[i] = h;
                    long previousValue = mTotalCollisions.get(h);
                    logger.debug("(seed {}) Hop Collision {}", i, hopCollision);
                    mTotalCollisions.put(h, previousValue + collisions);
                    logger.info("# seed {} # hop: {} \n total collisions table {}", i, (h), mTotalCollisions);
                    h += 1;
                }
            }

            logger.info("Total number of collisions for seed n.{} : {}", i, collisions);
            totalCollisionsPerHashFunction[i] = collisions;

            if((h-1) > lowerBoundDiameter) {
                previousLowerBoundDiameter = lowerBoundDiameter;
                lowerBoundDiameter = h-1;
                // new lower bound diameter founded, normalize all the total collision hash computed
                for(int j = 0; j<i; j++) {
                    // add the missing number of collisions from the previous lower bound diameter to the new founded
                    for (int k = lowerBoundDiameter; k > previousLowerBoundDiameter; k--) { //all hops between previousLowerBoundDiameter and lowerBoundDiameter
                        long previousValue = mTotalCollisions.get(k);
                        mTotalCollisions.put(k, previousValue + totalCollisionsPerHashFunction[j]);
                    }
                }
            } else if((h - 1) < lowerBoundDiameter) {
                for(int k = lowerBoundDiameter; k > (h - 1); k--) {
                    long previousValue = mTotalCollisions.get(k);
                    mTotalCollisions.put(k, previousValue + collisions);
                }
            }
            logger.debug("(seed {}) Collision table {}", i, mTotalCollisions);
            logger.debug("Ended computation on seed {}", i);
        }

        logger.info("Starting computation of the hop table from collision table");
        hopTable = hopTable();
        logger.info("Computation of the hop table completed");

        //normalize collisionsTable
        normalizeCollisionsTable();

        GraphMeasure graphMeasure = new GraphMeasure(hopTable);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setNumSeeds(numSeeds);
        graphMeasure.setCollisionsTable(collisionsTable);
        graphMeasure.setLastHops(lastHops);

        String minHashNodeIDsString = "";
        String separator = ",";
        for(int i = 0; i < numSeeds; i++){
            minHashNodeIDsString += (minHashNodeIDs[i] + separator);
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
    private Int2DoubleSortedMap hopTable() {
        Int2DoubleSortedMap hopTable = new Int2DoubleLinkedOpenHashMap();
        mTotalCollisions.forEach((key, value) -> {
            Double r = ((double) (value * mGraph.numNodes()) / this.numSeeds);
            hopTable.put(key.intValue(), r.doubleValue());
        });
        return hopTable;
    }

    /***
     * TODO Optimizable?
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    private void normalizeCollisionsTable() {
        int lowerBoundDiameter = collisionsTable.size() - 1;
        logger.debug("Diameter: " + lowerBoundDiameter);

        //Start with hop 1
        //There is no check for hop 0 because at hop 0 there is always (at least) 1 collision, never 0.
        for (int i = 1; i <= lowerBoundDiameter; i++) {
            int[] previousHopCollisions = collisionsTable.get(i - 1);
            int[] hopCollisions = collisionsTable.get(i);
            //TODO first if is better for performance?
            if (Arrays.stream(hopCollisions).anyMatch(coll -> coll == 0)) {
                for (int j = 0; j < hopCollisions.length; j++) {
                    if (hopCollisions[j] == 0) {
                        hopCollisions[j] = previousHopCollisions[j];
                    }
                }
            }
            collisionsTable.put(i, hopCollisions);
        }
    }

}
