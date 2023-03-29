package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of StandalonePropagateS (Propagate-S standalone boolean optimized version) algorithm
 */
public class StandalonePropagateS extends BMinHashOpt {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.StandaloneBMinHashOptimized");

    /**
     * Creates a new StandalonePropagateS instance with default values
     */
    public StandalonePropagateS(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes, boolean centrality) {
        super(g, numSeeds, threshold, nodes);
    }

    /**
     * Creates a new StandalonePropagateS instance with default values
     */
    public StandalonePropagateS(final ImmutableGraph g, int numSeeds, double threshold, boolean centrality) {
        this(g, numSeeds, threshold, null, centrality);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
    }

    /**
     * Execution of the StandalonePropagateS algorithm
     *
     * @return Computed metrics of the algorithm
     */
    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;
        long lastLogTime = startTime;
        long logTime;

        // seed as rows, hop as columns - cell values are collissions for each hash function at hop
        int[][] collisionsMatrix = new int[mNumSeeds][1];
        //for each hash function, the last hop executed
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;

        int lowerBound = 0;

        for (int s = 0; s < this.mNumSeeds; s++) {

            int collisions = 0;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(mGraph.numNodes())];
            int[] immutable = new int[lengthBitsArray(mGraph.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = mMinHashNodeIDs[s];

            // initialization of the collision "collisions" for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            int h = 0;
            boolean signatureIsChanged = true;

            while (signatureIsChanged) {

                //first hop - initialization
                if (h == 0) {

                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
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

                    int[] copy = new int[h + 1];
                    System.arraycopy(collisionsMatrix[s], 0, copy, 0, collisionsMatrix[s].length);
                    collisionsMatrix[s] = copy;

                    collisionsMatrix[s][h] = collisions;

                    lastHops[s] = h;
                    if (h > lowerBound)
                        lowerBound = h;
                    h += 1;
                }
            }

        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        //normalize collisionsTable
        normalizeCollisionsTable(collisionsMatrix, lowerBound);

        hopTableArray = hopTable(collisionsMatrix, lowerBound);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsMatrix(collisionsMatrix);
        graphMeasure.setHopTable(hopTableArray);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setTime(totalTime);
        graphMeasure.setLowerBoundDiameter(lowerBound);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTableArray));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTableArray, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTableArray));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTableArray, mThreshold));

        return graphMeasure;
    }

}
