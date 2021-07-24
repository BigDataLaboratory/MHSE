package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of MHSE (MinHash Signature Estimation) algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 **/
public class MHSE extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MHSE");

    private final Int2ObjectOpenHashMap<long[]> signatures;
    private Int2ObjectOpenHashMap<long[]> oldSignatures;
    private final long[] graphSignature;

    /**
     * Creates a new MHSE instance with default values
     */
    public MHSE(final GraphManager g, int numSeeds, double threshold, IntArrayList seeds) throws SeedsException {
        super(g, numSeeds, threshold, seeds);

        signatures = new Int2ObjectOpenHashMap<>(mGraph.numNodes());       //initialize signatures map with the expected number of elements(nodes) in the map
        oldSignatures = new Int2ObjectOpenHashMap<>(mGraph.numNodes());
        graphSignature = new long[mNumSeeds];
        Arrays.fill(graphSignature, Long.MAX_VALUE);                            //initialize graph signature with Long.MAX_VALUE
    }

    /**
     * Creates a new MHSE instance with default values
     */
    public MHSE(final GraphManager g, int numSeeds, double threshold) throws SeedsException {
        super(g, numSeeds, threshold);

        this.mSeeds = CreateSeeds.genSeeds(mNumSeeds);
        signatures = new Int2ObjectOpenHashMap<>(mGraph.numNodes());       //initialize signatures map with the expected number of elements(nodes) in the map
        oldSignatures = new Int2ObjectOpenHashMap<>(mGraph.numNodes());
        graphSignature = new long[mNumSeeds];
        Arrays.fill(graphSignature, Long.MAX_VALUE);                            //initialize graph signature with Long.MAX_VALUE
    }

    /**
     * Execution of the MHSE algorithm
     * @return Metrics of the algorithm
     */
    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;
        long lastLogTime = startTime;
        long logTime;
        long hopStartTime;

        Int2DoubleLinkedOpenHashMap hopTable = new Int2DoubleLinkedOpenHashMap();
        boolean signatureIsChanged = true;
        int hop = 0;
        int [] nodes = mGraph.get_nodes();
        int i ;


        while (signatureIsChanged) {
            hopStartTime = System.currentTimeMillis();

            signatureIsChanged = false;
            double overallJaccard = 0d;
            if (hop == 0) {
                initializeGraph();

                // jaccard computation
                i = 0;
                while (i<nodes.length) {
                    int node = nodes[i];
                    overallJaccard += jaccard(signatures.get(node), graphSignature);
                    i+=1;
                }
                signatureIsChanged = true;
            } else {
                // copy all the actual signatures in a new structure
                oldSignatures = new Int2ObjectOpenHashMap<>(mGraph.numNodes());
                i = 0;
                while(i<nodes.length) {
                    int node = nodes[i];
                    long[] signature = signatures.get(node);
                    long[] oldSignature = new long[signature.length];

                    System.arraycopy(signature, 0, oldSignature, 0, signature.length);
                    oldSignatures.put(node, oldSignature);
                    i+=1;
                }
                // updating the signatures
                i = 0;
                while(i<nodes.length) {
                    int node = nodes[i];

                    if (updateNodeSignature(node)) {
                        signatureIsChanged = true;
                    }
                    //TODO can we optimized code inserting jaccard computation in updateNodeSignature?
                    //TODO we can set jaccard as member variable without redirect it for every hop, updating it only when signatureIsChanged is true
                    overallJaccard += jaccard(signatures.get(node), graphSignature);

                    logTime = System.currentTimeMillis();
                    if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                        logger.info("# nodes analyzed {} / {} for hop {} [elapsed {}, node/s {}]",
                                node, mGraph.numNodes(),
                                hop,
                                TimeUnit.MILLISECONDS.toSeconds(logTime - hopStartTime),
                                TimeUnit.MILLISECONDS.toSeconds(node / (logTime - hopStartTime)));
                        lastLogTime = logTime;
                    }
                    i+=1;
                }
            }

            if (signatureIsChanged) {
                double estimatedCouples = overallJaccard * mGraph.numNodes();
                hopTable.put(hop, estimatedCouples);
                hop++;
            }

            logTime = System.currentTimeMillis();
            logger.info("hop # {} completed. Time elapsed to complete computation {} s.",
                    hop,
                    (logTime - hopStartTime) / (double) 1000);
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        GraphMeasure graphMeasure = new GraphMeasure();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setHopTable(hopTable);
        graphMeasure.setLowerBoundDiameter(hopTable.size() - 1);
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


    private void initializeGraph(){
        // Signatures initialization
        int [] nodes = mGraph.get_nodes();
        int j;
        j= 0;
        while(j<nodes.length) {
            int node = nodes[j];
            long[] signature = new long[mNumSeeds];
            // create a new signature for each node and compute signature for the graph
            for (int i = 0; i < mNumSeeds; i++) {
                signature[i] = CreateSeeds.hashFunction(node, mSeeds.getInt(i));
                // check if this part of the signature is the minimum for the graph
                if(signature[i] < graphSignature[i]){
                    graphSignature[i] = signature[i];
                    mMinHashNodeIDs[i] = node;
                }
            }
            signatures.put(node, signature);
            j+=1;
        }
    }

    /**
     * Compute the new signature for a node, based on the signature of the node's neighbours
     * @param node
     * @return true if the new signature is different from the previous one
     */
    public boolean updateNodeSignature(int node) {
        boolean signatureIsChanged = false;
        long[] newSignature = signatures.get(node);         //new signature to be updated
        int [] neigh = mGraph.get_neighbours(node);
        int k;
        int d = neigh.length;
        long[] neighbourSignature;
        int neighbour;
        k = 0;
        while(d-- != 0) {
            neighbour =neigh[k];
            k+=1;
            neighbourSignature = oldSignatures.get(neighbour);
            for(int i=0; i<neighbourSignature.length; i++){
                if(neighbourSignature[i] < newSignature[i]){
                    newSignature[i] = neighbourSignature[i];
                    signatureIsChanged = true;
                }
            }
        }
        return signatureIsChanged;
    }


    /**
     * Compute jaccard measure between two signatures
     *
     * @param sig1 signature
     * @param sig2 signature
     * @return jaccard value
     */
    private double jaccard(long[] sig1, long[] sig2) {
        double intersection = 0d;
        double union = sig1.length;
        for (int i = 0; i < sig1.length; i++) {
            if (sig1[i] == sig2[i]) {
                intersection = intersection + 1;
            }
        }
        return intersection / union;
    }

}


