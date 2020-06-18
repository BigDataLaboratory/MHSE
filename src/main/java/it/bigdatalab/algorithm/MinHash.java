package it.bigdatalab.algorithm;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public abstract class MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHash");

    protected IntArrayList mSeeds;
    protected ImmutableGraph mGraph;
    protected int numSeeds;
    private String inputFilePath;
    private boolean isSeedsRandom;
    private boolean runTests;
    private String direction;

    protected Int2DoubleSortedMap hopTable = new Int2DoubleLinkedOpenHashMap();

    public MinHash() throws IOException, DirectionNotSetException, SeedsException {
        initialize();
    }

    private void initialize() throws IOException, DirectionNotSetException, SeedsException {
        runTests = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.runTests"));

        isSeedsRandom = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isSeedsRandom"));
        logger.info("Has seeds list to be random? {}", isSeedsRandom);

        inputFilePath = PropertiesManager.getProperty("minhash.inputFilePath");
        logger.info("Loading graph at filepath {}", inputFilePath);
        mGraph = ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

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
            //TODO Create DirectionNotSetException
            throw new DirectionNotSetException("Direction property (\"minhash.direction\") not correctly set in properties file");
        }

        if(isSeedsRandom) {
            createSeeds();
        } else {
            String propertyName = "minhash.seeds";
            String seedsString = PropertiesManager.getProperty(propertyName);
            int[] seeds = Arrays.stream(seedsString.split(",")).mapToInt(Integer::parseInt).toArray();
            numSeeds = Integer.parseInt(PropertiesManager.getProperty("minhash.numSeeds"));

            if (numSeeds != seeds.length) {
                String message = "Specified different number of seeds in properties.  \"minhash.numSeeds\" is " + numSeeds + " and \"" + propertyName + "\" length is " + seeds.length;
                throw new SeedsException(message);
            }
            mSeeds = new IntArrayList();
            for (int i = 0; i < seeds.length; i++) {
                mSeeds.add(seeds[i]);
            }

        }

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
     *  on the seeds list
     */
    private void createSeeds() {
        mSeeds = new IntArrayList();
        Random random = new Random();

        for(int i = 0; i < numSeeds; i++) {
            mSeeds.add(random.nextInt());
        }
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @param totalCollisions
     * @return
     */
    public Int2DoubleSortedMap hopTable(Int2LongOpenHashMap totalCollisions) {
        Int2DoubleSortedMap hopTable = new Int2DoubleLinkedOpenHashMap();
        totalCollisions.forEach((key, value) -> {
            if(key != 0) {
                Double r = ((double) (value * mGraph.numNodes()) / this.numSeeds);
                hopTable.put(key, r);
            }
        });
        hopTable.put(0, mGraph.numNodes());

        return hopTable;
    }

    public abstract GraphMeasure runAlgorithm();

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
