package it.bigdatalab.algorithm;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;
import it.bigdatalab.model.GraphMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Implementation of MHSE (MinHash Signature Estimation) algorithm
 */
public class MHSE extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MHSE");

    //TODO check if Int2ObjectSortedMap<long[]> is better
    private Int2ObjectOpenHashMap<long[]> signatures;
    private Int2ObjectOpenHashMap<long[]> oldSignatures;
    private long[] graphSignature;

    /**
     * Creates a new MHSE instance with default values
     */
    public MHSE() throws IOException, DirectionNotSetException, SeedsException {
        super();
        signatures = new Int2ObjectOpenHashMap<long[]>(mGraph.numNodes());       //initialize signatures map with the expected number of elements(nodes) in the map
        oldSignatures = new Int2ObjectOpenHashMap<long[]>(mGraph.numNodes());
        graphSignature = new long[numSeeds];
        Arrays.fill(graphSignature, Long.MAX_VALUE);                            //initialize graph signature with Long.MAX_VALUE
        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
    }

    /**
     * Execution of the MHSE algorithm
     * @return Metrics of the algorithm
     */
    public GraphMeasure runAlgorithm() {

        boolean signatureIsChanged = true;
        int hop = 0;
        NodeIterator nodeIter;

        while(signatureIsChanged){
            logger.info("Analyzing hop " + hop);
            signatureIsChanged = false;
            double overallJaccard = 0d;
            if(hop == 0){
                initializeGraph();
                String graphSignatureStr = "";
                for(int i=0; i<graphSignature.length;i++){
                    graphSignatureStr += (graphSignature[i] + ",");
                }
                logger.info("Graph signature is: " + graphSignatureStr);

                // jaccard computation
                nodeIter = mGraph.nodeIterator();
                while(nodeIter.hasNext()) {
                    int node = nodeIter.nextInt();
                    overallJaccard += jaccard(signatures.get(node), graphSignature);
                }
                signatureIsChanged = true;
            } else {
                // copy all the actual signatures in a new structure
                oldSignatures = new Int2ObjectOpenHashMap<long[]>(mGraph.numNodes());
                nodeIter = mGraph.nodeIterator();
                while(nodeIter.hasNext()) {
                    int node = nodeIter.nextInt();
                    long[] signature = signatures.get(node);
                    long[] oldSignature = new long[signature.length];
                    //TODO Metodo più efficiente per deep copy?
                    System.arraycopy( signature, 0, oldSignature, 0, signature.length );
                    oldSignatures.put(node, oldSignature);
                }
                // updating the signatures
                nodeIter = mGraph.nodeIterator();
                int count = 0;
                while(nodeIter.hasNext()) {
                    int node = nodeIter.nextInt();
                    if (updateNodeSignature(node)){
                        count++;
                        signatureIsChanged = true;
                    }
                    //TODO can we optimized code inserting jaccard computation in updateNodeSignature?
                    //TODO we can set jaccard as member variable without redirect it for every hop, updating it only when signatureIsChanged is true
                    overallJaccard += jaccard(signatures.get(node), graphSignature);
                }
                logger.info("Node Signatures modified: " + count);
            }
            logger.info("Overall jaccard: " + overallJaccard);

            if(signatureIsChanged) {
                double estimatedCouples = overallJaccard * mGraph.numNodes();
                hopTable.put(hop, estimatedCouples);
                hop++;
            }
            logger.info("Hop " + hop + " completed");
        }

        GraphMeasure graphMeasure = new GraphMeasure(hopTable);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());

        String minHashNodeIDsString = "";
        String separator = ",";
        for(int i=0;i<numSeeds;i++){
            minHashNodeIDsString += (minHashNodeIDs[i] + separator);
        }
        graphMeasure.setMinHashNodeIDs(minHashNodeIDsString);


        return graphMeasure;
    }


    private void initializeGraph(){
        // Signatures initialization
        NodeIterator nodeIter = mGraph.nodeIterator();
        while(nodeIter.hasNext()) {
            int node = nodeIter.nextInt();
            long[] signature = new long[numSeeds];
            // create a new signature for each node and compute signature for the graph
            for(int i=0; i<numSeeds;i++){
                signature[i] = hashFunction(node, mSeeds.getInt(i));
                // check if this part of the signature is the minimum for the graph
                if(signature[i] < graphSignature[i]){
                    graphSignature[i] = signature[i];
                    minHashNodeIDs[i] = node;
                }
            }
            signatures.put(node, signature);
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
        long[] nodeSignature = oldSignatures.get(node);     //old signature to NOT be modified

        LazyIntIterator neighIter = mGraph.successors(node);
        int d = mGraph.outdegree(node);
        long[] neighbourSignature;
        int neighbour;

        while(d-- != 0) {
            neighbour = neighIter.nextInt();
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
     * @param sig1 signature
     * @param sig2 signature
     * @return
     */
    private double jaccard(long[] sig1, long[] sig2){
        double intersection = 0d;
        double union = (double)sig1.length;
        for(int i=0; i<sig1.length; i++){
            if(sig1[i] == sig2[i]){
                intersection = intersection + 1;
            }
        }
        return intersection/union;
    }

}


