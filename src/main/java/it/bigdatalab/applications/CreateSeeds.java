package it.bigdatalab.applications;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.model.SeedNode;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.GraphUtils;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
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
import java.util.stream.Collectors;


/**
 * Generate mNumTest random seed and create an (optional) mNumTest list of associated nodes
 * if mInputFilePath of an input graph is specified
 */
public class CreateSeeds {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.application.CreateSeeds");

    private final Parameter mParam;

    public CreateSeeds(Parameter param) {
        this.mParam = param;
    }

    public static void main(String[] args) throws IOException {

        int numSeeds = Integer.parseInt(PropertiesManager.getProperty("seed.numSeeds", Constants.NUM_SEEDS_DEFAULT));
        String inputFilePath = PropertiesManager.getProperty("seed.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("seed.outFolderPath");
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("seed.isolatedVertices"));
        int numTest = Integer.parseInt(PropertiesManager.getProperty("seed.numTest", Constants.NUM_TEST_DEFAULT));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("seed.inMemory", Constants.FALSE));

        Parameter param = new Parameter.Builder()
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
                param.getNumTests(),
                param.getInputFilePathGraph(),
                param.isInMemory(),
                param.keepIsolatedVertices(),
                param.getOutputFolderPath(),
                param.getNumSeeds());


        CreateSeeds createSeeds = new CreateSeeds(param);
        List<SeedNode> seedNode = createSeeds.generate();
        List<IntArrayList> seeds = seedNode.stream().map(SeedNode::getSeeds).collect(Collectors.toList());
        List<int[]> nodes = seedNode.stream().map(SeedNode::getNodes).collect(Collectors.toList());
        createSeeds.seedsToJson(seeds);
        createSeeds.nodesToJson(nodes);
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
        return hf.hashLong(node).asLong();
    }

    protected List<SeedNode> generate() throws IOException {
        boolean exist = new File(mParam.getInputFilePathGraph() + Constants.GRAPH_EXTENSION).isFile() && !mParam.getInputFilePathGraph().isEmpty();

        if (exist) {
            ImmutableGraph g = GraphUtils.loadGraph(mParam.getInputFilePathGraph(), mParam.isInMemory(), mParam.keepIsolatedVertices());
            List<SeedNode> seedNode = new ArrayList<>();

            for (int i = 0; i < mParam.getNumTests(); i++) {
                IntArrayList seeds = CreateSeeds.genSeeds(mParam.getNumSeeds());
                int[] nodes = seedToNode(g, seeds, mParam.getNumSeeds());
                seedNode.add(new SeedNode(seeds, nodes));
                logger.info("Created list #{}/{}", i + 1, mParam.getNumTests());
            }
            return seedNode;
        } else
            throw new FileNotFoundException("Graph file doesn't exist, please review your data in properties file");

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
    protected void seedsToJson(List<IntArrayList> seeds, String inputFilePath, String outputFolderPath, boolean isolatedVertices) {
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
     * Write the seeds' list in a JSON file
     *
     * @param seeds seeds list
     */
    protected void seedsToJson(List<IntArrayList> seeds) throws IOException {
        Gson gson = new GsonBuilder().create();
        String graphFileName = Paths.get(mParam.getInputFilePathGraph()).getFileName().toString();

        String path = mParam.getOutputFolderPath() +
                File.separator +
                "seeds_" +
                graphFileName +
                "_" +
                (mParam.keepIsolatedVertices() ? "with_iso" : "without_iso") +
                ".json";

        FileWriter writer = new FileWriter(path);
        gson.toJson(seeds, writer);
        logger.info("Seeds list wrote in {}", mParam.getOutputFolderPath());
    }

    /**
     * Write the nodes' list in a JSON file
     *
     * @param nodes nodes' list
     */
    protected void nodesToJson(List<int[]> nodes, String inputFilePath, String outputFolderPath, boolean isolatedVertices) {
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

    /**
     * Write the nodes' list in a JSON file
     *
     * @param nodes nodes' list
     */
    protected void nodesToJson(List<int[]> nodes) throws IOException {
        Gson gson = new GsonBuilder().create();
        String graphFileName = Paths.get(mParam.getInputFilePathGraph()).getFileName().toString();
        String path = mParam.getOutputFolderPath() +
                File.separator +
                "nodes_" +
                graphFileName +
                "_" +
                (mParam.keepIsolatedVertices() ? "with_iso" : "without_iso") +
                ".json";

        FileWriter writer = new FileWriter(path);
        gson.toJson(nodes, writer);
        logger.info("Nodes list wrote in {}", mParam.getOutputFolderPath());
    }
}
