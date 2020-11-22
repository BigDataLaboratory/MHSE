package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class StandaloneBMinHashOptimized extends BMinHashOpt {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.StandaloneBMinHashOptimized");

    /**
     * Creates a new BooleanMinHasOptimized instance with default values
     */
    public StandaloneBMinHashOptimized(final ImmutableGraph g, boolean isSeedsRandom, int numSeeds, double threshold) {
        super(g, numSeeds, threshold);

        if (isSeedsRandom) {
            for (int i = 0; i < mNumSeeds; i++) {
                mMinHashNodeIDs[i] = ThreadLocalRandom.current().nextInt(0, mGraph.numNodes());
            }
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

        // seed as rows, hop as columns - cell values are collissions for each hash function at hop
        int[][] collisionsMatrix = new int[mNumSeeds][Constants.N];
        //for each hash function, the last hop executed
        int[] lastHops = new int[mNumSeeds];
        double[] hopTableArray;

        int lowerBound = 0;

        for (int i = 0; i < this.mNumSeeds; i++) {

            logger.info("Starting computation on seed {}", i);

            int collisions = 0;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(mGraph.numNodes())];
            int[] immutable = new int[lengthBitsArray(mGraph.numNodes())];

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
                    assert collisionsMatrix[i] != null;
                    int[][] copy = new int[mNumSeeds][collisionsMatrix[i].length + Constants.N];
                    for (int height = 0; height < mNumSeeds; height++) {
                        copy[height] = new int[collisionsMatrix[height].length + Constants.N];
                        System.arraycopy(collisionsMatrix[height], 0, copy[height], 0, collisionsMatrix[height].length);
                        collisionsMatrix[height] = copy[height];
                    }
                }

                //first hop - initialization
                if (h == 0) {

                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
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

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        //normalize collisionsTable
        normalizeCollisionsTable(collisionsMatrix, lowerBound, lastHops);

        logger.info("Starting computation of the hop table from collision table");
        hopTableArray = hopTable(collisionsMatrix, lowerBound);
        logger.info("Computation of the hop table completed");

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt(hopTableArray, lowerBound, mThreshold);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsTable(collisionsMatrix);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setTime(totalTime);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTableArray));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTableArray, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTableArray));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTableArray, mThreshold));

        return graphMeasure;
    }

    /***
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    @Override
    public void normalizeCollisionsTable(int[][] collisionsMatrix, int lowerBound, int[] last) {

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
