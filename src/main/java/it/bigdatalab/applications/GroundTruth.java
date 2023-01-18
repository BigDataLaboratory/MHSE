package it.bigdatalab.applications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.bigdatalab.model.GraphGtMeasure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.GraphUtils;
import it.bigdatalab.utils.GsonHelper;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.algo.NeighbourhoodFunction;
import it.unimi.dsi.webgraph.algo.ParallelBreadthFirstVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;


public class GroundTruth {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.GroundTruths");

    private final String mMode;
    private final Parameter mParam;
    private final ImmutableGraph mGraph;

    public GroundTruth(ImmutableGraph g, Parameter param) {
        this.mGraph = g;
        this.mParam = param;
        this.mMode = Constants.DEFAULT_MODE;
    }

    public GroundTruth(ImmutableGraph g, Parameter param, String mode) {
        this.mGraph = g;
        this.mParam = param;
        this.mMode = mode;
    }

    public static void main(String[] args) throws IOException {
        String inputFilePath = PropertiesManager.getProperty("groundTruth.inputFilePath");
        String outputFolderPath = PropertiesManager.getProperty("groundTruth.outputFilePath");
        int threadNumber = Integer.parseInt(PropertiesManager.getProperty("groundTruth.threadNumber"));
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("groundTruth.isolatedVertices"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("groundTruth.inMemory"));
        String mode = PropertiesManager.getProperty("groundTruth.mode");

        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumThreads(threadNumber)
                .setInMemory(inMemory)
                .setIsolatedVertices(isolatedVertices).build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth groundTruth = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = groundTruth.computeGroundTruth();

        String path = param.getOutputFolderPath() +
                File.separator +
                Constants.GT +
                Paths.get(param.getInputFilePathGraph()).getFileName().toString() +
                Constants.NAMESEPARATOR +
                (param.keepIsolatedVertices() ? Constants.WITHISOLATED : Constants.WITHOUTISOLATED) +
                Constants.JSON_EXTENSION;

        GsonHelper.toJson(graphMeasure, path);
    }

    public GraphGtMeasure computeGroundTruth() {

        if (getMode().equals(Constants.WEBGRAPH))
            return runWebGraphMode();
        else if (getMode().equals(Constants.BFS))
            return runBFSMode();
        else
            throw new IllegalArgumentException("You must choose between WebGraph or BFS mode, no other alternatives");
    }

    /**
     * Execution of the Ground Truth algorithm performing
     * N times a Breadth-first Visit
     *
     * @return Graph Measures
     */
    private GraphGtMeasure runBFSMode() {
        long startTime = System.currentTimeMillis();
        ProgressLogger pl = new ProgressLogger();

        long visitedNodes = 0;
        double max = 0;
        double avgDistance = 0;
        ParallelBreadthFirstVisit bfs = new ParallelBreadthFirstVisit(mGraph, mParam.getNumThreads(), false, pl);

        // n times BFS
        NodeIterator nodeIterator = mGraph.nodeIterator();
        while(nodeIterator.hasNext()){
            int vertex = nodeIterator.nextInt();
            bfs.clear();
            int nodeNumber = bfs.visit(vertex);
            visitedNodes += nodeNumber;

            if(max < bfs.maxDistance()){
                max = bfs.maxDistance();
            }

            // d start from 0 because the first element of the queue is the root node
            // that is node at 0 distance
            // check this link:
            // http://webgraph.di.unimi.it/docs-big/it/unimi/dsi/big/webgraph/algo/ParallelBreadthFirstVisit.html#marker
            int d = 0;
            int a = 0;
            int b = 1;
            while(b<bfs.cutPoints.size()) {
                for (int q = bfs.cutPoints.getInt(a); q < bfs.cutPoints.getInt(b); q++) {
                    avgDistance += d;
                }
                d +=1;
                a += 1;
                b += 1;
            }
        }

        // avgDistance is the sum distances
        double totalAvgDistance = (avgDistance / ((double) mGraph.numNodes() * ((double) (mGraph.numNodes() - 1))));

        long endTime = System.currentTimeMillis();

        GraphGtMeasure gtMeasure = new GraphGtMeasure(
                mGraph.numNodes(), mGraph.numArcs(), totalAvgDistance, 0, max, visitedNodes);

        logger.info("\n\n********************* Stats ****************************\n\n" +
                        "Total Couples Reachable\t{}\n" +
                        "Avg Distance\t{}\n" +
                        "Diameter\t{}\n" +
                        "\n********************************************************\n\n",
                BigDecimal.valueOf(gtMeasure.getTotalCouples()).toPlainString(),
                gtMeasure.getAvgDistance(),
                gtMeasure.getDiameter());

        logger.info("Time elapsed {}", endTime - startTime);
        return gtMeasure;
    }

    /**
     * Execution of the Ground Truth algorithm using the
     * NeighbourhoodFunction by WebGraph
     *
     * @return Graph Measures
     */
    private GraphGtMeasure runWebGraphMode() {
        long startTime = System.currentTimeMillis();

        double visitedNodes;
        double avgDistance;
        double[] neighFunction;
        double diameter;
        double effectiveDiameter;

        ProgressLogger pl = new ProgressLogger();

        // Defining a new neighbourhood function f(.)
        // This function is obtained by executing N breadth first visits.
        neighFunction = NeighbourhoodFunction.compute(mGraph, mParam.getNumThreads(), pl);
        // Get the average distance using the NeighbourhoodFunction
        avgDistance = NeighbourhoodFunction.averageDistance(neighFunction);
        // Get the diameter lowerbound using the function of effective diameter with alpha = 1
        diameter = NeighbourhoodFunction.effectiveDiameter(1, neighFunction);
        // Get the 90% of the diameter using alpha = 0.9
        effectiveDiameter = NeighbourhoodFunction.effectiveDiameter(0.9, neighFunction);
        // Get the number of reachable pairs
        visitedNodes = neighFunction[neighFunction.length - 1];

        logger.info("Avg distance {}, Diameter {}, 90% Effective Diameter {}, Reachable Pairs {}",
                avgDistance, diameter, effectiveDiameter, visitedNodes);

        GraphGtMeasure gtMeasure = new GraphGtMeasure(
                mGraph.numNodes(), mGraph.numArcs(), avgDistance, effectiveDiameter, diameter, visitedNodes);

        logger.info("\n\n********************* Stats ****************************\n\n" +
                        "Total couples Reachable\t{}\n" +
                        "Avg Distance\t{}\n" +
                        "Diameter\t{}\n" +
                        "Effective Diameter\t{}\n" +
                        "\n********************************************************\n\n",
                BigDecimal.valueOf(gtMeasure.getTotalCouples()).toPlainString(),
                gtMeasure.getAvgDistance(),
                gtMeasure.getDiameter(),
                gtMeasure.getEffectiveDiameter());

        long endTime = System.currentTimeMillis();
        logger.info("Time elapsed {}", endTime - startTime);

        return gtMeasure;
    }


    private void writeResults(GraphGtMeasure gtMeasure) {
        String graphFileName = Paths.get(mParam.getInputFilePathGraph()).getFileName().toString();
        String path = mParam.getOutputFolderPath() +
                File.separator +
                "gt_" +
                graphFileName +
                "_" +
                (mParam.keepIsolatedVertices() ? "with_iso" : "without_iso") +
                ".json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(gtMeasure, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMode() {
        return mMode;
    }
}