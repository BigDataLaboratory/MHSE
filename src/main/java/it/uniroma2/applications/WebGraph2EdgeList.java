package it.uniroma2.applications;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.uniroma2.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class WebGraph2EdgeList {

    private String outputFolderPath;
    private String inputFilePath;

    private static final Logger logger = LoggerFactory.getLogger("it.uniroma2.applications.WebGraph2EdgeList");

    public WebGraph2EdgeList() {
        initialize();
    }

    private void initialize() {
        //Se si vuole trasformare da edgelist a unimi format e salvarlo su disco
        this.inputFilePath = PropertiesManager.getProperty("webGraph2EdgeList.inputFilePath");
        this.outputFolderPath = PropertiesManager.getProperty("webGraph2EdgeList.outputFolderPath");
    }

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
            String eol = System.getProperty("line.separator");

            for (int n = 0; n < graph.numNodes(); n++) {
                final int node = n;
                final int d = graph.outdegree(node);
                final int[] successors = graph.successorArray(node);
                for (int l = 0; l < d; l++) {
                    final int neighbour = successors[l];
                    writer.append(Integer.toString(node))
                            .append("\t")
                            .append(Integer.toString(neighbour))
                            .append(eol);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Numero nodi: " + graph.numNodes() + ", numero archi: " + graph.numArcs());
    }

    public static void main(String args[]) throws IOException {
        WebGraph2EdgeList t = new WebGraph2EdgeList();
        t.runAlgorithm();
    }

}


