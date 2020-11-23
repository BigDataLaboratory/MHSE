package it.bigdatalab.applications;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.bigdatalab.model.Parameter;
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
import java.io.FileNotFoundException;
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

    private final Parameter mParam;

    public CreateSeeds() {
        int numSeeds = Integer.parseInt(PropertiesManager.getProperty("seed.numSeeds", Constants.NUM_SEEDS_DEFAULT));
        String inputFilePath = PropertiesManager.getProperty("seed.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("seed.outFolderPath");
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("seed.isolatedVertices"));
        int numTest = Integer.parseInt(PropertiesManager.getProperty("seed.numTest", Constants.NUM_TEST_DEFAULT));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("seed.inMemory", Constants.FALSE));

        mParam = new Parameter.Builder()
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTest)
                .setNumSeeds(numSeeds)
                .setInMemory(inMemory)
                .setIsolatedVertices(isolatedVertices).build();

        logger.info("\n\n********************** Parameters **********************\n\n" +
                        "# list of seeds/nodes to generate: {}\n" +
                        "on graph read from: {}\n" +
                        "loading graph in memory? {}\n" +
                        "keep isolated nodes? {}\n" +
                        "results will written in: {}\n" +
                        "number of seeds to generate {}\n" +
                        "\n********************************************************\n\n",
                mParam.getNumTests(),
                mParam.getInputFilePathGraph(),
                mParam.isInMemory(),
                mParam.keepIsolatedVertices(),
                mParam.getOutputFolderPath(),
                mParam.getNumSeeds());
    }

    public static void main(String[] args) throws IOException {
        CreateSeeds createSeeds = new CreateSeeds();
        createSeeds.generate();
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

    private void generate() throws IOException {
        boolean exist = new File(mParam.getInputFilePathGraph() + Constants.GRAPH_EXTENSION).isFile() && !mParam.getInputFilePathGraph().isEmpty();
        if (exist) {
            ImmutableGraph g = loadGraph(mParam.getInputFilePathGraph(), mParam.isInMemory(), mParam.keepIsolatedVertices());
            // list of seeds lists
            ArrayList<IntArrayList> seedsList = new ArrayList<>();
            // list of nodes arrays
            ArrayList<int[]> nodesList = new ArrayList<>();

            for (int i = 0; i < mParam.getNumTests(); i++) {
                IntArrayList seeds = CreateSeeds.genSeeds(mParam.getNumSeeds());
                int[] nodes = seedToNode(g.copy(), seeds, mParam.getNumSeeds());
                nodesList.add(nodes);
                seedsList.add(seeds);
                logger.info("Created list #{}/{}", i + 1, mParam.getNumTests());
            }

            seedsToJson(seedsList, mParam.getInputFilePathGraph(), mParam.getOutputFolderPath(), mParam.keepIsolatedVertices());
            nodesToJson(nodesList, mParam.getInputFilePathGraph(), mParam.getOutputFolderPath(), mParam.keepIsolatedVertices());
        } else
            throw new FileNotFoundException("Graph file doesn't exist, please review your data in properties file");

    }

    /**
     * Load an input graph in WebGraph format
     *
     * @return an ImmutableGraph instance
     */
    private ImmutableGraph loadGraph(String inputFilePath, boolean inMemory, boolean isolatedVertices) throws IOException {
        logger.info("Loading graph at filepath {} (in memory: {})", inputFilePath, inMemory);
        ImmutableGraph graph = inMemory ?
                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
                ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

        // check if it must remove isolated nodes
        if (!isolatedVertices) graph = Preprocessing.removeIsolatedNodes(graph);

        return graph;
    }

    /**
     * Get associated nodes for a list of seeds
     *
     * @param g     input graph
     * @param seeds seeds' list
     * @return associated nodes for a list of seeds
     */
    private int[] seedToNode(ImmutableGraph g, IntArrayList seeds, int numSeeds) {
        return minHashNodes(g, seeds, numSeeds);
    }

    /**
     * Creates hash values for all graph nodes
     * and store minhash nodes according to seedIndex
     *
     * @param g an input graph
     */
    private int[] minHashNodes(ImmutableGraph g, IntArrayList seeds, int numSeeds) {
        int[] minHashNodeIDs = new int[numSeeds];

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
    private void seedsToJson(List<IntArrayList> seeds, String inputFilePath, String outputFolderPath, boolean isolatedVertices) {
        Gson gson = new GsonBuilder().create();
        String graphFileName = Paths.get(inputFilePath).getFileName().toString();

        String path = outputFolderPath +
                File.separator +
                "seeds_" +
                graphFileName +
                "_" +
                (isolatedVertices ? "with_iso" : "without_iso") +
                ".json";

        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(seeds, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Seeds list wrote in {}", outputFolderPath);
    }

    /**
     * Write the nodes' list in a JSON file
     *
     * @param nodes nodes' list
     */
    private void nodesToJson(List<int[]> nodes, String inputFilePath, String outputFolderPath, boolean isolatedVertices) {
        Gson gson = new GsonBuilder().create();
        String graphFileName = Paths.get(inputFilePath).getFileName().toString();
        String path = outputFolderPath +
                File.separator +
                "nodes_" +
                graphFileName +
                "_" +
                (isolatedVertices ? "with_iso" : "without_iso") +
                ".json";

        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(nodes, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Nodes list wrote in {}", outputFolderPath);
    }
}
