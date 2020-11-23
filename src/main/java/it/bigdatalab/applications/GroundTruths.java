package it.bigdatalab.applications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.bigdatalab.model.GraphGtMeasure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.Preprocessing;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import it.unimi.dsi.webgraph.algo.NeighbourhoodFunction;
import it.unimi.dsi.webgraph.algo.ParallelBreadthFirstVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;


public class GroundTruths {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.GroundTruths");

    private final String mMode;
    private final Parameter mParam;

    public GroundTruths() {
        String inputFilePath = PropertiesManager.getProperty("groundTruth.inputFilePath");
        String outputFolderPath = PropertiesManager.getProperty("groundTruth.outputFilePath");
        int threadNumber = Integer.parseInt(PropertiesManager.getProperty("groundTruth.threadNumber"));
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("groundTruth.isolatedVertices"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("groundTruth.inMemory"));
        mMode = PropertiesManager.getProperty("groundTruth.mode");

        mParam = new Parameter.Builder()
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumThreads(threadNumber)
                .setInMemory(inMemory)
                .setIsolatedVertices(isolatedVertices).build();
    }

    public static void main(String[] args) throws IOException {
        GroundTruths groundTruths = new GroundTruths();
        groundTruths.computeGroundTruth();
    }

    private void computeGroundTruth() throws IOException {
        ImmutableGraph g = loadGraph(mParam.getInputFilePathGraph(), mParam.isInMemory(), mParam.keepIsolatedVertices());

        if (getMode().equals("WebGraph"))
            writeResults(runWebGraphMode(g));
        else if (getMode().equals("BFS"))
            writeResults(runBFSMode(g));
    }

    /**
     * Execution of the Ground Truth algorithm performing
     * N times a Breadth-first Visit
     *
     * @return Graph Measures
     */
    private GraphGtMeasure runBFSMode(ImmutableGraph g) {
        long startTime = System.currentTimeMillis();
        ProgressLogger pl = new ProgressLogger();

        long visitedNodes = 0;
        double max = 0;
        double avgDistance = 0;
        ParallelBreadthFirstVisit bfs = new ParallelBreadthFirstVisit(g, mParam.getNumThreads(), false, pl);

        // n times BFS
        NodeIterator nodeIterator = g.nodeIterator();
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
                d+=1;
                a += 1;
                b += 1;
            }
        }

        // avgDistance is the sum distances
        double totalAvgDistance = (avgDistance / ((double) g.numNodes() * ((double) (g.numNodes() - 1))));

        long endTime = System.currentTimeMillis();

        GraphGtMeasure gtMeasure = new GraphGtMeasure(
                g.numNodes(), g.numArcs(), totalAvgDistance, 0, max, visitedNodes);

        logger.info("\n\n********************* Stats ****************************\n\n" +
                        "Total Couples Reachable\t{}\n" +
                        "Avg Distance\t{}\n" +
                        "Diameter\t{}\n" +
                        "\n********************************************************\n\n",
                BigDecimal.valueOf(gtMeasure.getTotalCouples()).toPlainString(),
                gtMeasure.getAvgDistance(),
                gtMeasure.getEffectiveDiameter());

        logger.info("Time elapsed {}", endTime - startTime);
        return gtMeasure;
    }

    /**
     * Execution of the Ground Truth algorithm using the
     * NeighbourhoodFunction by WebGraph
     *
     * @return Graph Measures
     */
    private GraphGtMeasure runWebGraphMode(ImmutableGraph g) {
        long startTime = System.currentTimeMillis();

        double visitedNodes;
        double avgDistance;
        double[] neighFunction;
        double diameter;
        double effectiveDiameter;

        ProgressLogger pl = new ProgressLogger();

        // Defining a new neighbourhood function f(.)
        // This function is obtained by executing N breadth first visits.
        neighFunction = NeighbourhoodFunction.compute(g, mParam.getNumThreads(), pl);
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
                g.numNodes(), g.numArcs(), avgDistance, effectiveDiameter, diameter, visitedNodes);

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

    private String getMode() {
        return mMode;
    }

    private ImmutableGraph loadGraph(String inputFilePath, boolean inMemory, boolean isolatedVertices) throws IOException {
        logger.info("Loading graph at filepath {} (in memory: {})", inputFilePath, inMemory);
        ImmutableGraph graph = inMemory ?
                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
                ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

        // check if it must remove isolated nodes
        if (!isolatedVertices)
            graph = Preprocessing.removeIsolatedNodes(graph);

        logger.info("\n\n********************** Graph Info **********************\n" +
                        "# nodes:\t{}\n" +
                        "# edges:\t{}\n" +
                        "********************************************************\n\n",
                graph.numNodes(), graph.numArcs());

        return graph;
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
}