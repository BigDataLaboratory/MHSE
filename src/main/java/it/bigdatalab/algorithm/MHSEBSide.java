package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of MHSEBSide (MinHash Signature Estimation B-Side) algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class MHSEBSide extends MinHash {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.BMHSE");
    private final int[][] signatures;
    private final int[][] oldSignatures;

    /**
     * Creates a new MHSE B-Side instance with default values
     */
    public MHSEBSide(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes) throws SeedsException {
        super(g, numSeeds, threshold, nodes);

        signatures = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        oldSignatures = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
    }

    /**
     * Creates a new MHSE B-Side instance with default values
     */
    public MHSEBSide(final ImmutableGraph g, int numSeeds, double threshold) throws SeedsException {
        super(g, numSeeds, threshold);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());

        signatures = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        oldSignatures = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
    }

    public int lengthBitsArray(int value) {
        return (int) Math.ceil(value / (double) Integer.SIZE);
    }

    @Override
    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;
        long lastLogTime = startTime;
        long logTime;
        long hopStartTime;

        long[] collisionsVector = new long[1];
        int[] position = new int[mNumSeeds];
        int[] remainder = new int[mNumSeeds];

        boolean signatureIsChanged = true;
        int h = 0;

        while (signatureIsChanged) {
            hopStartTime = System.currentTimeMillis();

            if (h == 0) {
                for (int s = 0; s < mNumSeeds; s++) {
                    position[s] = s >>> Constants.MASK;
                    remainder[s] = (s << Constants.REMAINDER) >>> Constants.REMAINDER;
                    signatures[mMinHashNodeIDs[s]][position[s]] |= (Constants.BIT) << remainder[s];
                }

                signatureIsChanged = true;
            } else {
                signatureIsChanged = false;


                    // update node signature

                    for (int s = 0; s < mNumSeeds; s++) {

                        int nodeMask = (Constants.BIT << remainder[s]);

                        for (int n = 0; n < mGraph.numNodes(); n++) {
                            if (((nodeMask & signatures[n][position[s]]) >>> remainder[s]) == 0) {
                                int bitNeigh;
                                int value = signatures[n][position[s]];
                                final int d = mGraph.outdegree(n);
                                final int[] successors = mGraph.successorArray(n);
                                for (int l = 0; l < d; l++) {
                                    if (((nodeMask & oldSignatures[successors[l]][position[s]]) >>> remainder[s]) == 1) {
                                        bitNeigh = (((1 << remainder[s]) & oldSignatures[successors[l]][position[s]]) >>> remainder[s]) << remainder[s];
                                        value = bitNeigh | nodeMask & oldSignatures[successors[l]][position[s]];
                                        signatureIsChanged = true;
                                        break;
                                    }
                                }
                                signatures[n][position[s]] = signatures[n][position[s]] | value;
                            } // else is already 1
                            logTime = System.currentTimeMillis();
                            if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                                logger.info("# nodes analyzed {} / {} for hop {} [elapsed {}, node/s {}]",
                                        n, mGraph.numNodes(),
                                        h,
                                        TimeUnit.MILLISECONDS.toSeconds(logTime - hopStartTime),
                                        TimeUnit.MILLISECONDS.toSeconds(n / (logTime - hopStartTime)));
                                lastLogTime = logTime;
                            }
                        }

                    }

            }
            if (signatureIsChanged) {
                // count the collision between the node signature and the graph signature
                long collisions = 0;
                for (int r = 0; r < mGraph.numNodes(); r++) {
                    System.arraycopy(signatures[r], 0, oldSignatures[r], 0, signatures[r].length);

                    for (int c = 0; c < signatures[r].length; c++) {
                        collisions += Integer.bitCount(signatures[r][c]);
                    }
                }

                long[] copy = new long[h + 1];
                System.arraycopy(collisionsVector, 0, copy, 0, collisionsVector.length);
                collisionsVector = copy;

                collisionsVector[h] = collisions;
                h += 1;
            }

            logTime = System.currentTimeMillis();
            logger.info("hop # {} completed. Time elapsed to complete computation {} s.",
                    h - 1,
                    (logTime - hopStartTime) / (double) 1000);
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        double[] hopTable = hopTable(collisionsVector);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setHopTable(hopTable);
        graphMeasure.setLowerBoundDiameter(collisionsVector.length - 1);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setSeedsList(mSeeds);
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTable));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTable, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTable));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTable, mThreshold));

        return graphMeasure;
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    public double[] hopTable(long[] collisionsVector) {
        double[] hopTable = new double[collisionsVector.length];
        for (int i = 0; i < hopTable.length; i++) {
            hopTable[i] = (double) (collisionsVector[i] * mGraph.numNodes()) / this.mNumSeeds;
        }
        return hopTable;
    }
}
