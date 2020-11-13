package it.bigdatalab.applications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import it.bigdatalab.algorithm.AlgorithmEnum;
import it.bigdatalab.algorithm.MinHash;
import it.bigdatalab.algorithm.MinHashFactory;
import it.bigdatalab.model.GraphMeasure;
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

    private String outputFolderPath;
    private String algorithmName;
    private MinHash algorithm;
    private String inputFilePath;
    private boolean mIsSeedsRandom;
    private String mInputFilePathSeed;
    private String mInputFilePathNodes;
    private int numTests;

    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.MinHashMain");

    /**
     * Run Minhash algorithm, exit from the process if direction of message transmission or seeds list are not
     * correctly set, or if input file is not correctly read from local file system.
     */
    public MinHashMain(){
        try {
            initialize();
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
     * Write the statistics computed on the input graph in a JSON file
     *
     * @param graphMeasure input graph statistics
     * @param path         output file path of the JSON file
     */
    private static void writeOnFile(GraphMeasure graphMeasure, String path) throws IOException {
        path += Constants.JSON_EXTENSION;
        Gson gson = new GsonBuilder().create();

        Type gmListType = new TypeToken<List<GraphMeasure>>() {
        }.getType();

        boolean exist = new File(path).isFile();
        List<GraphMeasure> graphMeasures = new ArrayList<>();

        if (exist) {
            FileReader fr = new FileReader(path);
            graphMeasures = new Gson().fromJson(fr, gmListType);
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
        gson.toJson(graphMeasures, fw);
        fw.close();

        logger.info("Graph measure wrote in " + path);
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
     * @throws MinHash.DirectionNotSetException
     * @throws MinHash.SeedsException
     * @throws IOException
     */
    private void initialize() throws MinHash.DirectionNotSetException, MinHash.SeedsException, IOException {
        inputFilePath = PropertiesManager.getProperty("minhash.inputFilePath");
        outputFolderPath = PropertiesManager.getProperty("minhash.outputFolderPath");
        algorithmName = PropertiesManager.getProperty("minhash.algorithmName");

        numTests = Integer.parseInt(PropertiesManager.getProperty("minhash.numTests", Constants.NUM_RUN_DEFAULT));
        mIsSeedsRandom = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("minhash.isSeedsRandom"));
        // read external json file for seeds' lists (mandatory) and nodes' lists (optional)
        if (!mIsSeedsRandom) {
            mInputFilePathSeed = PropertiesManager.getPropertyIfNotEmpty("minhash.inputFilePathSeed");
            mInputFilePathNodes = PropertiesManager.getProperty("minhash.inputFilePathNodes");
        }

        try {
            MinHashFactory mhf = new MinHashFactory();
            algorithm = mhf.getAlgorithm(AlgorithmEnum.valueOf(algorithmName));

        } catch(IllegalArgumentException iae){
            logger.error("There is no \"{}\" algorithm! ", algorithmName);
            iae.printStackTrace();
            System.exit(-4);
        } catch (MinHash.DirectionNotSetException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void run() throws IOException, MinHash.SeedsException {

        GraphMeasure graphMeasure;
        long startTime = System.currentTimeMillis();
        long totalTime;

        List<IntArrayList> seeds = new ArrayList<>();
        List<int[]> nodes = new ArrayList<>();

        if (!mIsSeedsRandom) {
            seeds = readSeedsFromJson();
            nodes = readNodesFromJson();
        }

        if (!mIsSeedsRandom) {
            numTests = numTests > seeds.size() ? seeds.size() : numTests;
            logger.info("Max number of run test executable is {}", numTests);
        }

        for (int i = 0; i < numTests; i++) {
            if (!mIsSeedsRandom) {
                algorithm.setSeeds(seeds.get(i));
                algorithm.setNodes(nodes.get(i));
            }
            graphMeasure = algorithm.runAlgorithm();
            logger.info("\nLower Bound Diameter\t{}\nTotal Couples Reachable\t{}\nTotal couples Percentage\t{}\nAvg Distance\t{}\nEffective Diameter\t{}",
                    graphMeasure.getLowerBoundDiameter(), new BigDecimal(graphMeasure.getTotalCouples()).toPlainString(), new BigDecimal(graphMeasure.getTotalCouplePercentage()).toPlainString(), graphMeasure.getAvgDistance(), graphMeasure.getEffectiveDiameter());

            graphMeasure.setAlgorithmName(algorithmName);
            String inputGraphName = new File(inputFilePath).getName();
            String outputFilePath = outputFolderPath + File.separator + inputGraphName;
            writeOnFile(graphMeasure, outputFilePath);
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
