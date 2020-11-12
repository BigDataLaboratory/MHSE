package it.bigdatalab.algorithm;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a new instance of ImmutableGraph based on an input ImmutableGraph graph
 * without isolated nodes, using indegree and outdegree of each node
 * to find and remove isolated nodes.
 */
public class Preprocessing {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.Preprocessing");


    /**
     * Empty constructor
     */
    Preprocessing() {

    }

    /**
     * Remove isolated nodes, if any, from an input ImmutableGraph graph
     *
     * @return a new instance of ImmutableGraph without isolated nodes, if any
     */
    public ImmutableGraph removeIsolatedNodes(ImmutableGraph graph) {
        logger.info("Deleting isolated nodes...");

        int numNodes = graph.numNodes();
        int d; // counter for outdegree iterator
        int s; // counter for indegree iterator
        boolean isBijective = true;

        NodeIterator nodeIterator = graph.nodeIterator();
        int[] indegree = new int[numNodes];
        int[] outdegree = new int[numNodes];
        int[] mappedGraph = new int[numNodes];

        // compute nodes indegree
        while (numNodes-- != 0) {
            int vertex = nodeIterator.nextInt();
            d = nodeIterator.outdegree();
            outdegree[vertex] = d;
            int[] neighbours = nodeIterator.successorArray();

            s = d;
            while (s-- != 0) {
                ++indegree[neighbours[s]];
            }
        }

        // for each node, if a node is isolated (indegree and outdegree are 0)
        // set -1 value for the node in the mapped graph array associated to the input graph
        d = 0;
        for (s = 0; s < indegree.length; s++) {
            if ((indegree[s] == 0) && (outdegree[s] == 0)) {
                mappedGraph[s] = -1;
                if (isBijective) {
                    isBijective = false;
                }
            } else {
                mappedGraph[s] = d;
                d += 1;
            }
        }

        if (!isBijective) {
            graph = Transform.map(graph, mappedGraph);
            // new # number of nodes of the input graph without isolated nodes
            logger.info("Removed {} nodes ", numNodes - graph.numNodes());
            logger.info("The graph has {} nodes",graph.numNodes());
        } else {
            logger.info("The graph does not contain isolated vertices");
        }

        return graph;
    }
}
