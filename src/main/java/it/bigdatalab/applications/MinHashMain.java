package it.bigdatalab.applications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.bigdatalab.algorithm.*;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.util.Arrays;

public class MinHashMain {

    private String outputFolderPath;
    private String algorithmName;
    private MinHash algorithm;
    private String inputFilePath;
    private boolean runTests;
    private int numTests;

    private static final Logger logger = LoggerFactory.getLogger("it.misebigdatalab");

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
     * Run Minhash algorithm (specified in the algorithmName parameter) using properties read from properties file such as:
     * - inputFilePath  the path to the input file representing a graph in a WebGraph format. If the input graph has an edgelist format
     * - outputFolderPath the path to the output folder path that will contain results of the execution of the algorithm
     * - algorithmName represent the name of the MinHash algorithm to be executed (see AlghorithmEnum for available algorithms)
     * - runTests if it is True, the Test mode will be activated, multiple tests will be run for the same algorithm.
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
        runTests = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.runTests"));
        numTests = Integer.parseInt(PropertiesManager.getProperty("minhash.numTests"));

        try {
            MinHashFactory mhf = new MinHashFactory();
            algorithm = mhf.getAlgorithm(AlgorithmEnum.valueOf(algorithmName));
        } catch(IllegalArgumentException iae){
            logger.error("There is no \"{}\" algorithm! ", algorithmName);
            iae.printStackTrace();
            System.exit(-4);
        }
    }


    private void run() throws IOException, MinHash.SeedsException {

        GraphMeasure graphMeasure;
        long startTime;
        long endTime;
        long totalTime;

        //TODO REFACTOR?
        if(runTests) {
            graphMeasure = new GraphMeasure();
            for(int i=1;i<=numTests;i++){
                logger.info("Executing test n.{}", i);
                //numTests executions
                startTime = System.currentTimeMillis();
                String propertyName = "minhash.seeds" + i;
                String seedsString = PropertiesManager.getProperty(propertyName);
                //check if the new seedsString has the same number of elements of the property mhse.numSeeds
                int[] seeds = Arrays.stream(seedsString.split(",")).mapToInt(Integer::parseInt).toArray();
                int numSeeds = Integer.parseInt(PropertiesManager.getProperty("minhash.numSeeds"));
                if (numSeeds != seeds.length) {
                    String message = "Specified different number of seeds in properties.  \"mhse.numSeeds\" is " + numSeeds + " and \"" + propertyName + "\" length is " + seeds.length;
                    throw new MinHash.SeedsException(message);
                }

                PropertiesManager.setProperty("minhash.seeds", seedsString);

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

                graphMeasure = algorithm.runAlgorithm();
                logger.info("\nLower Bound Diameter\t{}\nTotal Couples Reachable\t{}\nTotal couples Percentage\t{}\nAvg Distance\t{}\nEffective Diameter\t{}",
                        graphMeasure.getLowerBoundDiameter(), new BigDecimal(graphMeasure.getTotalCouples()).toPlainString() , new BigDecimal(graphMeasure.getTotalCouplePercentage()).toPlainString(), graphMeasure.getAvgDistance(), graphMeasure.getEffectiveDiameter());
                endTime = System.currentTimeMillis();
                totalTime = endTime - startTime;
                logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);
                graphMeasure.setAlgorithmName(algorithmName);
                graphMeasure.setTime(totalTime);
                String inputGraphName = new File(inputFilePath).getName();
                String outputFilePath = outputFolderPath + File.separator + inputGraphName;
                writeOnFile(graphMeasure, outputFilePath);
                logger.info("Test n.{} executed correctly", i);
            }
        } else {
            //Single execution
            startTime = System.currentTimeMillis();
            graphMeasure = algorithm.runAlgorithm();
            logger.info("\nLower Bound Diameter\t{}\nTotal Couples Reachable\t{}\nTotal couples Percentage\t{}\nAvg Distance\t{}\nEffective Diameter\t{}",
                    graphMeasure.getLowerBoundDiameter(), new BigDecimal(graphMeasure.getTotalCouples()).toPlainString() , new BigDecimal(graphMeasure.getTotalCouplePercentage()).toPlainString(), graphMeasure.getAvgDistance(), graphMeasure.getEffectiveDiameter());

            endTime = System.currentTimeMillis();
            totalTime = endTime - startTime;
            logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);
            graphMeasure.setAlgorithmName(algorithmName);
            graphMeasure.setTime(totalTime);

            String graphName = new File(inputFilePath).getName();
            String outputFilePath = outputFolderPath + File.separator + graphName;
            writeOnFile(graphMeasure, outputFilePath);
        }
    }

    /**
     * Write the statistics computed on the input graph in a JSON file
     * @param graphMeasure input graph statistics
     * @param path output file path of the JSON file
     */
    private static void writeOnFile(GraphMeasure graphMeasure, String path) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        File output = new File(path);
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output, true))); // append mode file writer
        out.println();
        objectMapper.writeValue(out, graphMeasure);
        out.close();
        logger.info("Graph measure wrote in " + path);
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
