package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class StandaloneBMinHash extends BMinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.StandaloneBMinHash");

    /**
     * Creates a new BooleanMinHash instance with default values
     */
    public StandaloneBMinHash(final ImmutableGraph g, boolean isSeedsRandom, int numSeeds, double threshold) {
        super(g, numSeeds, threshold);

        if (isSeedsRandom) {
            for (int i = 0; i < mNumSeeds; i++)
                mMinHashNodeIDs[i] = ThreadLocalRandom.current().nextInt(0, mGraph.numNodes());
        }
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

            // initialization of the collision "collisions" for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            Int2LongSortedMap hopCollision = new Int2LongLinkedOpenHashMap();
            int collisions = 0;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(mGraph.numNodes())];
            int[] immutable = new int[lengthBitsArray(mGraph.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = mMinHashNodeIDs[i];

            int h = 0;
            boolean signatureIsChanged = true;

            while (signatureIsChanged) {
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
                    int remainderPositionRandomNode = ((randomNode << Constants.REMAINDER) >>> Constants.REMAINDER);
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
                    signatureIsChanged = true;
                } else {   //next hops
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
                    for (int aMutable : mutable) {
                        collisions += Integer.bitCount(aMutable);
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
        normalizeCollisionsTable(collisionsTable);

        logger.info("Starting computation of the hop table from collision table");
        hopTable = hopTable(collisionsTable);
        logger.info("Computation of the hop table completed");

        GraphMeasure graphMeasure = new GraphMeasure(hopTable, mThreshold);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsTable(collisionsTable);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setTime(totalTime);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTable));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTable, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTable));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTable, mThreshold));

        return graphMeasure;
    }

}
