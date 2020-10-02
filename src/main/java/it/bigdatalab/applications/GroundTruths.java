package it.bigdatalab.applications;

import it.bigdatalab.algorithm.MinHash;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.algo.ParallelBreadthFirstVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GroundTruths {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHash");
    private String inputFilePath;
    private String outputFolderPath;
    private ImmutableGraph mGraph;
    private ProgressLogger pl;

    public GroundTruths() throws IOException {

        initialize();

    }

    private void initialize() throws IOException{
        inputFilePath = PropertiesManager.getProperty("groundTruth.inputFilePath");
        outputFolderPath = PropertiesManager.getProperty("groundTruth.outputFolderPath");
        logger.info("Loading graph at filepath {}", inputFilePath);
        mGraph = ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

    }

    private void run() throws IOException{
        long startTime;
        long endTime;
        long totalTime;

        ParallelBreadthFirstVisit bfs = new ParallelBreadthFirstVisit(mGraph,4,false,pl);

    }

}