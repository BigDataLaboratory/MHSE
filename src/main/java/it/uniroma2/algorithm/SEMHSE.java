package it.uniroma2.algorithm;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;
import it.uniroma2.model.GraphMeasure;

import java.io.IOException;
import java.util.Arrays;

/**
 * Implementation of SE-MHSE (Space Efficient - MinHash Signature Estimation) algorithm
 */
public class SEMHSE extends MinHash {

    private Int2LongSortedMap mTotalCollisions;
    private int[] totalCollisionsPerHashFunction;
    private Int2LongOpenHashMap hashes;
    private Int2LongOpenHashMap oldHashes;
    private long[] graphSignature;

    /**
     * Creates a SE-MHSE instance with default values
     */
    public SEMHSE() throws DirectionNotSetException, SeedsException, IOException {
        super();
        mTotalCollisions = new Int2LongLinkedOpenHashMap();
        totalCollisionsPerHashFunction = new int[numSeeds];     //for each hash function get the number of total collisions
        graphSignature = new long[numSeeds];
        Arrays.fill(graphSignature, Long.MAX_VALUE);                            //initialize graph signature with Long.MAX_VALUE
        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
    }

    /**
     * Execution of the SE-MHSE algorithm
     * @return Metrics of the algorithm
     */

    public GraphMeasure runAlgorithm() {

        NodeIterator nodeIter;
        int lowerBoundDiameter = 0;
        int previousLowerBoundDiameter;

        for (int i = 0; i < numSeeds; i++) {
            int hop = 0;
            int collisions = 0;
            boolean signatureIsChanged = true;
            logger.info("Starting computation for hash function n.{}", i);
            hashes = new Int2LongOpenHashMap(mGraph.numNodes());

            while (signatureIsChanged) {
                //first hop - initialization
                if (hop == 0) {
                    initializeGraph(i);
                    //collisions computation
                    nodeIter = mGraph.nodeIterator();
                    while (nodeIter.hasNext()) {
                        int node = nodeIter.nextInt();
                        if (hashes.get(node) == graphSignature[i]) {
                            collisions++;
                        }
                    }
                } else {   //next hops
                    signatureIsChanged = false;
                    // copy all the actual hashes in a new structure
                    oldHashes = new Int2LongOpenHashMap(mGraph.numNodes());
                    nodeIter = mGraph.nodeIterator();
                    while (nodeIter.hasNext()) {
                        int node = nodeIter.nextInt();
                        oldHashes.put(node, hashes.get(node));
                    }

                    //collisions for this hash function, until this hop
                    collisions = 0;
                    //number of nodes updated
                    int count = 0;

                    //update of the hash values
                    nodeIter = mGraph.nodeIterator();
                    while (nodeIter.hasNext()) {
                        int node = nodeIter.nextInt();
                        if (updateNodeHashValue(node)) {
                            count++;
                            signatureIsChanged = true;
                        }
                        //check if there is a collision between graph minhash and actual node hashValue
                        if (hashes.get(node) == graphSignature[i]) {
                            collisions++;
                        }
                    }
                }
                logger.debug("Number of collisions: {}", collisions);

                if (signatureIsChanged) {
                    long previousValue = mTotalCollisions.get(hop);
                    mTotalCollisions.put(hop, previousValue + collisions);
                    logger.debug("Hop {} for seed n.{} completed", hop, i);
                    hop++;
                }
            }

            logger.info("Total number of collisions for seed n.{} : {}", i, collisions);
            totalCollisionsPerHashFunction[i] = collisions;
            //collisions computation completed for this hash function
            //update of the lower bound diameter
            if ((hop - 1) > lowerBoundDiameter) {
                previousLowerBoundDiameter = lowerBoundDiameter;
                lowerBoundDiameter = (hop - 1);
                //I have to normalize collisions of all the previous hash functions,
                //for all the missing hops between previousLowerBoundDiameter and lowerBoundDiameter
                //because I reached a new lowerBoundDiameter
                for (int j = 0; j < i; j++) { //previous hash functions
                    for (int k = lowerBoundDiameter; k > previousLowerBoundDiameter; k--) { //all hops between previousLowerBoundDiameter and lowerBoundDiameter
                        long previousValue = mTotalCollisions.get(k);
                        mTotalCollisions.put(k, previousValue + totalCollisionsPerHashFunction[j]);
                    }
                }
            } else if ((hop - 1) < lowerBoundDiameter) {
                //I have to add collisions of this hash function to all the remaining hops
                //between hop (excluded) and lowerBoundDiameter (included)
                for (int k = lowerBoundDiameter; k > (hop - 1); k--) {
                    long previousValue = mTotalCollisions.get(k);
                    mTotalCollisions.put(k, previousValue + collisions);
                }
            }
            logger.info("Computation for hash function n.{} completed", i);
        }

        logger.info("Starting computation of the hop table from collision table");
        hopTable = hopTable(mTotalCollisions);
        logger.info("Computation of the hop table completed");

        GraphMeasure graphMeasure = new GraphMeasure(hopTable);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        return graphMeasure;
    }

    /**
     * Initialization of the graph structures
     * Creates hash values for all graph nodes
     * and store minhash in graphSignature according to seedIndex
     * @param seedIndex
     */
    private void initializeGraph(int seedIndex){
        int seed = mSeeds.getInt(seedIndex);
        NodeIterator nodeIter = mGraph.nodeIterator();
        while(nodeIter.hasNext()) {
            int node = nodeIter.nextInt();
            long hashValue = hashFunction(node, seed);
            hashes.put(node,hashValue);
            if(hashValue < graphSignature[seedIndex]){
                graphSignature[seedIndex] = hashValue;
            }
        }
        logger.info("MinHash for seed {} is {}", seedIndex, graphSignature[seedIndex]);
    }

    /**
     * Calculates the new signature for a node, based on the signature of the node's neighbours
     * @param node
     * @return true if the new signature is different from the previous one
     */

    public boolean updateNodeHashValue(int node) {
        boolean hashValueIsChanged = false;
        long newHashValue = hashes.get(node);         //new signature to be updated
//        long oldHashValue = oldHashes.get(node);     //old hash value to NOT be modified

        LazyIntIterator neighIter = mGraph.successors(node);
        int d = mGraph.outdegree(node);
        long neighbourHashValue;
        int neighbour;

        while(d-- != 0) {
            neighbour = neighIter.nextInt();
            neighbourHashValue = oldHashes.get(neighbour);
            if(neighbourHashValue < newHashValue){
                newHashValue = neighbourHashValue;
                hashValueIsChanged = true;
            }
        }
        hashes.put(node,newHashValue);
        return hashValueIsChanged;
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @param totalCollisions
     * @return
     */
    public Int2DoubleSortedMap hopTable(Int2LongSortedMap totalCollisions) {
        Int2DoubleSortedMap hopTable = new Int2DoubleLinkedOpenHashMap();
        totalCollisions.forEach((key, value) -> {
                Double r = ((double) (value * mGraph.numNodes()) / this.numSeeds);
                hopTable.put(key, r);
        });

        return hopTable;
    }


}


