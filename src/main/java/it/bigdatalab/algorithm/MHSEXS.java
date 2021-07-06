package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of MHSEBSideS (MinHash Signature Estimation B-Side slow version) algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class MHSEXS extends MinHash {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.BMHSE");
    private final int[] signatures;
    private final int[] oldSignatures;

    /**
     * Creates a new BMHSE instance with default values
     */
    public MHSEXS(final CompressedGraph g, int numSeeds, double threshold, int[] nodes) throws SeedsException {
        super(g, numSeeds, threshold, nodes);

        signatures = new int[lengthBitsArray(mNumSeeds * mGraph.numNodes())];
        oldSignatures = new int[lengthBitsArray(mNumSeeds * mGraph.numNodes())];
    }

    /**
     * Creates a new BMHSE instance with default values
     */
    public MHSEXS(final CompressedGraph g, int numSeeds, double threshold) throws SeedsException {
        super(g, numSeeds, threshold);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());

        signatures = new int[lengthBitsArray(mNumSeeds * mGraph.numNodes())];
        oldSignatures = new int[lengthBitsArray(mNumSeeds * mGraph.numNodes())];
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

        boolean signatureIsChanged = true;
        int h = 0;

        while (signatureIsChanged) {
            hopStartTime = System.currentTimeMillis();

            if (h == 0) {
                for (int s = 0; s < mNumSeeds; s++) {
                    int node = mMinHashNodeIDs[s] + (mGraph.numNodes() * s);
                    int position = node >>> Constants.MASK;
                    int remainder = (node << Constants.REMAINDER) >>> Constants.REMAINDER;

                    signatures[position] |= (Constants.BIT) << remainder;
                }

                signatureIsChanged = true;
            } else {
                signatureIsChanged = false;

                System.arraycopy(signatures, 0, oldSignatures, 0, signatures.length);

                for (int n = 0; n < mGraph.numNodes(); n++) {
                    // update node signature

                    for (int s = 0; s < mNumSeeds; s++) {
                        int node = n + (mGraph.numNodes() * s);
                        int position = node >>> Constants.MASK;
                        int remainder = (node << Constants.REMAINDER) >>> Constants.REMAINDER;

                        int nodeMask = (Constants.BIT << remainder);
                        int value = signatures[position];
                        int bitNeigh;

                        if (((nodeMask & value) >>> remainder) == 0) {
                            final int [] successors = mGraph.get_neighbours(n,true);
                            int d = successors.length;

                            for (int l = 0; l < d; l++) {
                                final int neigh = successors[l] + (mGraph.numNodes() * s);
                                int neighPosition = neigh >>> Constants.MASK;
                                int remainderNeigh = (neigh << Constants.REMAINDER) >>> Constants.REMAINDER;
                                bitNeigh = (((1 << remainderNeigh) & oldSignatures[neighPosition]) >>> remainderNeigh) << remainder;
                                value = bitNeigh | nodeMask & oldSignatures[position];
                                if ((value >>> remainder) == 1) {
                                    signatureIsChanged = true;
                                    break;
                                }
                            }
                        } // else is already 1
                        signatures[position] = signatures[position] | value;
                    }

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
            if (signatureIsChanged) {
                // count the collision between the node signature and the graph signature
                long collisions = 0;
                for (int aSig : signatures) {
                    collisions += Integer.bitCount(aSig);
                }

                long[] copy = new long[h + 1];
                System.arraycopy(collisionsVector, 0, copy, 0, collisionsVector.length);
                collisionsVector = copy;

                collisionsVector[h] = collisions;
                h += 1;
            }

            logTime = System.currentTimeMillis();
            logger.info("hop # {} completed. Time elapsed to complete computation {} s.",
                    h,
                    (logTime - hopStartTime) / (double) 1000);
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        double[] hopTable = hopTable(collisionsVector);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setHopTable(hopTable);
        graphMeasure.setLowerBoundDiameter(hopTable.length - 1);
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
