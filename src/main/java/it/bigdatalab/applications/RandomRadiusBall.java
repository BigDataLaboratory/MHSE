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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RandomRadiusBall {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.RandomRadiusBall");
    private final Parameter mParam;
    private final int mNumberOfThreads;
    private final ImmutableGraph mGraph;
    private final float t;
    protected IntArrayList mSeeds;

    private boolean doCentrality;

    private boolean mNormalized;

    public RandomRadiusBall(@NotNull Parameter param) throws IOException {
        this.mParam = param;
        this.mGraph = GraphUtils.loadGraph(param.getInputFilePathGraph(),param.isTranspose(),param.isInMemory(),param.keepIsolatedVertices(),"out");
        this.mNumberOfThreads = getNumberOfMaxThreads(param.getNumThreads());
        //this.mParam = param;
        this.t = param.getTBall();
    }
    // Implementation of the algorithm Random-Radius Ball Method for Estimating Closeness Centrality
    /**
     * Number of max threads to use for the computation
     *
     * @param suggestedNumberOfThreads if not equal to zero return the number of threads
     *                                 passed as parameter, else the number of max threads available
     * @return number of threads to use for the computation
     */
    private static int getNumberOfMaxThreads(int suggestedNumberOfThreads) {
        if (suggestedNumberOfThreads > 0) return suggestedNumberOfThreads;
        return Runtime.getRuntime().availableProcessors();
    }

    public static void main(String[] args) throws IOException {

        String inputFilePath = PropertiesManager.getPropertyIfNotEmpty("RRB.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("RRB.outputFolderPath");
        int numTests = Integer.parseInt(PropertiesManager.getProperty("RRB.numTests", Constants.NUM_RUN_DEFAULT));
        float t = Float.parseFloat(PropertiesManager.getProperty("RRB.t"));
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("RRB.isolatedVertices"));
        boolean transpose = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("RRB.transpose"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("RRB.inMemory", Constants.FALSE));
        int suggestedNumberOfThreads = Integer.parseInt(PropertiesManager.getProperty("RRB.suggestedNumberOfThreads", Constants.NUM_THREAD_DEFAULT));
        Parameter param = new Parameter.Builder()
                .setAlgorithmName("RRB")
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(0)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setIsolatedVertices(isolatedVertices)
                .setNumThreads(suggestedNumberOfThreads)
                .setTBall(t)
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
            measure.setRun(i+1);
            measures.add(measure);
            logger.info("\n\n********************************************************\n\n" +
                    "Test n. {} executed correctly\n\n" +
                    "********************************************************\n\n",i+1);

        }
        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Application successfully completed. Time elapsed (in milliseconds) {}", totalTime);
        return measures;
    }
    //iteration_thread(s,task_id,local_random_ball_size[taskIndex],local_centrality[taskIndex]);
    private void iteration_thread(int s,float t,float [] centrality,int [] ball_size){
        int n = mGraph.numNodes();
        double[] dist = new double[n];
        int tau,h;
        double r;
        r = Math.random();
        tau = (int) Math.floor(t / r);
        //BFS of depth tau from 1
        ball_size[s] += tau;
        Arrays.fill(dist, -1);
        Queue<Integer> ball = new LinkedList<>();
        ball.add(s);
        dist[s] = 0;
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



    }

    public Measure runRBB(){
        long startTime = System.currentTimeMillis();
        long totalTime;
        int i,j,n;
        n = mGraph.numNodes();
        int [][] local_random_ball_size = new int[mNumberOfThreads][n];
        float [][] local_centrality = new float[mNumberOfThreads][n];
        ArrayList<Integer> random_ball_size_array = new ArrayList<Integer>();
        float [] centrality = new float[n];
        double random_ball_size,std_random_ball_size;
        int task_size = (int) Math.ceil((double) n / mNumberOfThreads);
        int d = n / mNumberOfThreads;
        int y = n % mNumberOfThreads;
        int ntasks = (d== 0) ? y:mNumberOfThreads;
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads

        for (int f = 0; f < ntasks; f++) {
            int start = f * task_size;
            int end = Math.min((f + 1) * task_size,  n);
            final int taskRangeStart = start;
            final int taskRangeEnd = end;
            final int taskIndex = f;
            // Here we could change lists with arrays of fixed sizes
            executor.execute(() -> {
                int task_id = 0;
                for (int s = taskRangeStart; s < taskRangeEnd; s++) {
                    iteration_thread(s,t,local_centrality[taskIndex],local_random_ball_size[taskIndex]);
                    task_id +=1;
                }
            });

        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);



        // Normalizing the estimator the original estimator is centrality[i] /t
        random_ball_size = 0.0;
        for (i = 0; i <n; i++) {
            for (j = 0; j< mNumberOfThreads; j++){
                centrality[i] += local_centrality[j][i];
                random_ball_size+=local_random_ball_size[j][i];
                random_ball_size_array.add(local_random_ball_size[j][i]);
            }
            centrality[i] = centrality[i] /(t*(n-1));
        }
        random_ball_size = random_ball_size/n;
        std_random_ball_size = 0.0;
        for (i = 0; i< n; i++){
            std_random_ball_size += (random_ball_size_array.get(i) - random_ball_size) * (random_ball_size_array.get(i) - random_ball_size);
        }
        std_random_ball_size = Math.sqrt(std_random_ball_size/n);
        logger.info("Average Ball Size {} ({})",random_ball_size,std_random_ball_size);



        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setTime(totalTime);
        graphMeasure.setHarmonicCentrality(centrality);
        graphMeasure.setTBall(t);
        graphMeasure.setAvgBallSize(random_ball_size);
        graphMeasure.setStdBallSize(std_random_ball_size);

        return graphMeasure;
    }
}

/*

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
*/