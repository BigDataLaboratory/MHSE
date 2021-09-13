package it.bigdatalab.applications;

import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

public class WebGraph2AdjcencyList {

    private String outputFolderPath;
    private String inputFilePath;

    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.WebGraph2AdjcencyList");

    public WebGraph2AdjcencyList() {
        initialize();
    }

    /**
     * Read from properties file
     * - graph's input file path in Webgraph format
     * - outfolder path where persist the graph in the edgelist format
     */
    private void initialize() {
        //To convert a graph from Webgraph to edgelist format
        this.inputFilePath = PropertiesManager.getProperty("WebGraph2AdjcencyList.inputFilePath");
        this.outputFolderPath = PropertiesManager.getProperty("WebGraph2AdjcencyList.outputFolderPath");
    }

    /**
     * Read graph as ImmutableGraph, for each node get the list of neighbours, for each neighbour append a new edge in the edgelist file.
     * Every pair is divided using the separator read from properties file.
     */
    public void runAlgorithm() {
        ImmutableGraph graph = null;
        Long2ObjectLinkedOpenHashMap<LongArrayList> normalizedAdjList = new Long2ObjectLinkedOpenHashMap<LongArrayList>();

        try {
            graph = ImmutableGraph.load(inputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String graphName = new File(inputFilePath).getName();
        String adjListOutputFilePath = outputFolderPath + File.separator + graphName + ".adjlist";


        try (Writer writer = new FileWriter(adjListOutputFilePath)) {
            // split each pair of nodes using the separator read from properties file
            //String eol = System.getProperty("line.separator");
            String eol = "\n";
            for (int n = 0; n < graph.numNodes(); n++) { // for each node of the graph
                if(!normalizedAdjList.containsKey(n)){
                    LongArrayList adjacencyList = new LongArrayList();
                    adjacencyList.add(n);
                    normalizedAdjList.put(n, adjacencyList);
                }
                final int node = n;
                final int d = graph.outdegree(node); // get node's outdegree to loop over its neighbours
                final int[] successors = graph.successorArray(node); // get the node's neighbours
                LongArrayList adjacencyList = new LongArrayList();

                for (int l = 0; l < d; l++) {

                    final int neighbour = successors[l];

                    adjacencyList.add(neighbour);
                    if(!normalizedAdjList.containsKey(neighbour)){
                        LongArrayList adjacencyListNeig = new LongArrayList();
                        adjacencyListNeig.add(neighbour);
                        normalizedAdjList.put(neighbour, adjacencyListNeig);
                    }



                }
                Collections.sort(adjacencyList);
                normalizedAdjList.put(n,adjacencyList);
            }

            for(int n= 0; n<normalizedAdjList.size();n++){
                String line;
                line= Integer.toString(n) + "\t";
                LongArrayList adjacencyList = normalizedAdjList.get(n);
                for(int j = 0;j<adjacencyList.size();j++){
                    line+=Long.toString(adjacencyList.getLong(j));
                    if(j<adjacencyList.size()-1) {
                        line+="\t";
                    }
                }
                line+=eol;
                writer.append(line);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("# nodes: " + graph.numNodes() + ", # edges: " + graph.numArcs());
    }

    public static void main(String args[]) throws IOException {
        WebGraph2AdjcencyList t = new WebGraph2AdjcencyList();
        t.runAlgorithm();
    }

}
