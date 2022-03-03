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
import it.bigdatalab.model.SeedNode;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.GraphUtils;
import it.bigdatalab.utils.GsonHelper;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class MinHashMain {

    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.MinHashMain");

    private final Parameter mParam;

    /**
     * Run Minhash algorithm, exit from the process if direction of message transmission or seeds list are not
     * correctly set, or if input file is not correctly read from local file system.
     */
    public MinHashMain(Parameter param) {
        this.mParam = param;
    }


    /**
     * Write the statistics computed on the input graph in a JSON file
     *
     * @param measures input graph statistics
     * @param path     output file path of the JSON file
     */
    private static void writeOnFile(List<Measure> measures, String path) throws IOException {
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
        graphMeasures.addAll(measures);
        // No append replace the whole file
        FileWriter fw = new FileWriter(path);
        gson.toJson(graphMeasures, gmListType, fw);
        fw.close();

        logger.info("Graph measure wrote in " + path);
    }

    private static int[] rangeNodes(@NotNull String range) {
        return Arrays.stream(range.split(",")).mapToInt(Integer::parseInt).toArray();
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

    public static void main(String[] args)  throws CloneNotSupportedException {

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
        String inputFilePathSeedNode = null;
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
                inputFilePathSeedNode = PropertiesManager.getPropertyIfNotEmpty("minhash.inputFilePathSeedNode");
            }
        } else {
            if (numSeeds == 0)
                throw new IllegalArgumentException("# seeds must be set at value > 0 if seeds are random, please review your data");
        }

        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("minhash.isolatedVertices"));
        String direction = PropertiesManager.getPropertyIfNotEmpty("minhash.direction");
        boolean transpose = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("minhash.transpose"));
        boolean reorder = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("minhash.reorder"));

        double threshold = Double.parseDouble(PropertiesManager.getPropertyIfNotEmpty("minhash.threshold"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.inMemory", Constants.FALSE));
        boolean computeCentrality = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.computeCentrality", Constants.FALSE));
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("minhash.suggestedNumberOfThreads", Constants.NUM_THREAD_DEFAULT));
        boolean persistCollisionTable = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.persistCollisionTable", Constants.TRUE));
        boolean webGraph = Boolean.parseBoolean(PropertiesManager.getProperty("graph.webGraph",Constants.FALSE));
        boolean compGraph = Boolean.parseBoolean(PropertiesManager.getProperty("graph.compressedGraph",Constants.TRUE));

        Parameter param = new Parameter.Builder()
                .setAlgorithmName(algorithmName)
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(numSeeds)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setSeedsRandom(isSeedsRandom)
                .setInputFilePathSeedNode(inputFilePathSeedNode)
                .setIsolatedVertices(isolatedVertices)
                .setRange(range)
                .setThreshold(threshold)
                .setDirection(direction)
                .setComputeCentrality(computeCentrality)
                .setReordering(reorder)
                .setNumThreads(suggestedNumberOfThreads)
                .setPersistCollisionTable(persistCollisionTable)
                .setWebG(webGraph)
                .setCompG(compGraph)
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
                        "graph will be reordered by outdegree: {}\n" +
                        "algorithm must compute centrality: {}\n" +
                        "persist collision table: {}\n" +
                        "number of threads: {}\n" +
                        "Web graph: {}\n"+
                        "Compressed graph: {}\n"+
                        "\n********************************************************\n\n",
                param.getNumTests(),
                param.getAlgorithmName(),
                param.isTranspose(), param.getInputFilePathGraph(),
                param.isInMemory(),
                param.keepIsolatedVertices(),
                param.getOutputFolderPath(),
                param.getNumSeeds(), param.isAutomaticRange(),
                param.getDirection(),
                param.getThreshold(),
                param.getReordering(),
                param.computeCentrality(),
                param.persistCollisionTable(),
                param.getNumThreads(),
                param.getWebGraph(),
                param.getCompGraph());

        MinHashMain main = new MinHashMain(param);
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

    private int[] computeNodesFromRange(int start, int end) {
        return IntStream.rangeClosed(start, end).toArray();
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
     */
    public List<Measure> run() throws IOException, CloneNotSupportedException,MinHash.SeedsException {

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

        final GraphManager g = new GraphManager(mParam.getWebGraph(),mParam.getCompGraph(),mParam.getInputFilePathGraph(),mParam.isTranspose(),mParam.isInMemory(),mParam.keepIsolatedVertices(),mParam.getDirection(),mParam.getCompEGraph());
        //final GraphManager g = GraphUtils.loadGraph(mParam.getInputFilePathGraph(),mParam.isInMemory(),mParam.keepIsolatedVertices(),mParam.getWebGraph(),mParam.getCompGraph(), mParam.isTranspose(), mParam.getDirection());

        //final CompressedGraph g = new CompressedGraph(mParam.getInputFilePathGraph(),SplitInputPath[0]+".adjlist_offset.txt" ,true);
//        final ImmutableGraph g = GraphUtils.loadGraph(
//                mParam.getInputFilePathGraph(),
//                mParam.isTranspose(),
//                mParam.isInMemory(),
//                mParam.keepIsolatedVertices(),
//                mParam.getDirection(),
//                mParam.getReordering());

        MinHashFactory mhf = new MinHashFactory();

        for (int i = 0; i < numTest; i++) {

            MinHash minHash = mParam.isSeedsRandom() ?
                    mhf.getAlgorithm(g, AlgorithmEnum.valueOf(mParam.getAlgorithmName()), mParam.getNumSeeds(), mParam.getThreshold(), mParam.getNumThreads(), mParam.computeCentrality()) :
                    mhf.getAlgorithm(g, AlgorithmEnum.valueOf(mParam.getAlgorithmName()), mParam.getNumSeeds(), mParam.getThreshold(), seedsNodes.get(i).getSeeds(), seedsNodes.get(i).getNodes(), mParam.getNumThreads(), mParam.computeCentrality());

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

            if (!mParam.persistCollisionTable()) {
                if (measure instanceof GraphMeasureOpt) {
                    ((GraphMeasureOpt) measure).setCollisionsMatrix(null);
                } else if (measure instanceof GraphMeasure) {
                    ((GraphMeasure) measure).setCollisionsTable(null);
                }
                measure.setMinHashNodeIDs(null);
            }

            measures.add(measure);
            //writeOnFile(measure, outputFilePath);

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
