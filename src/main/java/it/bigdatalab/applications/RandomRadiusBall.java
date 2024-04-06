package it.bigdatalab.applications;

import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.bigdatalab.algorithm.MinHash;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class RandomRadiusBall {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.RandomRadiusBall");
    private final Parameter mParam;
    private final ImmutableGraph mGraph;
    private final int t;
    protected IntArrayList mSeeds;

    private boolean doCentrality;

    private boolean mNormalized;

    public RandomRadiusBall(@NotNull Parameter param) throws IOException {
        this.mParam = param;
        this.mGraph = GraphUtils.loadGraph(param.getInputFilePathGraph(),param.isTranspose(),param.isInMemory(),param.keepIsolatedVertices(),"out");

        //this.mParam = param;
        this.t = param.getNumSeeds();
    }
    // Implementation of the algorithm Random-Radius Ball Method for Estimating Closeness Centrality


    public static void main(String[] args) throws IOException {

        String inputFilePath = PropertiesManager.getPropertyIfNotEmpty("RRB.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("RRB.outputFolderPath");
        int numTests = Integer.parseInt(PropertiesManager.getProperty("RRB.numTests", Constants.NUM_RUN_DEFAULT));
        int t = Integer.parseInt(PropertiesManager.getProperty("RRB.t"));
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("RRB.isolatedVertices"));
        boolean transpose = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("RRB.transpose"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("RRB.inMemory", Constants.FALSE));
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("RRB.suggestedNumberOfThreads", Constants.NUM_THREAD_DEFAULT));
        Parameter param = new Parameter.Builder()
                .setAlgorithmName("RRB")
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(t)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setIsolatedVertices(isolatedVertices)
                .setNumThreads(suggestedNumberOfThreads)
                .build();
        logger.info("\n\n********************** Parameters **********************\n\n" +
                        "# executions will be run {} time(s)\n" +
                        "ready to start algorithm: {}\n" +
                        "on graph (transpose version? {}) read from: {}\n" +
                        "loading graph in memory? {}\n" +
                        "keep isolated nodes? {}\n" +
                        "results will written in: {}\n" +
                        "t {}\n" +
                        "number of threads: {}\n" +
                        "\n********************************************************\n\n",
                param.getNumTests(),
                param.getAlgorithmName(),
                param.isTranspose(), param.getInputFilePathGraph(),
                param.isInMemory(),
                param.keepIsolatedVertices(),
                param.getOutputFolderPath(),
                param.getNumSeeds(),
                param.getNumThreads());

        RandomRadiusBall rrb = new RandomRadiusBall(param);
        try{
            List<Measure> measures = rrb.run();
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

    public List<Measure> run() throws IOException {
        Measure measure;
        int numTest = mParam.getNumTests();

        long startTime = System.currentTimeMillis();
        long totalTime;

        //List<SeedNode> seedsNodes = new ArrayList<>();
        List<Measure> measures = new ArrayList<>();


        for (int i = 0; i < numTest; i++) {
            measure = runRBB();
            measure.setAlgorithmName(mParam.getAlgorithmName());
            measure.setRun(i + 1);
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

    public Measure runRBB(){
        long startTime = System.currentTimeMillis();
        long totalTime,logTime,nodeStartTime;
        long lastLogTime = startTime;
        int i,n,tau,h;
        n = mGraph.numNodes();
        double[] dist = new double[n];
        int [] random_ball_size = new int[n];
        float [] centrality = new float[mGraph.numNodes()];
        double r;


        for (i = 0; i <n; i++) {
            nodeStartTime = System.currentTimeMillis();

            r = Math.random();
            tau = (int) Math.floor(t / r);
            random_ball_size[i] = tau;
            //BFS of depth tau from i
            Arrays.fill(dist, -1);
            Queue<Integer> ball = new LinkedList<>();
            ball.add(i);
            dist[i] = 0;
            h = 0;
            while (!ball.isEmpty() && h < tau) {
                int w = ball.remove();
                final int d = mGraph.outdegree(w);
                final int[] successors = mGraph.successorArray(w);
                for (int l = 0; l < d; l++) {
                    if (dist[successors[l]] == -1) {
                        dist[successors[l]] = dist[w] + 1;
                        centrality[successors[l]] += 1;
                        ball.add(successors[l]);
                    }
                }


                h += 1;

            }
            logTime = System.currentTimeMillis();
            if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                logger.info("# nodes analyzed {} / {}  [elapsed {}, node/s {}]",
                        i, mGraph.numNodes(),

                        (logTime - nodeStartTime) / (double) 1000,
                        ((n + 1) / ((logTime - nodeStartTime) / (double) 1000)));
                lastLogTime = logTime;
            }
        }
        // Normalizing the estimator the original estimator is centrality[i] /t
        for (i = 0; i <n; i++) {
            centrality[i] = centrality[i] /(t*(n-1));
        }

        float avg_ball_size = 0;
        double std_ball_size = 0;


        for (i=0 ; i<n;i++){
            avg_ball_size += random_ball_size[i];
        }
        avg_ball_size = avg_ball_size /n;
        for (i = 0; i<n;i++){
            std_ball_size += (random_ball_size[i]-avg_ball_size) * (random_ball_size[i]-avg_ball_size);
        }
        std_ball_size = Math.sqrt(std_ball_size) /n;

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);
        logger.info("Average random ball size {} standard deviation {}", avg_ball_size,std_ball_size);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setTime(totalTime);
        graphMeasure.setHarmonicCentrality(centrality);

        return graphMeasure;
    }
}
