package it.bigdatalab.applications;

import com.opencsv.CSVWriter;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.GraphUtils;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class InOutDegree {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.application.InOutDegree");

    private final Parameter mParam;
    private final ImmutableGraph mGraph;

    public InOutDegree(ImmutableGraph g, Parameter param) {
        this.mGraph = g;
        this.mParam = param;
    }

    public static void main(String[] args) throws IOException {

        String inputFilePath = PropertiesManager.getProperty("inoutdegree.inputFilePath");
        String outputFolderPath = PropertiesManager.getPropertyIfNotEmpty("inoutdegree.outFolderPath");
        boolean isolatedVertices = Boolean.parseBoolean(PropertiesManager.getProperty("inoutdegree.isolatedVertices"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("inoutdegree.inMemory", Constants.FALSE));

        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setInMemory(inMemory)
                .setIsolatedVertices(isolatedVertices).build();

        logger.info("\n\n********************** Parameters **********************\n\n" +
                        "on graph read from: {}\n" +
                        "loading graph in memory? {}\n" +
                        "keep isolated nodes? {}\n" +
                        "results will written in: {}\n" +
                        "\n********************************************************\n\n",
                param.getInputFilePathGraph(),
                param.isInMemory(),
                param.keepIsolatedVertices(),
                param.getOutputFolderPath());

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());
        InOutDegree inOutDegree = new InOutDegree(g, param);

        int[][] inOut = inOutDegree.computeInOutDegree();
        String path = param.getOutputFolderPath() +
                File.separator +
                Constants.INOUTDEGREE +
                Constants.NAMESEPARATOR +
                Paths.get(param.getInputFilePathGraph()).getFileName().toString() +
                Constants.NAMESEPARATOR +
                (param.keepIsolatedVertices() ? Constants.WITHISOLATED : Constants.WITHOUTISOLATED) +
                Constants.CSV_EXTENSION;

        String[] headerRecord = {"node", "indegree", "outdegree"};

        CSVWriter writer = new CSVWriter(new FileWriter(path), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

        writer.writeNext(headerRecord);
        for (int i = 0; i < inOut.length; i++) {
            writer.writeNext(Arrays.stream(inOut[i]).mapToObj(String::valueOf).toArray(String[]::new));
        }
        writer.close();
    }

    private int[][] computeInOutDegree() {
        int numNodes = mGraph.numNodes();

        int[][] inOut = new int[numNodes][3];
        int d; // counter for outdegree iterator
        int s; // counter for indegree iterator

        // compute nodes indegree
        NodeIterator nodeIterator = mGraph.nodeIterator();
        while (numNodes-- != 0) {
            int node = nodeIterator.nextInt();

            inOut[node][Constants.NODE_ID] = node;

            d = nodeIterator.outdegree();
            inOut[node][Constants.OUTDEGREE] = d;

            int[] neighbours = nodeIterator.successorArray();
            s = d;
            while (s-- != 0) {
                ++inOut[neighbours[s]][Constants.INDEGREE];
            }
        }
        return inOut;
    }
}
