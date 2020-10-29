package it.bigdatalab.algorithm;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

public abstract class MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHash");

    protected IntArrayList mSeeds;
    protected ImmutableGraph mGraph;
    protected int numSeeds;
    protected boolean isolatedVertices;
    protected int[] minHashNodeIDs;
    protected boolean isSeedsRandom;
    private String inputFilePath;
    private boolean runTests;
    private String direction;
    private long mMemoryUsed;

    protected Int2DoubleSortedMap hopTable = new Int2DoubleLinkedOpenHashMap();

    public MinHash() throws IOException, DirectionNotSetException {
        initialize();
    }

    private void initialize() throws IOException, DirectionNotSetException {

        mMemoryUsed = 0;
        runTests = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.runTests"));

        isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isolatedVertices"));
        logger.info("Keep the isolated vertices? {}", isolatedVertices);

        isSeedsRandom = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isSeedsRandom"));
        logger.info("Has seeds list to be random? {}", isSeedsRandom);

        inputFilePath = PropertiesManager.getProperty("minhash.inputFilePath");
        logger.info("Loading graph at filepath {}", inputFilePath);
        mGraph = ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");




        if(!isolatedVertices){
            logger.info("Deleting isolated nodes...");
            NodeIterator nodeIterator = mGraph.nodeIterator();
            int[] indegree = new int[mGraph.numNodes()];
            int [] outdegree = new int[mGraph.numNodes()];
            int [] mappedGraph = new int[mGraph.numNodes()];
            int numNodes = mGraph.numNodes();
            int d;
            int s;
            Boolean isBijective = true;
            while(numNodes-- != 0) {
                int vertex = nodeIterator.nextInt();
                d = nodeIterator.outdegree();
                outdegree[vertex] = d;
                int[] neighbours = nodeIterator.successorArray();
                for (s = d; s-- != 0; ++indegree[neighbours[s]]) {}
            }
            d = 0;
            for (s = 0; s< indegree.length;s++){
                if((indegree[s] == 0) && (outdegree[s] ==0)){
                    mappedGraph[s] = -1;
                    if(isBijective) {
                        isBijective = false;
                    }
                }else{
                    mappedGraph[s] = d;
                    d+=1;
                }
            }
            numNodes = mGraph.numNodes();
            if(!isBijective) {
                mGraph = Transform.map(mGraph, mappedGraph);
                logger.info("Deleted {} nodes ",numNodes-mGraph.numNodes());
            }else {
                logger.info("The graph does not contain isolated vertices");
            }

        }

        direction = PropertiesManager.getProperty("minhash.direction");
        logger.info("Direction selected is {}", direction);

        if(direction.equals("in")){
            //Double transpose because more efficient
            logger.info("Transposing graph...");
            mGraph = Transform.transpose(Transform.transpose(mGraph));
            logger.info("Transposing graph ended");
        } else if(direction.equals("out")){
            logger.info("Transposing graph...");
            mGraph = Transform.transpose(mGraph);
            logger.info("Transposing graph ended");
        } else {
            throw new DirectionNotSetException("Direction property (\"minhash.direction\") not correctly set in properties file");
        }

        /*Dictionary<Integer,Double> personalization = new Hashtable<Integer, Double>();
        Dictionary<Integer,Double> nstart = new Hashtable<Integer, Double>();
        Dictionary<Integer,Double> dangling = new Hashtable<Integer, Double>();
        */

//        Clustering cluster = new Clustering();
//        cluster.runAlgorithm();
//        System.exit(-1);
        //cluster.pagerank(mGraph,0.85,personalization,100,1.0e-6,nstart,dangling);
        //cluster.apx_pagerank(mGraph,0.85,0,1.0e-6);
        //cluster.print_pagerank();


        numSeeds = Integer.parseInt(PropertiesManager.getProperty("minhash.numSeeds"));
        minHashNodeIDs = new int[numSeeds];


    }

    /***
     * Long signed hashing of a node with the specified seed.
     * @param node id of the node
     * @param seed integer to be used as seed for the hash function
     * @return a long that is the hash for the node
     */
    public long hashFunction(int node, int seed) {
        HashFunction hf = Hashing.murmur3_128(seed);
        return hf.hashLong((long)node).asLong();
    }

    /***
     *  Generate a random integer and append it
     *  to the seeds list
     */
    protected void createSeeds() {
        mSeeds = new IntArrayList();
        Random random = new Random();

        for(int i = 0; i < numSeeds; i++) {
            int randomNum = random.nextInt();
            while(mSeeds.contains(randomNum)){
                randomNum = random.nextInt();
            }
            mSeeds.add(randomNum);
        }
    }

    public void memoryUsed() {
        // Calculate the used memory
        System.gc();
        long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        logger.debug("Old memory value: {} Actual used memory: {}", mMemoryUsed, memory);
        if (memory > mMemoryUsed) {
            mMemoryUsed = memory;
        }
    }

    public long getMaxUsedMemory() {
        return mMemoryUsed;
    }

    public abstract GraphMeasure runAlgorithm() throws IOException;

    public static class SeedsException extends Throwable {
        public SeedsException(String message) {
            super(message);
        }
    }

    public static class DirectionNotSetException extends Throwable {
        public DirectionNotSetException(String message) {
            super(message);
        }
    }
}
