package it.bigdatalab.algorithm;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
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
    private String direction;
    private long mMemoryUsed;

    protected Int2DoubleLinkedOpenHashMap hopTable = new Int2DoubleLinkedOpenHashMap();

    public MinHash() throws IOException, DirectionNotSetException {
        initialize();
    }

    private void initialize() throws IOException, DirectionNotSetException {

        mMemoryUsed = 0;

        isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isolatedVertices"));
        logger.info("Keep the isolated vertices? {}", isolatedVertices);

        isSeedsRandom = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isSeedsRandom"));
        logger.info("Has seeds list to be random? {}", isSeedsRandom);

        inputFilePath = PropertiesManager.getProperty("minhash.inputFilePath");
        logger.info("Loading graph at filepath {}", inputFilePath);
        mGraph = ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");


        if (!isolatedVertices) {
            Preprocessing preprocessing = new Preprocessing();
            mGraph = preprocessing.removeIsolatedNodes(mGraph);
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
    protected IntArrayList createSeeds() {
        IntArrayList seeds = new IntArrayList();
        Random random = new Random();

        for(int i = 0; i < numSeeds; i++) {
            int randomNum = random.nextInt();
            while (seeds.contains(randomNum)) {
                randomNum = random.nextInt();
            }
            seeds.add(randomNum);
        }

        return seeds;
    }

    public IntArrayList getSeeds() {
        return mSeeds;
    }

    public void setSeeds(IntArrayList seeds) throws SeedsException {
        if (numSeeds != seeds.size()) {
            String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + numSeeds + " and length of seeds list is " + seeds.size();
            throw new SeedsException(message);
        }

        this.mSeeds = seeds;
    }

    public int[] getNodes() {
        return minHashNodeIDs;
    }

    public void setNodes(int[] nodes) throws SeedsException {
        if (numSeeds != nodes.length) {
            String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + numSeeds + " and length of nodes list is " + minHashNodeIDs.length;
            throw new SeedsException(message);
        }

        this.minHashNodeIDs = nodes;
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
