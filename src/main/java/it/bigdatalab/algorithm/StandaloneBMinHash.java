package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static it.bigdatalab.utils.Constants.MASK;
import static it.bigdatalab.utils.Constants.REMAINDER;

public class StandaloneBMinHash extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.StandaloneBMinHash");

    /**
     * Creates a new BooleanMinHash instance with default values
     */
    public StandaloneBMinHash(String inputFilePath, boolean isSeedsRandom, boolean isolatedVertices, String direction, int numSeeds, double threshold) throws DirectionNotSetException, SeedsException, IOException {
        super(inputFilePath, isolatedVertices, direction, numSeeds, threshold);

        if (isSeedsRandom) {
            for (int i = 0; i < mNumSeeds; i++)
                mMinHashNodeIDs[i] = ThreadLocalRandom.current().nextInt(0, mGraph.numNodes());
        }
        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
    }

    /**
     * Execution of the StandaloneBMinHash algorithm
     *
     * @return Computed metrics of the algorithm
     */
    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;

        Int2DoubleLinkedOpenHashMap hopTable;
        //for each hop a list of collisions for each hash function
        Int2ObjectOpenHashMap<int[]> collisionsTable = new Int2ObjectOpenHashMap<>();   //key is hop, value is collisions for each hash function at that hop
        //for each hash function, the last hop executed
        int[] lastHops = new int[mNumSeeds];

        for (int i = 0; i < this.mNumSeeds; i++) {

            logger.info("Starting computation on seed {}", i);

            Int2LongSortedMap hopCollision;
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
            hopCollision = new Int2LongLinkedOpenHashMap();
            int h = 0;
            boolean signatureIsChanged = true;

            while(signatureIsChanged) {
                logger.debug("(seed {}) Starting computation on hop {}", i, h);

                int[] hopCollisions;
                if (collisionsTable.containsKey(h)) {
                    hopCollisions = collisionsTable.get(h);
                } else {
                    hopCollisions = new int[mNumSeeds];
                }

                //first hop - initialization
                if (h == 0) {
                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = ((randomNode << REMAINDER) >>> REMAINDER);
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> MASK] |= (Constants.BIT) << remainderPositionRandomNode;
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

                    hopCollisions[i] = collisions;      //related to seed i at hop h
                    collisionsTable.put(h, hopCollisions);
                    logger.debug("Number of collisions: {}", collisions);       //conteggio giusto
                    lastHops[i] = h;
                    logger.debug("(seed {}) Hop Collision {}", i, hopCollision);
                    h += 1;
                }
            }

            logger.info("Total number of collisions for seed n.{} : {}", i, collisions);
            logger.debug("Ended computation on seed {}", i);
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        //normalize collisionsTable
        collisionsTable = normalizeCollisionsTable(collisionsTable);

        logger.info("Starting computation of the hop table from collision table");
        hopTable = hopTable(collisionsTable);
        logger.info("Computation of the hop table completed");

        GraphMeasure graphMeasure = new GraphMeasure(hopTable, mThreshold);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsTable(collisionsTable);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setTime(totalTime);
        graphMeasure.setDirection(mDirection);

        return graphMeasure;
    }

    private int lengthBitsArray(int numberOfNodes) {
        return (int) Math.ceil(numberOfNodes / (double) Long.SIZE);
    }


    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    private Int2DoubleLinkedOpenHashMap hopTable(Int2ObjectOpenHashMap<int[]> collisionsTable) {
        Int2DoubleLinkedOpenHashMap hopTable = new Int2DoubleLinkedOpenHashMap();
        int lastHop = collisionsTable.size() - 1;
        long sumCollisions = 0;

        for(int hop = 0; hop <= lastHop; hop++){
            int[] collisions = collisionsTable.get(hop);
            sumCollisions = Arrays.stream(collisions).sum();
            double couples = (double) (sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hopTable.put(hop, couples);
            logger.info("hop " + hop + " total collisions " + Arrays.stream(collisions).sum() + " couples: " + couples);
        }
        return hopTable;
    }

    /***
     * TODO Optimizable?
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    private Int2ObjectOpenHashMap<int[]> normalizeCollisionsTable(Int2ObjectOpenHashMap<int[]> ct) {
        int lowerBoundDiameter = ct.size() - 1;

        //Start with hop 1
        //There is no check for hop 0 because at hop 0 there is always (at least) 1 collision, never 0.
        for (int i = 1; i <= lowerBoundDiameter; i++) {
            int[] previousHopCollisions = ct.get(i - 1);
            int[] hopCollisions = ct.get(i);
            //TODO first if is better for performance?
            if (Arrays.stream(hopCollisions).anyMatch(coll -> coll == 0)) {
                for (int j = 0; j < hopCollisions.length; j++) {
                    if (hopCollisions[j] == 0) {
                        hopCollisions[j] = previousHopCollisions[j];
                    }
                }
            }
            ct.put(i, hopCollisions);
        }
        return ct;
    }

}
