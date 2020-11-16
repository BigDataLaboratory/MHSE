package it.bigdatalab.applications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.bigdatalab.algorithm.Preprocessing;
import it.bigdatalab.model.GraphGtMeasure;
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
import java.nio.file.Paths;



public class GroundTruths {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.GroundTruths");

    private String mMode;
    private String mInputFilePath;
    private String mOutputFolderPath;
    private int mThreadNumber;
    private boolean mIsolatedVertices;
    private ImmutableGraph mGraph;

    public GroundTruths() throws IOException {
        initialize();
    }

    public static void main(String args[]) throws IOException {
        GroundTruths groundTruths = new GroundTruths();

        if (groundTruths.getMode().equals("WebGraph"))
            groundTruths.writeResults(groundTruths.runWebGraphMode());
        else
            groundTruths.writeResults(groundTruths.runBFSMode());
    }

    private void initialize() throws IOException{
        mInputFilePath = PropertiesManager.getProperty("groundTruth.inputFilePath");
        mOutputFolderPath = PropertiesManager.getProperty("groundTruth.outputFilePath");
        logger.info("Loading graph at filepath {}", mInputFilePath);
        mGraph = ImmutableGraph.load(mInputFilePath);
        logger.info("Loading graph completed successfully");
        mThreadNumber = Integer.parseInt(PropertiesManager.getProperty("groundTruth.threadNumber"));
        logger.info("Number of Threads " + mThreadNumber);
        mMode = PropertiesManager.getProperty("groundTruth.mode");

        mIsolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("groundTruth.isolatedVertices"));
        logger.info("Keep the isolated vertices? {}", mIsolatedVertices);

        if (!mIsolatedVertices) {
            Preprocessing preprocessing = new Preprocessing();
            mGraph = preprocessing.removeIsolatedNodes(mGraph);
        }
    }

    /**
     * Execution of the Ground Truth algorithm performing
     * N times a Breadth-first Visit
     * @return Graph Measures
     */
    private GraphGtMeasure runBFSMode() {
        long startTime = System.currentTimeMillis();
        ProgressLogger pl = new ProgressLogger();

        long visitedNodes = 0;
        double max = 0;
        double avgDistance = 0;
        ParallelBreadthFirstVisit bfs = new ParallelBreadthFirstVisit(mGraph, mThreadNumber, false, pl);

        logger.info("# nodes {}, # edges {}",
                mGraph.numNodes(), mGraph.numArcs());

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
                d+=1;
                a+=1;
                b+=1;
            }

        }

        // avgDistance is the sum distances
        double totalAvgDistance = (avgDistance / ((double) mGraph.numNodes() * ((double) (mGraph.numNodes() - 1))));

        long endTime = System.currentTimeMillis();

        logger.info("Avg distance {}, Diameter {}, Reachable Pairs {}",
                totalAvgDistance, max, visitedNodes);

        GraphGtMeasure gtMeasure = new GraphGtMeasure(
                mGraph.numNodes(), mGraph.numArcs(), totalAvgDistance, 0, max, visitedNodes);

        logger.info("Time elapsed {}", endTime - startTime);
        return gtMeasure;
    }

    /**
     * Execution of the Ground Truth algorithm using the
     * NeighbourhoodFunction by WebGraph
     * @return Graph Measures
     */
    private GraphGtMeasure runWebGraphMode() throws IOException {
        long startTime = System.currentTimeMillis();

        double visitedNodes;
        double avgDistance;
        double[] neighFunction;
        double diameter;
        double effectiveDiameter;

        ProgressLogger pl = new ProgressLogger();

        logger.info("# nodes {}, # edges {}",
                mGraph.numNodes(), mGraph.numArcs());
        // Defining a new neighbourhood function f(.)
        // This function is obtained by executing N breadth first visits.
        neighFunction = NeighbourhoodFunction.compute(mGraph, mThreadNumber, pl);
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

        long endTime = System.currentTimeMillis();
        logger.info("Time elapsed {}", endTime - startTime);

        return gtMeasure;
    }

    private void writeResults(GraphGtMeasure gtMeasure) {
        String graphFileName = Paths.get(mInputFilePath).getFileName().toString();
        String path = mOutputFolderPath +
                File.separator +
                "gt_" +
                graphFileName +
                "_" +
                (mIsolatedVertices ? "with_iso" : "without_iso") +
                ".json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(gtMeasure, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getMode() {
        return mMode;
    }
}