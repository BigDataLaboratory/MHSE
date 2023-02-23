package it.bigdatalab.applications;

import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.bigdatalab.algorithm.MinHash;
import it.bigdatalab.model.*;
import it.bigdatalab.utils.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class randomBFS {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.RandomBFS");
    private final Parameter mParam;
    private final ImmutableGraph mGraph;
    private final int nSeed;
    protected IntArrayList mSeeds;


    public randomBFS(Parameter param) throws IOException {
        this.mParam = param;
        this.mGraph = GraphUtils.loadGraph(param.getInputFilePathGraph(),param.isTranspose(),param.isInMemory(),param.keepIsolatedVertices(),"out");

        //this.mParam = param;
        this.nSeed = param.getNumSeeds();
    }

    public static void main(String[] args) throws IOException {
        String inputFilePath = PropertiesManager.getPropertyIfNotEmpty("randomBFS.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("randomBFS.outputFolderPath");
        int numTests = Integer.parseInt(PropertiesManager.getProperty("randomBFS.numTests", Constants.NUM_RUN_DEFAULT));
        int numSeeds = Integer.parseInt(PropertiesManager.getProperty("randomBFS.numSeeds"));
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("randomBFS.isolatedVertices"));
        boolean transpose = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("randomBFS.transpose"));
        double threshold = Double.parseDouble(PropertiesManager.getPropertyIfNotEmpty("randomBFS.threshold"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("randomBFS.inMemory", Constants.FALSE));
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("randomBFS.suggestedNumberOfThreads", Constants.NUM_THREAD_DEFAULT));
        Parameter param = new Parameter.Builder()
                .setAlgorithmName("randomBFS")
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(numSeeds)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setIsolatedVertices(isolatedVertices)
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
                        "number of seeds {}\n" +
                        "threshold for eff. diameter is: {}\n" +
                        "number of threads: {}\n" +
                        "\n********************************************************\n\n",
                param.getNumTests(),
                param.getAlgorithmName(),
                param.isTranspose(), param.getInputFilePathGraph(),
                param.isInMemory(),
                param.keepIsolatedVertices(),
                param.getOutputFolderPath(),
                param.getNumSeeds(),
                param.getThreshold(),
                param.getNumThreads());
        randomBFS rbf = new randomBFS(param);
        try{
            List<Measure> measures = rbf.run();
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
        }catch (IOException | MinHash.SeedsException e) {
            e.printStackTrace();
        }
    }

    private int getRandomNumber(int max) {
        return (int) ((Math.random() * (max)));
    }

    public List<Measure> run() throws IOException {
        Measure measure;
        int numTest = mParam.getNumTests();

        long startTime = System.currentTimeMillis();
        long totalTime;

        //List<SeedNode> seedsNodes = new ArrayList<>();
        List<Measure> measures = new ArrayList<>();


        for (int i = 0; i < numTest; i++) {
            measure = run_bfs();
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

    private Measure run_bfs(){
        mSeeds = new IntArrayList();
        int n = mGraph.numNodes();
        long startTime = System.currentTimeMillis();
        long totalTime,logTime,hopStartTime;
        long lastLogTime = startTime;
        //ProgressLogger pl = new ProgressLogger();
        //double avgDistance = 0.0;
        double[] dd = new double[n];
        double[] dist = new double[n];
        double lower_bound = 0;
        Arrays.fill(dd, 0);
        int seed;
        int h;

        for (int i = 0; i< nSeed; i++){
            seed = getRandomNumber(n);
            mSeeds.add(seed);
            Arrays.fill(dist, -1);
            Queue<Integer> ball = new LinkedList<>();
            ball.add(seed);
            dist[seed] = 0;
            h = 0;
            while(ball.size() != 0){
                hopStartTime =  System.currentTimeMillis();
                int w = ball.remove();
                final int d = mGraph.outdegree(w);
                final int[] successors = mGraph.successorArray(w);
                for (int l = 0;l<d; l++){
                    if (dist[successors[l]] == -1){
                        dist[successors[l]] = dist[w] +1;
                        dd[(int) dist[successors[l]]] +=1;
                        if (lower_bound < dist[successors[l]]){
                            lower_bound = dist[successors[l]];
                        }
                        ball.add(successors[l]);
                    }
                }
                logTime = System.currentTimeMillis();
                if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                    logger.info("# nodes analyzed {} / {} for hop {} [elapsed {}, node/s {}]",
                            n, mGraph.numNodes(),
                            h,
                            (logTime - hopStartTime) / (double) 1000,
                            ((n + 1) / ((logTime - hopStartTime) / (double) 1000)));
                    lastLogTime = logTime;
                }
                h+=1;
            }
        }
        double[] R = new double[(int) lower_bound];
        Arrays.fill(R, 0);
        double accum = 0;

        for (h = 0; h< lower_bound;h++){
            accum += dd[h];
            if (h == 0) {
                R[h] = n*dd[h]/nSeed;
            }else{
                R[h] = n*accum / nSeed;
            }
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setHopTable(R);
        graphMeasure.setLowerBoundDiameter((int) lower_bound);
        graphMeasure.setThreshold(mParam.getThreshold());
        graphMeasure.setSeedsList(mSeeds);
        graphMeasure.setNumSeeds(nSeed);
        // gm
        graphMeasure.setAvgDistance(Stats.averageDistance(R));
        graphMeasure.setTime(totalTime);
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(R, mParam.getThreshold()));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(R));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(R, mParam.getThreshold()));
        return graphMeasure;
    }




}
