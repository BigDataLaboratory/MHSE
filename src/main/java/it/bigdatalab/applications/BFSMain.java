package it.bigdatalab.applications;

import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.bigdatalab.algorithm.MinHash;
import it.bigdatalab.algorithm.RandomBFS;
import it.bigdatalab.model.*;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.GraphUtils;
import it.bigdatalab.utils.GsonHelper;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BFSMain extends Main {

    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.BFSMain");


    /**
     * Run BFS algorithm, exit from the process if seeds list is not
     * correctly set, or if input file is not correctly read from local file system.
     */
    public BFSMain(Parameter param) {
        super(param);
    }

    public static void main(String[] args) {

        logger.info("\n\n\n" +
                "|\\    /| |    |  |¯¯¯¯  |¯¯¯¯\n" +
                "| \\  / | |----|  |____  |--- \n" +
                "|  \\/  | |    |  _____| |____\n\n\n");

        String inputFilePath = PropertiesManager.getPropertyIfNotEmpty("randomBFS.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("randomBFS.outputFolderPath");
        String algorithmName = PropertiesManager.getPropertyIfNotEmpty("randomBFS.algorithmName");

        int numTests = Integer.parseInt(PropertiesManager.getProperty("randomBFS.numTests", Constants.NUM_RUN_DEFAULT));

        int numSeeds = Integer.parseInt(PropertiesManager.getProperty("randomBFS.numSeeds"));
        boolean isSeedsRandom = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("randomBFS.isSeedsRandom"));
        String direction = Constants.OUT_DIRECTION;

        // read external json file for seeds' lists (mandatory) and nodes' lists (optional)
        String inputFilePathSeedNode = null;
        int[] range = null;
        if (!isSeedsRandom) {
            //Load random bfs node IDs from properties file
            inputFilePathSeedNode = PropertiesManager.getPropertyIfNotEmpty("randomBFS.inputFilePathSeedNode");
        } else {
            if (numSeeds == 0)
                throw new IllegalArgumentException("# seeds must be set at value > 0 if seeds are random, please review your data");
        }

        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("randomBFS.isolatedVertices"));
        boolean transpose = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("randomBFS.transpose"));

        boolean computeCentrality = Boolean.parseBoolean(PropertiesManager.getProperty("randomBFS.computeCentrality", Constants.FALSE));
        double threshold = Double.parseDouble(PropertiesManager.getPropertyIfNotEmpty("randomBFS.threshold"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("randomBFS.inMemory", Constants.FALSE));
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("randomBFS.suggestedNumberOfThreads", Constants.NUM_THREAD_DEFAULT));

        Parameter param = new Parameter.Builder()
                .setAlgorithmName(algorithmName)
                .setDirection(direction)
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(numSeeds)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setSeedsRandom(isSeedsRandom)
                .setComputeCentrality(computeCentrality)
                .setInputFilePathSeedNode(inputFilePathSeedNode)
                .setIsolatedVertices(isolatedVertices)
                .setRange(range)
                .setThreshold(threshold)
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
                        "threshold for eff. diameter is: {}\n" +
                        "algorithm must compute centrality: {}\n" +
                        "number of threads: {}\n" +
                        "\n********************************************************\n\n",
                param.getNumTests(),
                param.getAlgorithmName(),
                param.isTranspose(), param.getInputFilePathGraph(),
                param.isInMemory(),
                param.keepIsolatedVertices(),
                param.getOutputFolderPath(),
                param.getNumSeeds(), param.isAutomaticRange(),
                param.getThreshold(),
                param.computeCentrality(),
                param.getNumThreads());

        BFSMain main = new BFSMain(param);
        try {
            List<Measure> measures = main.run();
            String inputGraphName = new File(param.getInputFilePathGraph()).getName();
            String outputFilePath = param.getOutputFolderPath() + File.separator + inputGraphName + Constants.NAMESEPARATOR + param.getAlgorithmName() + Constants.JSON_EXTENSION;

            RuntimeTypeAdapterFactory<Measure> adapter = RuntimeTypeAdapterFactory.of(Measure.class, "type")
                    .registerSubtype(GraphMeasure.class, GraphMeasure.class.getName())
                    .registerSubtype(GraphMeasureOpt.class, GraphMeasureOpt.class.getName());

            List<Measure> measuresRead = GsonHelper.fromJson(
                    outputFilePath, new TypeToken<List<Measure>>() {
                    }.getType(), adapter);

            measuresRead.addAll(measures);

            GsonHelper.toJson(
                    measuresRead,
                    outputFilePath,
                    new TypeToken<List<Measure>>() {
                    }.getType(),
                    adapter);

        } catch (IOException | MinHash.SeedsException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run RandomBFS algorithm using properties read from properties file such as:
     * - inputFilePath  the path to the input file representing a graph in a WebGraph format. If the input graph has an edgelist format
     * - outputFolderPath the path to the output folder path that will contain results of the execution of the algorithm
     * - mIsSeedsRandom if it is False, seeds' list and nodes' list must be read from external json file for testing purpose
     * whose paths are set in mInputFilePathSeed and mInputFilePathNodes
     * - numTests number of tests to be executed
     * If algorithmName is empty or not available in AlgorithmEnum, exit from the process
     */
    public List<Measure> run() throws IOException, MinHash.SeedsException {
        Measure measure;
        int numTest = mParam.getNumTests();

        long startTime = System.currentTimeMillis();
        long totalTime;

        List<SeedNode> seedsNodes = new ArrayList<>();
        List<Measure> measures = new ArrayList<>();

        if (!mParam.isSeedsRandom()) {
            // only for boolean version of minhash
            if (mParam.getRange() != null) {
                int[] n = computeNodesFromRange(mParam.getRange()[0], mParam.getRange()[1]);
                seedsNodes.add(new SeedNode(null, n));
            } else {
                seedsNodes = GsonHelper.fromJson(mParam.getInputFilePathSeedNode(), new TypeToken<List<SeedNode>>() {
                }.getType());
            }
            if (numTest > seedsNodes.size())
                throw new IllegalStateException("# run > list of seeds/nodes, please review your input");
        }

        final ImmutableGraph g = GraphUtils.loadGraph(
                mParam.getInputFilePathGraph(),
                mParam.isTranspose(),
                mParam.isInMemory(),
                mParam.keepIsolatedVertices(),
                mParam.getDirection(),
                mParam.getReordering());

        for (int i = 0; i < numTest; i++) {
            RandomBFS randomBFS = mParam.isSeedsRandom() ?
                    new RandomBFS(g, mParam.getNumSeeds(), mParam.getThreshold(), mParam.getNumThreads(), mParam.computeCentrality()) :
                    new RandomBFS(g, mParam.getNumSeeds(), mParam.getThreshold(), seedsNodes.get(i).getNodes(), mParam.getNumThreads(), mParam.computeCentrality());

            measure = randomBFS.runAlgorithm();
            measure.setAlgorithmName(mParam.getAlgorithmName());
            measure.setRun(i + 1);

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

            measures.add(measure);

            logger.info("\n\n********************************************************\n\n" +
                            "Test n.{} executed correctly\n\n" +
                            "********************************************************\n\n",
                    i + 1);
        }
        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Application successfully completed. Time elapsed (in milliseconds) {}", totalTime);
        return measures;
    }

}
