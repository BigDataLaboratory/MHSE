package it.bigdatalab.applications;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.model.SeedNode;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.GraphUtils;
import it.bigdatalab.utils.GsonHelper;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Generate mNumTest random seed and create an (optional) mNumTest list of associated nodes
 * if mInputFilePath of an input graph is specified
 */
public class CreateSeeds {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.application.CreateSeeds");

    private final Parameter mParam;
    private final GraphManager mGraph;

    public CreateSeeds(GraphManager g, Parameter param) {
        this.mGraph = g;
        this.mParam = param;
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
     *  Generate a random integer and append it
     *  to the seeds list
     */
    public static int[] genNodes(int numSeeds, int maxNumNodes) {
        int[] nodes = new int[numSeeds];
        for (int i = 0; i < numSeeds; i++)
            nodes[i] = ThreadLocalRandom.current().nextInt(0, maxNumNodes);
        return nodes;
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

    public static void main(String[] args) throws IOException {

        int numSeeds = Integer.parseInt(PropertiesManager.getProperty("seed.numSeeds", Constants.NUM_SEEDS_DEFAULT));
        String inputFilePath = PropertiesManager.getProperty("seed.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("seed.outFolderPath");
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("seed.isolatedVertices"));
        int numTest = Integer.parseInt(PropertiesManager.getProperty("seed.numTest", Constants.NUM_TEST_DEFAULT));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("seed.inMemory", Constants.FALSE));
        boolean webGraph = Boolean.parseBoolean(PropertiesManager.getProperty("graph.webGraph",Constants.FALSE));
        boolean compGraph = Boolean.parseBoolean(PropertiesManager.getProperty("graph.compressedGraph",Constants.TRUE));

        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTest)
                .setNumSeeds(numSeeds)
                .setInMemory(inMemory)
                .setIsolatedVertices(isolatedVertices)
                .setWebG(webGraph)
                .setCompG(compGraph)
                .build();

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
        //    public static GraphManager loadGraph(String inputFilePath, boolean inMemory, boolean isolatedVertices,boolean webGraph, boolean compGraph,boolean transpose, String direction) throws IOException {
        GraphManager g = GraphUtils.loadGraph(param.getInputFilePathGraph(),param.isInMemory(),param.keepIsolatedVertices(),param.getWebGraph(),param.getCompGraph(), param.isTranspose(), param.getDirection(),param.getCompEGraph());
        //CompressedGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());
        CreateSeeds createSeeds = new CreateSeeds(g, param);

        List<SeedNode> seedNode = createSeeds.generate();
        String path = param.getOutputFolderPath() +
                File.separator +
                Constants.SEEDNODE +
                Constants.NAMESEPARATOR +
                Paths.get(param.getInputFilePathGraph()).getFileName().toString() +
                Constants.NAMESEPARATOR +
                (param.keepIsolatedVertices() ? Constants.WITHISOLATED : Constants.WITHOUTISOLATED) +
                Constants.JSON_EXTENSION;

        GsonHelper.toJson(seedNode, path);

    }


    /**
     * Get associated nodes for a list of seeds
     *
     * @param g     input graph
     * @param seeds seeds' list
     * @return associated nodes for a list of seeds
     */
    private int[] seedToNode(GraphManager g, IntArrayList seeds, int numSeeds) {
        return minHashNodes(g, seeds, numSeeds);
    }

    /**
     * Creates hash values for all graph nodes
     * and store minhash nodes according to seedIndex
     *
     * @param g an input graph
     */
    private int[] minHashNodes(GraphManager g, IntArrayList seeds, int numSeeds) {
        int[] minHashNodeIDs = new int[numSeeds];

        for (int s = 0; s < seeds.size(); s++) {
            int [] nodeIter = g.get_nodes();
            //NodeIterator nodeIter = g.nodeIterator();
            int j = 0;
            long maxHashValue = Long.MAX_VALUE;
            //while (nodeIter.hasNext()) {
            while (j<nodeIter.length) {

                int node = nodeIter[j];
                j+=1;
                //int node = nodeIter.nextInt();
                long hashValue = hashFunction(node, seeds.getInt(s));
                if (hashValue < maxHashValue) {
                    maxHashValue = hashValue;
                    minHashNodeIDs[s] = node;
                }
            }
        }
        return minHashNodeIDs;
    }

    protected List<SeedNode> generate() {
        List<SeedNode> seedNode = new ArrayList<>();

        for (int i = 0; i < mParam.getNumTests(); i++) {
            IntArrayList seeds = CreateSeeds.genSeeds(mParam.getNumSeeds());
            int[] nodes = seedToNode(mGraph, seeds, mParam.getNumSeeds());
            seedNode.add(new SeedNode(seeds, nodes));
            logger.info("Created list #{}/{}", i + 1, mParam.getNumTests());
        }
        return seedNode;
    }
}
