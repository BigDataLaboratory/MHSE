package it.bigdatalab.utils;

import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GraphUtils {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.utils.GraphUtils");

    /**
     * Load an input graph in WebGraph format
     *
     * @return an ImmutableGraph instance
     */
    public static GraphManager loadGraph(String inputFilePath, boolean inMemory, boolean isolatedVertices,boolean webGraph, boolean compGraph,boolean transpose, String direction,boolean GroupVarint) throws IOException {
        logger.info("Loading graph at filepath {} (in memory: {})", inputFilePath, inMemory);

            GraphManager graph = new GraphManager(webGraph,compGraph,inputFilePath,transpose,inMemory,isolatedVertices,direction,GroupVarint);
        //String[] SplitInputFilePath = inputFilePath.split(".");

        //CompressedGraph graph = new CompressedGraph( inputFilePath,SplitInputFilePath[0]+"_offset.txt" ,true);
//        ImmutableGraph graph = inMemory ?
//                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
//                ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

        // check if it must remove isolated nodes
//        if (!isolatedVertices)
//            graph = Preprocessing.removeIsolatedNodes(graph);
//
//        logger.info("\n\n********************** Graph Info **********************\n" +
//                        "# nodes:\t{}\n" +
//                        "********************************************************\n\n",
//                graph.numNodes());

        return graph;
    }


    public static GraphManager loadGraph(String inputFilePath,String offset, boolean inMemory, boolean isolatedVertices,boolean webGraph, boolean compGraph,boolean transpose, String direction,boolean GroupVarint) throws IOException {
        logger.info("Loading graph at filepath {} (in memory: {})", inputFilePath, inMemory);
        //String[] SplitInputFilePath = inputFilePath.split(".");
        //CompressedGraph graph = new CompressedGraph(inputFilePath,SplitInputFilePath[0]+"_offset.txt",true);
        GraphManager graph = new GraphManager(webGraph,compGraph,inputFilePath,transpose,inMemory,isolatedVertices,direction,GroupVarint);

//        ImmutableGraph graph = inMemory ?
//                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
//                ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

        // check if it must remove isolated nodes
//        if (!isolatedVertices) {
//            graph = Preprocessing.removeIsolatedNodes(graph);
//        }

        // transpose graph based on direction selected
        // and graph type loaded (original or transposed)
//        if (transpose) {
//            if (direction.equals(Constants.IN_DIRECTION)) {
//                logger.info("Transposing graph cause direction is {}", direction);
//                graph = Transform.transpose(graph);
//                logger.debug("Transposing graph ended");
//            }
//        } else {
//            if (direction.equals(Constants.OUT_DIRECTION)) {
//                logger.info("Transposing graph cause direction is {}", direction);
//                graph = Transform.transpose(graph);
//                logger.debug("Transposing graph ended");
//            }
//        }

/*todo
        logger.info("\n\n********************** Graph Info **********************\n" +
                        "# nodes:\t{}\n" +
                        "# edges:\t{}\n" +
                        "********************************************************\n\n",
                graph.numNodes(), graph.numArcs());
*/

        return graph;
    }


}
