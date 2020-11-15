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
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MinHashMain {

    private String mOutputFolderPath;
    private String mAlgorithmName;
    private MinHash mAlgorithm;
    private String mInputFilePath;
    private boolean mIsSeedsRandom;

    private String mInputFilePathSeed;
    private String mInputFilePathNodes;
    private int mNumTests;

    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.MinHashMain");

    /**
     * Run Minhash algorithm, exit from the process if direction of message transmission or seeds list are not
     * correctly set, or if input file is not correctly read from local file system.
     */
    public MinHashMain(){

        this.mInputFilePath = PropertiesManager.getProperty("minhash.inputFilePath");
        this.mOutputFolderPath = PropertiesManager.getProperty("minhash.outputFolderPath");
        this.mAlgorithmName = PropertiesManager.getProperty("minhash.algorithmName");

        this.mNumTests = Integer.parseInt(PropertiesManager.getProperty("minhash.numTests", Constants.NUM_RUN_DEFAULT));
        this.mIsSeedsRandom = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("minhash.isSeedsRandom"));
        // read external json file for seeds' lists (mandatory) and nodes' lists (optional)
        if (!mIsSeedsRandom) {
            mInputFilePathSeed = PropertiesManager.getPropertyIfNotEmpty("minhash.inputFilePathSeed");
            mInputFilePathNodes = PropertiesManager.getProperty("minhash.inputFilePathNodes");
        }

        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isolatedVertices"));
        String direction = PropertiesManager.getProperty("minhash.direction");
        int numSeeds = Integer.parseInt(PropertiesManager.getProperty("minhash.numSeeds"));
        double threshold = Double.parseDouble(PropertiesManager.getProperty("minhash.threshold"));

        try {
            initialize(isolatedVertices, direction, numSeeds, threshold);
        } catch (MinHash.DirectionNotSetException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        } catch (MinHash.SeedsException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(-2);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-3);
        }
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
     * @throws MinHash.DirectionNotSetException
     * @throws MinHash.SeedsException
     * @throws IOException
     */
    private void initialize(boolean isolatedVertices, String direction, int numSeeds, double threshold) throws MinHash.DirectionNotSetException, MinHash.SeedsException, IOException {

        try {
            MinHashFactory mhf = new MinHashFactory();
            mAlgorithm = mhf.getAlgorithm(AlgorithmEnum.valueOf(mAlgorithmName), mInputFilePath, mIsSeedsRandom, isolatedVertices, direction, numSeeds, threshold);
        } catch (IllegalArgumentException iae) {
            logger.error("There is no \"{}\" algorithm! ", mAlgorithmName);
            iae.printStackTrace();
            System.exit(-4);
        } catch (MinHash.DirectionNotSetException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

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

    private void run() throws IOException, MinHash.SeedsException {

        Measure measure;
        long startTime = System.currentTimeMillis();
        long totalTime;

        List<IntArrayList> seeds = new ArrayList<>();
        List<int[]> nodes = new ArrayList<>();

        if (!mIsSeedsRandom) {
            seeds = readSeedsFromJson();
            nodes = readNodesFromJson();
        }

        if (!mIsSeedsRandom) {
            mNumTests = mNumTests > seeds.size() ? seeds.size() : mNumTests;
            logger.info("Max number of run test executable is {}", mNumTests);
        }

        for (int i = 0; i < mNumTests; i++) {
            if (!mIsSeedsRandom) {
                mAlgorithm.setSeeds(seeds.get(i));
                mAlgorithm.setNodes(nodes.get(i));
            }
            measure = mAlgorithm.runAlgorithm();
            logger.info("\nLower Bound Diameter\t{}\nTotal Couples Reachable\t{}\nTotal couples Percentage\t{}\nAvg Distance\t{}\nEffective Diameter\t{}",
                    measure.getLowerBoundDiameter(), new BigDecimal(measure.getTotalCouples()).toPlainString(), new BigDecimal(measure.getTotalCouplePercentage()).toPlainString(), measure.getAvgDistance(), measure.getEffectiveDiameter());
            measure.setAlgorithmName(mAlgorithmName);
            measure.setRun(i + 1);

            String inputGraphName = new File(mInputFilePath).getName();
            String outputFilePath = mOutputFolderPath + File.separator + inputGraphName;
            writeOnFile(measure, outputFilePath);
            logger.info("Test n.{} executed correctly", i + 1);
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Application successfully completed. Time elapsed (in milliseconds) {}", totalTime);
    }

    /**
     * @return list of seeds' list read from external json file
     * @throws FileNotFoundException
     */
    private List<IntArrayList> readSeedsFromJson() throws FileNotFoundException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<IntArrayList>>() {
        }.getType();
        return gson.fromJson(new FileReader(mInputFilePathSeed), listType);
    }

    /**
     *
     * @return list of nodes' list read from external json file
     * @throws FileNotFoundException
     */
    private List<int[]> readNodesFromJson() throws FileNotFoundException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<int[]>>() {
        }.getType();
        return gson.fromJson(new FileReader(mInputFilePathNodes), listType);
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
