package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * Implementation of SE-MHSE (Space Efficient - MinHash Signature Estimation) algorithm
 */
public class SEMHSE extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.SEMHSE");

    private Int2ObjectOpenHashMap<int[]> collisionsTable;
    private int[] lastHops;
    private Int2LongOpenHashMap hashes;
    private Int2LongOpenHashMap oldHashes;
    private long[] graphSignature;

    /**
     * Creates a SE-MHSE instance with default values
     */
    public SEMHSE() throws DirectionNotSetException, IOException, SeedsException {
        super();

        if(mIsSeedsRandom) {
            setSeeds(createSeeds());
        }

        collisionsTable = new Int2ObjectOpenHashMap<int[]>();       //for each hop a list of collisions for each hash function
        lastHops = new int[mNumSeeds];                           //for each hash function, the last hop executed
        graphSignature = new long[mNumSeeds];
        Arrays.fill(graphSignature, Long.MAX_VALUE);                            //initialize graph signature with Long.MAX_VALUE
        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
    }

    /**
     * Execution of the SE-MHSE algorithm
     * @return Computed metrics of the algorithm
     */
    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;

        NodeIterator nodeIter;

        for (int i = 0; i < mNumSeeds; i++) {
            int hop = 0;
            int collisions = 0;
            boolean signatureIsChanged = true;
            logger.info("Starting computation for hash function n.{}", i);
            hashes = new Int2LongOpenHashMap(mGraph.numNodes());

            while (signatureIsChanged) {
                int[] hopCollisions;
                if(collisionsTable.containsKey(hop)){
                    hopCollisions = collisionsTable.get(hop);
                } else {
                    hopCollisions = new int[mNumSeeds];
                }

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
                            signatureIsChanged = true;
                        }
                        //check if there is a collision between graph minhash and actual node hashValue
                        if (hashes.get(node) == graphSignature[i]) {
                            collisions++;
                        }
                    }
                }

                if (signatureIsChanged) {
                    hopCollisions[i] = collisions;
                    collisionsTable.put(hop, hopCollisions);
                    logger.debug("Number of collisions: {}", collisions);
                    lastHops[i] = hop;
                    logger.debug("Hop {} for seed n.{} completed", hop, i);
                    hop++;
                }
                //memoryUsed();
            }
            logger.info("Total number of collisions for seed n.{} : {}", i, collisions);
            logger.info("Computation for hash function n.{} completed", i);
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        //normalize collisionsTable
        logger.info("Normalizing Collisions table...");
        normalizeCollisionsTable();
        logger.info("Collisions table normalized!");

        logger.info("Starting computation of the hop table from collision table");
        hopTable = hopTable();
        logger.info("Computation of the hop table completed");


        GraphMeasure graphMeasure = new GraphMeasure(hopTable);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setCollisionsTable(collisionsTable);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setSeedsList(getSeeds());
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(getNodes());
        graphMeasure.setMaxMemoryUsed(getMaxUsedMemory());
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
                mMinHashNodeIDs[seedIndex] = node;
            }
        }
        logger.info("MinHash for seed {} is {}, belonging to node ID {}", seedIndex, graphSignature[seedIndex], mMinHashNodeIDs[seedIndex]);
    }

    /**
     * Calculates the new signature for a node, based on the signature of the node's neighbours
     * @param node
     * @return true if the new signature is different from the previous one
     */
    public boolean updateNodeHashValue(int node) {
        boolean hashValueIsChanged = false;
        long newHashValue = hashes.get(node);         //new signature to be updated

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
     * @return hop table
     */

    private Int2DoubleLinkedOpenHashMap hopTable() {
        Int2DoubleLinkedOpenHashMap hopTable = new Int2DoubleLinkedOpenHashMap();
        int lastHop = collisionsTable.size() - 1;
        long sumCollisions = 0;

        for(int hop = 0; hop <= lastHop; hop++){
            int[] collisions = collisionsTable.get(hop);
            sumCollisions = Arrays.stream(collisions).sum();
            double couples = (double) (sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hopTable.put(hop, couples);
            logger.info("hop " + hop + " total collisions " + Arrays.stream(collisions).sum() + " couples: " + couples);
        }
        return hopTable;
    }


    /***
     * TODO Optimizable?
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    private void normalizeCollisionsTable() {
        int lowerBoundDiameter = collisionsTable.size() - 1;
        logger.debug("Diameter: " + lowerBoundDiameter);

        //Start with hop 1
        //There is no check for hop 0 because at hop 0 there is always (at least) 1 collision, never 0.
        for(int i=1; i<=lowerBoundDiameter; i++){
            int[] previousHopCollisions = collisionsTable.get(i-1);
            int[] hopCollisions = collisionsTable.get(i);
            //TODO first if is better for performance?
            if(Arrays.stream(hopCollisions).anyMatch(coll -> coll == 0)){
                for(int j=0;j<hopCollisions.length;j++){
                    if(hopCollisions[j] == 0){
                        hopCollisions[j] = previousHopCollisions[j];
                    }
                }
            }
            collisionsTable.put(i, hopCollisions);
        }
    }

}


