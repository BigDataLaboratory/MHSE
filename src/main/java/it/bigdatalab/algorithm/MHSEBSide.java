package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MHSEBSide extends MinHash {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.BMHSE");
    private final int[] graphSignature;
    private int[] signatures;
    private int[] oldSignatures;

    /**
     * Creates a new BMHSE instance with default values
     */
    public MHSEBSide(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes) throws SeedsException {
        super(g, numSeeds, threshold, nodes);

        signatures = new int[lengthBitsArray(mNumSeeds * mGraph.numNodes())];
        oldSignatures = new int[lengthBitsArray(mNumSeeds * mGraph.numNodes())];
        graphSignature = new int[lengthBitsArray(mNumSeeds)];
    }

    /**
     * Creates a new BMHSE instance with default values
     */
    public MHSEBSide(final ImmutableGraph g, int numSeeds, double threshold) throws SeedsException {
        super(g, numSeeds, threshold);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());

        signatures = new int[lengthBitsArray(mNumSeeds * mGraph.numNodes())];
        oldSignatures = new int[lengthBitsArray(mNumSeeds * mGraph.numNodes())];
        graphSignature = new int[lengthBitsArray(mNumSeeds)];
    }

    public int lengthBitsArray(int value) {
        return (int) Math.ceil(value / (double) Integer.SIZE);
    }

    @Override
    public Measure runAlgorithm() throws IOException {
        long startTime = System.currentTimeMillis();
        long totalTime;
        long lastLogTime = startTime;
        long logTime;
        long hopStartTime;

        double[] hopTable = new double[1];

        boolean signatureIsChanged = true;
        int h = 0;

        while (signatureIsChanged) {
            hopStartTime = System.currentTimeMillis();

            double overallJaccard = 0d;
            if (h == 0) {
                for (int s = 0; s < mNumSeeds; s++) {
                    int node = mMinHashNodeIDs[s] + (mGraph.numNodes() * s);
                    int position = node >>> Constants.MASK;
                    int remainder = (node << Constants.REMAINDER) >>> Constants.REMAINDER;
                    signatures[position] |= (Constants.BIT) << remainder;
                }
                overallJaccard += mNumSeeds;
                signatureIsChanged = true;
            } else {
                signatureIsChanged = false;

                System.arraycopy(signatures, 0, oldSignatures, 0, signatures.length);

                int remainderPositionNode;
                int quotientNode;
                for (int n = 0; n < mGraph.numNodes(); n++) {
                    // update node signature
                    final int d = mGraph.outdegree(n);
                    final int[] successors = mGraph.successorArray(n);

                    for (int s = 0; s < mNumSeeds; s++) {
                        int node = mMinHashNodeIDs[s] + (mGraph.numNodes() * s);
                        int position = node >>> Constants.MASK;
                        int remainder = (node << Constants.REMAINDER) >>> Constants.REMAINDER;

                        int nodeMask = (Constants.BIT << remainder);
                        int value = signatures[position];

                        if (((nodeMask & value) >>> remainder) == 0) {
                            for (int l = 0; l < d; l++) {
                                final int neigh = successors[l];
                            }
                        } // else is already 1
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
                double estimatedCouples = overallJaccard * mGraph.numNodes();

                double[] copy = new double[h + 1];
                System.arraycopy(hopTable, 0, copy, 0, hopTable.length);
                hopTable = copy;

                hopTable[h] = estimatedCouples;

                h += 1;
            }
            logTime = System.currentTimeMillis();
            logger.info("hop # {} completed. Time elapsed to complete computation {} s.",
                    h,
                    (logTime - hopStartTime) / (double) 1000);
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

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
}
