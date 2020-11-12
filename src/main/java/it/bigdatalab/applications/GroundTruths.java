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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class GroundTruths {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.GroundTruths");

    private String mMode;
    private String mInputFilePath;
    private String mOutputFolderPath;
    private int mThreadNumber;
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
        mThreadNumber = Integer.parseInt(PropertiesManager.getProperty("goundTruth.threadNumber"));
        logger.info("Number of Threads " + mThreadNumber);
        mMode = PropertiesManager.getProperty("groundTruth.mode");

        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("minhash.isolatedVertices"));
        logger.info("Keep the isolated vertices? {}", isolatedVertices);

        if (!isolatedVertices) {
            Preprocessing preprocessing = new Preprocessing();
            mGraph = preprocessing.removeIsolatedNodes(mGraph);
        }
    }

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

        neighFunction = NeighbourhoodFunction.compute(mGraph, mThreadNumber, pl);
        avgDistance = NeighbourhoodFunction.averageDistance(neighFunction);
        diameter = NeighbourhoodFunction.effectiveDiameter(1, neighFunction);
        effectiveDiameter = NeighbourhoodFunction.effectiveDiameter(0.9, neighFunction);
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
        String path = mOutputFolderPath + "/gt_" + graphFileName + ".json";
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