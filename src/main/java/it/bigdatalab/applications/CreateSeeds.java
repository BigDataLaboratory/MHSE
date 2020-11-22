package it.bigdatalab.applications;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Preprocessing;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Generate mNumTest random seed and create an (optional) mNumTest list of associated nodes
 * if mInputFilePath of an input graph is specified
 */
public class CreateSeeds {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.application.CreateSeeds");

    private int mNumSeeds;
    private int mNumTest;
    private String mInputFilePath;
    private String mOutFolderPath;
    private boolean mIsolatedVertices;

    public CreateSeeds() {
        getProperties();
    }

    public static void main(String args[]) throws IOException {
        CreateSeeds createSeeds = new CreateSeeds();
        ImmutableGraph g = null;

        boolean exist = new File(createSeeds.getInputFile() + Constants.GRAPH_EXTENSION).isFile() && !createSeeds.getInputFile().isEmpty();
        if (exist) {
            g = createSeeds.loadGraph();
            if (!createSeeds.getIsolatedVertices())
                g = Preprocessing.removeIsolatedNodes(g);
        }

        // list of seeds lists
        ArrayList<IntArrayList> seedsList = new ArrayList<>();
        // list of nodes arrays
        ArrayList<int[]> nodesList = new ArrayList<>();

        for (int i = 0; i < createSeeds.getNumTest(); i++) {
            IntArrayList seeds = CreateSeeds.genSeeds(createSeeds.getNumSeeds());
            if (exist) {
                int[] nodes = createSeeds.seedToNode(g.copy(), seeds);
                nodesList.add(nodes);
            }
            seedsList.add(seeds);
            logger.info("Created list #{}/{}", i + 1, createSeeds.getNumTest());
        }

        createSeeds.seedsToJson(seedsList);
        if (exist) createSeeds.nodesToJson(nodesList);
    }

    /**
     * Read properties for CreateSeeds application from property file
     */
    private void getProperties() {
        mNumSeeds = Integer.parseInt(PropertiesManager.getProperty("seed.numSeeds", Constants.NUM_SEEDS_DEFAULT));
        mInputFilePath = PropertiesManager.getProperty("seed.inputFilePath");
        mOutFolderPath = PropertiesManager.getPropertyIfNotEmpty("seed.outFolderPath");
        mIsolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("seed.isolatedVertices"));
        mNumTest = Integer.parseInt(PropertiesManager.getProperty("seed.numTest", Constants.NUM_TEST_DEFAULT));
    }

    /***
     *  Generate a random integer and append it
     *  to the seeds list
     */
    public static IntArrayList genSeeds(int numSeeds) {
        IntArrayList seeds = new IntArrayList();
        Random random = new Random();

        for (int i = 0; i < numSeeds; i++) {
            int randomNum = random.nextInt();
            while (seeds.contains(randomNum)) {
                randomNum = random.nextInt();
            }
            seeds.add(randomNum);
        }
        return seeds;
    }

    /***
     * Long signed hashing of an integer node with the specified seed.
     * @param node id of the node
     * @param seed integer to be used as seed for the hash function
     * @return a long that is the hash for the node
     */
    public static long hashFunction(int node, int seed) {
        HashFunction hf = Hashing.murmur3_128(seed);
        return hf.hashLong((long) node).asLong();
    }

    /**
     * Load an input graph in WebGraph format
     *
     * @return an ImmutableGraph instance
     */
    private ImmutableGraph loadGraph() {
        logger.info("Loading graph at filepath {}", mInputFilePath);
        ImmutableGraph graph = null;
        try {
            graph = ImmutableGraph.load(mInputFilePath);
            graph = Transform.transpose(Transform.transpose(graph));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Loading graph completed");
        return graph;
    }

    /**
     * Get associated nodes for a list of seeds
     *
     * @param g     input graph
     * @param seeds seeds' list
     * @return associated nodes for a list of seeds
     */
    private int[] seedToNode(ImmutableGraph g, IntArrayList seeds) {
        return minHashNodes(g, seeds);
    }

    /**
     * Creates hash values for all graph nodes
     * and store minhash nodes according to seedIndex
     *
     * @param g an input graph
     */
    private int[] minHashNodes(ImmutableGraph g, IntArrayList seeds) {
        int[] minHashNodeIDs = new int[mNumSeeds];

        for (int s = 0; s < seeds.size(); s++) {
            NodeIterator nodeIter = g.nodeIterator();
            long maxHashValue = Long.MAX_VALUE;
            while (nodeIter.hasNext()) {
                int node = nodeIter.nextInt();
                long hashValue = hashFunction(node, seeds.getInt(s));
                if (hashValue < maxHashValue) {
                    maxHashValue = hashValue;
                    minHashNodeIDs[s] = node;
                }
            }
        }
        return minHashNodeIDs;
    }

    /**
     * Write the seeds' list in a JSON file
     *
     * @param seeds seeds list
     */
    private void seedsToJson(List<IntArrayList> seeds) throws IOException {
        Gson gson = new GsonBuilder().create();
        String graphFileName = Paths.get(mInputFilePath).getFileName().toString();

        String path = mOutFolderPath +
                File.separator +
                "seeds_" +
                graphFileName +
                "_" +
                (mIsolatedVertices ? "with_iso" : "without_iso") +
                ".json";

        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(seeds, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Seeds list wrote in {}", mOutFolderPath);
    }

    /**
     * Write the nodes' list in a JSON file
     *
     * @param nodes nodes' list
     */
    private void nodesToJson(List<int[]> nodes) throws IOException {
        Gson gson = new GsonBuilder().create();
        String graphFileName = Paths.get(mInputFilePath).getFileName().toString();
        String path = mOutFolderPath +
                File.separator +
                "nodes_" +
                graphFileName +
                "_" +
                (mIsolatedVertices ? "with_iso" : "without_iso") +
                ".json";

        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(nodes, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Nodes list wrote in {}", mOutFolderPath);
    }


    /*******************************************************************************
     *                                  GETTER METHODS
     * ****************************************************************************/

    /**
     * @return number of list of seeds to generate
     */
    public int getNumTest() {
        return mNumTest;
    }

    /**
     * @param numTest number of test
     */
    public void setNumTest(int numTest) {
        this.mNumTest = numTest;
    }

    /**
     * @return input file path of a graph
     */
    public String getInputFile() {
        return mInputFilePath;
    }

    /**
     * @return true if you keep the isolated vertices
     */
    public boolean getIsolatedVertices() {
        return mIsolatedVertices;
    }


    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/

    /**
     * @return max number of seeds
     */
    public int getNumSeeds() {
        return mNumSeeds;
    }

    /**
     * @param numSeeds max number of seeds
     */
    public void setNumSeeds(int numSeeds) {
        this.mNumSeeds = numSeeds;
    }

    /**
     * @param isolatedVertices true if you keep the isolated vertices
     */
    public void setIsolatedVertices(boolean isolatedVertices) {
        this.mIsolatedVertices = isolatedVertices;
    }


}
