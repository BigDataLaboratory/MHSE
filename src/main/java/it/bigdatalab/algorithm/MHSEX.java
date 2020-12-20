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
 * Implementation of MHSE X (MinHash Signature Estimation X version) algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class MHSEX extends MinHash {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.BMHSE");

    /**
     * Creates a new MHSE X instance with default values
     */
    public MHSEX(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes) throws SeedsException {
        super(g, numSeeds, threshold, nodes);
    }

    /**
     * Creates a new MHSE X instance with default values
     */
    public MHSEX(final ImmutableGraph g, int numSeeds, double threshold) throws SeedsException {
        super(g, numSeeds, threshold);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
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

        int[] trackerMutable = new int[lengthBitsArray(mGraph.numNodes())];
        int[] trackerImmutable = new int[lengthBitsArray(mGraph.numNodes())];

        int[][] signMutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];
        int[][] signImmutable = new int[mGraph.numNodes()][lengthBitsArray(mNumSeeds)];

        boolean signatureIsChanged = true;
        int h = 0;

        int nPosition, nRemainder, neighPosition, neighRemainder, neighMask;
        while (signatureIsChanged) {
            hopStartTime = System.currentTimeMillis();

            if (h == 0) {
                for (int s = 0; s < mNumSeeds; s++) {
                    position[s] = s >>> Constants.MASK;
                    remainder[s] = (s << Constants.REMAINDER) >>> Constants.REMAINDER;
                    signMutable[mMinHashNodeIDs[s]][position[s]] |= (Constants.BIT) << remainder[s];
                    trackerMutable[mMinHashNodeIDs[s] >>> Constants.MASK] |= (Constants.BIT) << ((mMinHashNodeIDs[s] << Constants.REMAINDER) >>> Constants.REMAINDER);
                }

                signatureIsChanged = true;
            } else {
                signatureIsChanged = false;

                // update node signature
                for (int n = 0; n < mGraph.numNodes(); n++) {
                    final int d = mGraph.outdegree(n);
                    final int[] successors = mGraph.successorArray(n);

                    nPosition = n >>> Constants.MASK;
                    nRemainder = (n << Constants.REMAINDER) >>> Constants.REMAINDER;

                    // for each neigh of the node n
                    for (int l = d; l-- != 0; ) {
                        // check if the neigh has been modified
                        // in the previous hop. If true, it can modify
                        // the node n
                        neighPosition = successors[l] >>> Constants.MASK;
                        neighRemainder = (successors[l] << Constants.REMAINDER) >>> Constants.REMAINDER;
                        neighMask = (Constants.BIT << neighRemainder);

                        if (((neighMask & trackerImmutable[neighPosition]) >>> neighRemainder) == 1) {
                            // for each element of the signature of the node n
                            int sMask;
                            for (int s = 0; s < mNumSeeds; s++) {
                                sMask = (Constants.BIT << remainder[s]);

                                // check if the s-th element of the node n signature
                                // it's 0, else jump to the next s-th element of the signature
                                if (((sMask & signMutable[n][position[s]]) >>> remainder[s]) == 0) {
                                    int bitNeigh;
                                    int value;
                                    // change the s-th element of the node n signature
                                    // only if the s-th element of the neigh signature is 1
                                    if (((sMask & signImmutable[successors[l]][position[s]]) >>> remainder[s]) == 1) {
                                        bitNeigh = (((1 << remainder[s]) & signImmutable[successors[l]][position[s]]) >>> remainder[s]) << remainder[s];
                                        value = bitNeigh | sMask & signImmutable[successors[l]][position[s]];
                                        signatureIsChanged = true; // track the signature changes, to run the next hop
                                        trackerMutable[nPosition] |= (Constants.BIT) << nRemainder;
                                        signMutable[n][position[s]] = signMutable[n][position[s]] | value;
                                    }
                                } // else is already 1
                            }
                        }
                    }
                    logTime = System.currentTimeMillis();
                    if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                        logger.info("# nodes analyzed {} / {} for hop {} [elapsed {}, node/s {}]",
                                n, mGraph.numNodes(),
                                h,
                                TimeUnit.MILLISECONDS.toSeconds(logTime - hopStartTime),
                                TimeUnit.MILLISECONDS.toSeconds((logTime - hopStartTime) / n));
                        lastLogTime = logTime;
                    }
                }

            }
            if (signatureIsChanged) {
                System.arraycopy(trackerMutable, 0, trackerImmutable, 0, trackerMutable.length);
                trackerMutable = new int[lengthBitsArray(mGraph.numNodes())];

                // count the collisions of the signatures
                long collisions = 0;
                for (int r = 0; r < mGraph.numNodes(); r++) {
                    System.arraycopy(signMutable[r], 0, signImmutable[r], 0, signMutable[r].length);
                    for (int c = 0; c < signMutable[r].length; c++) {
                        collisions += Integer.bitCount(signMutable[r][c]);
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
