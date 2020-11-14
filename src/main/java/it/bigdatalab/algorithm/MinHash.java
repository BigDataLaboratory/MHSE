package it.bigdatalab.algorithm;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

public abstract class MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHash");

    protected int mNumSeeds;
    protected boolean mIsSeedsRandom;
    protected IntArrayList mSeeds;
    protected ImmutableGraph mGraph;
    protected int[] mMinHashNodeIDs;
    private long mMemoryUsed;

    protected Int2DoubleLinkedOpenHashMap hopTable = new Int2DoubleLinkedOpenHashMap();

    public MinHash() throws IOException, DirectionNotSetException {
        initialize();
    }

    private void initialize() throws IOException, DirectionNotSetException {

        mMemoryUsed = 0;

        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isolatedVertices"));
        logger.info("Keep the isolated vertices? {}", isolatedVertices);

        mIsSeedsRandom = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isSeedsRandom"));
        logger.info("Has seeds list to be random? {}", mIsSeedsRandom);

        String inputFilePath = PropertiesManager.getProperty("minhash.inputFilePath");
        logger.info("Loading graph at filepath {}", inputFilePath);
        mGraph = ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");


        if (!isolatedVertices) {
            Preprocessing preprocessing = new Preprocessing();
            mGraph = preprocessing.removeIsolatedNodes(mGraph);
        }

        String direction = PropertiesManager.getProperty("minhash.direction");
        logger.info("Direction selected is {}", direction);

        switch (direction) {
            case "in":
                //Double transpose because more efficient
                logger.info("Transposing graph...");
                mGraph = Transform.transpose(Transform.transpose(mGraph));
                logger.info("Transposing graph ended");
                break;
            case "out":
                logger.info("Transposing graph...");
                mGraph = Transform.transpose(mGraph);
                logger.info("Transposing graph ended");
                break;
            default:
                throw new DirectionNotSetException("Direction property (\"minhash.direction\") not correctly set in properties file");
        }

        mNumSeeds = Integer.parseInt(PropertiesManager.getProperty("minhash.numSeeds"));
        mMinHashNodeIDs = new int[mNumSeeds];
    }

    /***
     * Long signed hashing of a node with the specified seed.
     * @param node id of the node
     * @param seed integer to be used as seed for the hash function
     * @return a long that is the hash for the node
     */
    protected long hashFunction(int node, int seed) {
        HashFunction hf = Hashing.murmur3_128(seed);
        return hf.hashLong((long) node).asLong();
    }

    /***
     *  Generate a random integer and append it
     *  to the seeds list
     */
    public IntArrayList createSeeds() {
        IntArrayList seeds = new IntArrayList();
        Random random = new Random();

        for (int i = 0; i < mNumSeeds; i++) {
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
        if (mNumSeeds != seeds.size()) {
            String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + numSeeds + " and length of seeds list is " + seeds.size();
            throw new SeedsException(message);
        }

        this.mSeeds = seeds;
    }

    public int[] getNodes() {
        return mMinHashNodeIDs;
    }

    public void setNodes(int[] nodes) throws SeedsException {
        if (mNumSeeds != nodes.length) {
            String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + numSeeds + " and length of nodes list is " + minHashNodeIDs.length;
            throw new SeedsException(message);
        }

        this.mMinHashNodeIDs = nodes;
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

    public abstract Measure runAlgorithm() throws IOException;

    public static class SeedsException extends Throwable {
         SeedsException(String message) {
            super(message);
        }
    }

    public static class DirectionNotSetException extends Throwable {
        DirectionNotSetException(String message) {
            super(message);
        }
    }
}
