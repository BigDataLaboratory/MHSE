package it.misebigdatalab.applications;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.misebigdatalab.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class WebGraph2EdgeList {

    private String outputFolderPath;
    private String inputFilePath;

    private static final Logger logger = LoggerFactory.getLogger("it.misebigdatalab.applications.WebGraph2EdgeList");

    public WebGraph2EdgeList() {
        initialize();
    }

    /**
     * Read from properties file
     * - graph's input file path in Webgraph format
     * - outfolder path where persist the graph in the edgelist format
     */
    private void initialize() {
        //To convert a graph from Webgraph to edgelist format
        this.inputFilePath = PropertiesManager.getProperty("webGraph2EdgeList.inputFilePath");
        this.outputFolderPath = PropertiesManager.getProperty("webGraph2EdgeList.outputFolderPath");
    }

    /**
     * Read graph as ImmutableGraph, for each node get the list of neighbours, for each neighbour append a new edge in the edgelist file.
     * Every pair is divided using the separator read from properties file.
     */
    public void runAlgorithm() {
        ImmutableGraph graph = null;
        try {
            graph = ImmutableGraph.load(inputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String graphName = new File(inputFilePath).getName();
        String edgelistOutputFilePath = outputFolderPath + File.separator + graphName + ".edgelist";


        try (Writer writer = new FileWriter(edgelistOutputFilePath)) {
            // split each pair of nodes using the separator read from properties file
            String eol = System.getProperty("line.separator");

            for (int n = 0; n < graph.numNodes(); n++) { // for each node of the graph
                final int node = n;
                final int d = graph.outdegree(node); // get node's outdegree to loop over its neighbours
                final int[] successors = graph.successorArray(node); // get the node's neighbours
                for (int l = 0; l < d; l++) {
                    final int neighbour = successors[l];
                    writer.append(Integer.toString(node))
                            .append("\t") // used to split the ids of a pair
                            .append(Integer.toString(neighbour))
                            .append(eol);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("# nodes: " + graph.numNodes() + ", # edges: " + graph.numArcs());
    }

    public static void main(String args[]) throws IOException {
        WebGraph2EdgeList t = new WebGraph2EdgeList();
        t.runAlgorithm();
    }

}


