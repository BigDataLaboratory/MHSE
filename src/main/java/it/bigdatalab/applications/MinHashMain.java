package it.bigdatalab.applications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.bigdatalab.algorithm.AlgorithmEnum;
import it.bigdatalab.algorithm.MinHash;
import it.bigdatalab.algorithm.MinHashFactory;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Preprocessing;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class MinHashMain {

    private final Parameter mParam;

    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.MinHashMain");

    /**
     * Run Minhash algorithm, exit from the process if direction of message transmission or seeds list are not
     * correctly set, or if input file is not correctly read from local file system.
     */
    public MinHashMain() {

        logger.info("\n\n\n" +
                "|\\    /| |    |  |¯¯¯¯  |¯¯¯¯\n" +
                "| \\  / | |----|  |____  |--- \n" +
                "|  \\/  | |    |  _____| |____\n\n\n");

        String inputFilePath = PropertiesManager.getPropertyIfNotEmpty("minhash.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("minhash.outputFolderPath");
        String algorithmName = PropertiesManager.getPropertyIfNotEmpty("minhash.algorithmName");

        int numTests = Integer.parseInt(PropertiesManager.getProperty("minhash.numTests", Constants.NUM_RUN_DEFAULT));

        int numSeeds = Integer.parseInt(PropertiesManager.getProperty("minhash.numSeeds"));
        boolean isSeedsRandom = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("minhash.isSeedsRandom"));

        // read external json file for seeds' lists (mandatory) and nodes' lists (optional)
        String inputFilePathSeed = null;
        String inputFilePathNodes = null;
        int[] range = null;
        if (!isSeedsRandom) {
            if (numSeeds == 0) {
                //used to compute ground truth
                String nodeIDRange = PropertiesManager.getPropertyIfNotEmpty("minhash.nodeIDRange");
                range = rangeNodes(nodeIDRange);
                numSeeds = range[1] - range[0] + 1;
                logger.info("Set range for node ids = {}, numSeeds automatically reset to {}", range, numSeeds);
            } else {
                //Load minHash node IDs from properties file
                inputFilePathSeed = PropertiesManager.getPropertyIfNotEmpty("minhash.inputFilePathSeed");
                inputFilePathNodes = PropertiesManager.getProperty("minhash.inputFilePathNodes");
            }
        } else {
            if (numSeeds == 0)
                throw new IllegalArgumentException("# seeds must be set at value > 0 if seeds are random, please review your data");
        }

        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("minhash.isolatedVertices"));
        String direction = PropertiesManager.getPropertyIfNotEmpty("minhash.direction");
        boolean transpose = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("minhash.transpose"));

        double threshold = Double.parseDouble(PropertiesManager.getPropertyIfNotEmpty("minhash.threshold"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.inMemory", Constants.FALSE));
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("minhash.suggestedNumberOfThreads", Constants.NUM_THREAD_DEFAULT));
        suggestedNumberOfThreads = getNumberOfMaxThreads(suggestedNumberOfThreads);

        mParam = new Parameter.Builder()
                .setAlgorithmName(algorithmName)
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(numSeeds)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setSeedsRandom(isSeedsRandom)
                .setInputFilePathNodes(inputFilePathNodes)
                .setInputFilePathSeed(inputFilePathSeed)
                .setIsolatedVertices(isolatedVertices)
                .setRange(range)
                .setThreshold(threshold)
                .setDirection(direction)
                .setNumThreads(suggestedNumberOfThreads)
                .build();

        logger.info("\n\n********************** Parameters **********************\n\n" +
                        "# executions will be run {} time(s)\n" +
                        "ready to start algorithm: {}\n" +
                        "on graph (transpose version? {}) read from: {}\n" +
                        "loading graph in memory? {}\n" +
                        "keep isolated nodes? {}\n" +
                        "results will written in: {}\n" +
                        "number of seeds {}, automatic range? {}\n" +
                        "direction is: {}\n" +
                        "threshold for eff. diameter is: {}\n" +
                        "number of threads: {}\n" +
                        "\n********************************************************\n\n",
                mParam.getNumTests(),
                mParam.getAlgorithmName(),
                mParam.isTranspose(), mParam.getInputFilePathGraph(),
                mParam.isInMemory(),
                mParam.keepIsolatedVertices(),
                mParam.getOutputFolderPath(),
                mParam.getNumSeeds(), mParam.isAutomaticRange(),
                mParam.getDirection(),
                mParam.getThreshold(),
                mParam.getNumThreads());
    }

    /**
     * Run Minhash algorithm (specified in the algorithmName parameter) using properties read from properties file such as:
     * - inputFilePath  the path to the input file representing a graph in a WebGraph format. If the input graph has an edgelist format
     * - outputFolderPath the path to the output folder path that will contain results of the execution of the algorithm
     * - algorithmName represent the name of the MinHash algorithm to be executed (see AlghorithmEnum for available algorithms)
     * - mIsSeedsRandom if it is False, seeds' list and nodes' list must be read from external json file for testing purpose
     * whose paths are set in mInputFilePathSeed and mInputFilePathNodes
     * - numTests number of tests to be executed
     * If algorithmName is empty or not available in AlgorithmEnum, exit from the process
     *
     */


    /**
     * Write the statistics computed on the input graph in a JSON file
     *
     * @param graphMeasure input graph statistics
     * @param path         output file path of the JSON file
     */
    private static void writeOnFile(Measure graphMeasure, String path) throws IOException {
        path += Constants.JSON_EXTENSION;

        RuntimeTypeAdapterFactory<Measure> typeAdapterFactory = RuntimeTypeAdapterFactory
                .of(Measure.class, "type")
                .registerSubtype(GraphMeasure.class, GraphMeasure.class.getName())
                .registerSubtype(GraphMeasureOpt.class, GraphMeasureOpt.class.getName());

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeAdapterFactory)
                .create();

        Type gmListType = new TypeToken<List<Measure>>() {
        }.getType();

        boolean exist = new File(path).isFile();
        List<Measure> graphMeasures = new ArrayList<>();

        if (exist) {
            FileReader fr = new FileReader(path);
            graphMeasures = gson.fromJson(fr, gmListType);
            fr.close();
            // If graph measures list is empty
            if (null == graphMeasures) {
                graphMeasures = new ArrayList<>();
            }
        }

        // Add new graphMeasure to the list
        graphMeasures.add(graphMeasure);
        // No append replace the whole file
        FileWriter fw = new FileWriter(path);
        gson.toJson(graphMeasures, gmListType, fw);
        fw.close();

        logger.info("Graph measure wrote in " + path);
    }

    /**
     * Number of max threads to use for the computation
     *
     * @param suggestedNumberOfThreads if not equal to zero return the number of threads
     *                                 passed as parameter, else the number of max threads available
     * @return number of threads to use for the computation
     */
    private static int getNumberOfMaxThreads(int suggestedNumberOfThreads) {
        if (suggestedNumberOfThreads != 0) return suggestedNumberOfThreads;
        return Runtime.getRuntime().availableProcessors();
    }

    private ImmutableGraph loadGraph(String inputFilePath, boolean transpose, boolean inMemory, boolean isolatedVertices, String direction) throws IOException {
        logger.info("Loading graph at filepath {} (in memory: {})", inputFilePath, inMemory);
        ImmutableGraph graph = inMemory ?
                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
                ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

        // check if it must remove isolated nodes
        if (!isolatedVertices) {
            graph = Preprocessing.removeIsolatedNodes(graph);
        }

        // transpose graph based on direction selected
        // and graph type loaded (original or transposed)
        if (transpose) {
            if (direction.equals(Constants.IN_DIRECTION)) {
                logger.info("Transposing graph cause direction is {}", direction);
                graph = Transform.transpose(graph);
                logger.debug("Transposing graph ended");
            }
        } else {
            if (direction.equals(Constants.OUT_DIRECTION)) {
                logger.info("Transposing graph cause direction is {}", direction);
                graph = Transform.transpose(graph);
                logger.debug("Transposing graph ended");
            }
        }

/*todo
        logger.info("\n\n********************** Graph Info **********************\n" +
                        "# nodes:\t{}\n" +
                        "# edges:\t{}\n" +
                        "********************************************************\n\n",
                graph.numNodes(), graph.numArcs());
*/

        return graph;
    }

    private void run() throws IOException, MinHash.SeedsException {

        Measure measure;
        int numTest = mParam.getNumTests();

        long startTime = System.currentTimeMillis();
        long totalTime;

        List<IntArrayList> seeds = new ArrayList<>();
        List<int[]> nodes = new ArrayList<>();

        if (!mParam.isSeedsRandom()) {
            // only for boolean version of minhash
            if (mParam.getRange() != null) {
                int[] n = computeNodesFromRange(mParam.getRange()[0], mParam.getRange()[1]);
                nodes.add(n);
            } else {
                seeds = readSeedsFromJson(mParam.getInputFilePathSeed());
                nodes = readNodesFromJson(mParam.getInputFilePathNodes());
                if (numTest > seeds.size() || numTest > nodes.size())
                    throw new IllegalStateException("# run > list of seeds/nodes, please review your input");
            }
        }

        final ImmutableGraph g = loadGraph(
                mParam.getInputFilePathGraph(),
                mParam.isTranspose(),
                mParam.isInMemory(),
                mParam.keepIsolatedVertices(),
                mParam.getDirection());

        MinHashFactory mhf = new MinHashFactory();
        MinHash minHash = mhf.getAlgorithm(
                g,
                AlgorithmEnum.valueOf(mParam.getAlgorithmName()),
                mParam.isSeedsRandom(),
                mParam.getNumSeeds(),
                mParam.getThreshold(),
                mParam.getNumThreads());

        for (int i = 0; i < numTest; i++) {

            if (!mParam.isSeedsRandom()) {
                if (mParam.getRange() == null) {
                    minHash.setSeeds(seeds.get(i));
                }
                minHash.setNodes(nodes.get(i));
            }

            measure = minHash.runAlgorithm();

            measure.setAlgorithmName(mParam.getAlgorithmName());
            measure.setRun(i + 1);
            measure.setDirection(mParam.getDirection());

            logger.info("\n\n********************* Stats ****************************\n\n" +
                            "Lower Bound Diameter\t{}\n" +
                            "Total Couples Reachable\t{}\n" +
                            "Total couples Percentage\t{}\n" +
                            "Avg Distance\t{}\n" +
                            "Effective Diameter\t{}\n" +
                            "\n********************************************************\n\n",
                    measure.getLowerBoundDiameter(),
                    BigDecimal.valueOf(measure.getTotalCouples()).toPlainString(),
                    BigDecimal.valueOf(measure.getTotalCouplePercentage()).toPlainString(),
                    measure.getAvgDistance(),
                    measure.getEffectiveDiameter());

            if (minHash.getNodes().length == g.numNodes()) {
                if (measure instanceof GraphMeasureOpt) {
                    ((GraphMeasureOpt) measure).setCollisionsMatrix(null);
                } else if (measure instanceof GraphMeasure) {
                    ((GraphMeasure) measure).setCollisionsTable(null);
                }
                measure.setMinHashNodeIDs(null);
            }

            String inputGraphName = new File(mParam.getInputFilePathGraph()).getName();
            String outputFilePath = mParam.getOutputFolderPath() + File.separator + inputGraphName;
            writeOnFile(measure, outputFilePath);
            logger.info("\n\n********************************************************\n\n" +
                            "Test n.{} executed correctly\n\n" +
                            "********************************************************\n\n",
                    i + 1);
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Application successfully completed. Time elapsed (in milliseconds) {}", totalTime);
    }

    /**
     * @return list of seeds' list read from external json file
     * @throws FileNotFoundException
     */
    private List<IntArrayList> readSeedsFromJson(String inputFilePath) throws FileNotFoundException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<IntArrayList>>() {
        }.getType();
        return gson.fromJson(new FileReader(inputFilePath), listType);
    }

    /**
     * @return list of nodes' list read from external json file
     * @throws FileNotFoundException
     */
    private List<int[]> readNodesFromJson(String inputFilePath) throws FileNotFoundException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<int[]>>() {
        }.getType();
        return gson.fromJson(new FileReader(inputFilePath), listType);
    }

    private int[] rangeNodes(@NotNull String range) {
        return Arrays.stream(range.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    private int[] computeNodesFromRange(int start, int end) {
        return IntStream.rangeClosed(start, end).toArray();
    }

    public static void main(String[] args) {
        MinHashMain main = new MinHashMain();
        try {
            main.run();
        } catch (IOException | MinHash.SeedsException e) {
            e.printStackTrace();
        }
    }


}
