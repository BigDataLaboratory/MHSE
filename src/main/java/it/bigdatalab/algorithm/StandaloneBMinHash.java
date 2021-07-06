package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of StandaloneBMinHash (MinHash Signature Estimation standalone boolean version) algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class StandaloneBMinHash extends BMinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.StandaloneBMinHash");

    /**
     * Creates a new BooleanMinHash instance with default values
     */
    public StandaloneBMinHash(final CompressedGraph g, int numSeeds, double threshold, int[] nodes) {
        super(g, numSeeds, threshold, nodes);
    }

    /**
     * Creates a new BooleanMinHash instance with default values
     */
    public StandaloneBMinHash(final CompressedGraph g, int numSeeds, double threshold) {
        super(g, numSeeds, threshold);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
    }

    /**
     * Execution of the StandaloneBMinHash algorithm
     *
     * @return Computed metrics of the algorithm
     */
    public Measure runAlgorithm() {
        logger.info("Running {} algorithm", StandaloneBMinHash.class.getCanonicalName());
        long startTime = System.currentTimeMillis();
        long lastLogTime = startTime;
        long logTime;
        long totalTime;

        Int2DoubleLinkedOpenHashMap hopTable;
        //for each hop a list of collisions for each hash function
        Int2ObjectOpenHashMap<int[]> collisionsTable = new Int2ObjectOpenHashMap<>();   //key is hop, value is collisions for each hash function at that hop
        //for each hash function, the last hop executed
        int[] lastHops = new int[mNumSeeds];

        for (int s = 0; s < this.mNumSeeds; s++) {

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
            int randomNode = mMinHashNodeIDs[s];

            int h = 0;
            boolean signatureIsChanged = true;

            while (signatureIsChanged) {
                //logger.debug("(seed {}) Starting computation on hop {}", s, h);

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
                        final int [] successors = mGraph.get_neighbours(node,true);
                        int d = successors.length;

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

                        logTime = System.currentTimeMillis();
                        if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                            int maxHop = Arrays.stream(lastHops).summaryStatistics().getMax() + 1;
                            logger.info("(seed # {}) # nodes analyzed {} / {} for hop {} / {} (upper bound), estimated time remaining {}",
                                    s + 1,
                                    n, mGraph.numNodes(),
                                    h + 1, maxHop,
                                    String.format("%d min, %d sec",
                                            TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - startTime)) / (s + 1)) - (logTime - startTime)),
                                            TimeUnit.MILLISECONDS.toSeconds(((mNumSeeds * (logTime - startTime)) / (s + 1)) - (logTime - startTime)) -
                                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - startTime)) / (s + 1)) - (logTime - startTime)))));
                            lastLogTime = logTime;
                        }
                    }
                }

                if (signatureIsChanged) {
                    // count the collision between the node signature and the graph signature
                    collisions = 0;
                    for (int aMutable : mutable) {
                        collisions += Integer.bitCount(aMutable);
                    }

                    hopCollisions[s] = collisions;      //related to seed s at hop h
                    collisionsTable.put(h, hopCollisions);
                    lastHops[s] = h;
                    h += 1;
                }
            }
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        //normalize collisionsTable
        normalizeCollisionsTable(collisionsTable);

        hopTable = hopTable(collisionsTable);

        GraphMeasure graphMeasure = new GraphMeasure();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setHopTable(hopTable);
        graphMeasure.setLowerBoundDiameter(hopTable.size() - 1);
        graphMeasure.setThreshold(mThreshold);
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
