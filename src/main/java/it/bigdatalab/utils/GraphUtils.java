package it.bigdatalab.utils;

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
    public static ImmutableGraph loadGraph(String inputFilePath, boolean inMemory, boolean isolatedVertices) throws IOException {
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
                        "********************************************************\n\n",
                graph.numNodes());

        return graph;
    }


    public static ImmutableGraph loadGraph(String inputFilePath, boolean transpose, boolean inMemory, boolean isolatedVertices, String direction) throws IOException {
        logger.info("Loading graph at filepath {} (in memory: {})", inputFilePath, inMemory);
        ImmutableGraph graph = inMemory ?
                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
                ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

        // check if it must remove isolated nodes
        if (!isolatedVertices) {
            graph = Preprocessing.removeIsolatedNodes(graph);
        }

        // transpose graph based on direction selected
        // and graph type loaded (original or transposed)
        if (transpose) {
            if (direction.equals(Constants.IN_DIRECTION)) {
                logger.info("Transposing graph cause direction is {}", direction);
                graph = Transform.transpose(graph);
                logger.info("Transposing graph ended");
            }
        } else {
            if (direction.equals(Constants.OUT_DIRECTION)) {
                logger.info("Transposing graph cause direction is {}", direction);
                graph = Transform.transpose(graph);
                logger.info("Transposing graph ended");
            }
        }

/*todo
        logger.info("\n\n********************** Graph Info **********************\n" +
                        "# nodes:\t{}\n" +
                        "# edges:\t{}\n" +
                        "********************************************************\n\n",
                graph.numNodes(), graph.numArcs());
*/

        return graph;
    }

    public static ImmutableGraph loadGraph(String inputFilePath, boolean transpose, boolean inMemory, boolean isolatedVertices, String direction, boolean reordering) throws IOException {
        logger.info("Loading graph at filepath {} (in memory: {})", inputFilePath, inMemory);
        ImmutableGraph graph = inMemory ?
                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
                ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");

        // check if it must remove isolated nodes
        if (!isolatedVertices) {
            graph = Preprocessing.removeIsolatedNodes(graph);
        }

        // transpose graph based on direction selected
        // and graph type loaded (original or transposed)
        if (transpose) {
            if (direction.equals(Constants.IN_DIRECTION)) {
                logger.info("Transposing graph cause direction is {}", direction);
                graph = Transform.transpose(graph);
                logger.info("Transposing graph ended");
            }
        } else {
            if (direction.equals(Constants.OUT_DIRECTION)) {
                logger.info("Transposing graph cause direction is {}", direction);
                graph = Transform.transpose(graph);
                logger.info("Transposing graph ended");
            }
        }

        if (reordering) {
            Preprocessing p = new Preprocessing();
            graph = p.reorderGraphByOutDegree(graph);
        }


        logger.info("\n\n********************** Graph Info **********************\n" +
                        "# nodes:\t{}\n" +
                        "********************************************************\n\n",
                graph.numNodes());


        return graph;
    }
}
