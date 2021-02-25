package it.bigdatalab.utils;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Create a new instance of ImmutableGraph based on an input ImmutableGraph graph
 * without isolated nodes, using indegree and outdegree of each node
 * to find and remove isolated nodes.
 */
public class Preprocessing {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.utils.Preprocessing");


    public ImmutableGraph reorderGraphByOutDegree(final ImmutableGraph graph) {
        logger.info("Start reordering graph noded by out degree");
        ImmutableGraph g = graph;

        int numNodes = g.numNodes();
        int d; // counter for outdegree iterator

        NodeIterator nodeIterator = g.nodeIterator();
        Integer[] outdegree = new Integer[numNodes];
        int[] mappedGraph = new int[numNodes];

        // compute nodes indegree
        while (numNodes-- != 0) {
            int vertex = nodeIterator.nextInt();
            d = nodeIterator.outdegree();
            outdegree[vertex] = d;
        }

        IndexSorter<Integer> is = new IndexSorter<>(outdegree);
        is.sort();

        int newId = 0;
        Integer[] indexes = is.getIndexes();

        int i = 0;
        int j = indexes.length - 1;
        //logger.debug("i {} j {} indexes[i] {} indexes[j] {}", i, j, indexes[i],indexes[j]);

        while (j >= i) {
            if (j == i) {
                mappedGraph[indexes[j]] = newId;
            } else {
                //logger.debug("i {} j {} indexes[j] {}, new id = {}", i, j, indexes[j], newId);
                mappedGraph[indexes[j]] = newId;
                newId = newId + 1;
                //logger.debug("i {} j {} indexes[j] {}, new id = {}", i, j, indexes[i], newId);
                mappedGraph[indexes[i]] = newId;
                newId = newId + 1;
                i++;
                j--;
            }
        }

        //4 0-4 1-3 2-2


        g = Transform.map(g, mappedGraph);
        return g;
    }

    /**
     * Remove isolated nodes, if any, from an input ImmutableGraph graph
     *
     * @return a new instance of ImmutableGraph without isolated nodes, if any
     */
    public static ImmutableGraph removeIsolatedNodes(final ImmutableGraph graph) {
        logger.info("Start removing isolated nodes from graph");
        ImmutableGraph g = graph;

        int numNodes = g.numNodes();
        int d; // counter for outdegree iterator
        int s; // counter for indegree iterator
        boolean isBijective = true;

        NodeIterator nodeIterator = g.nodeIterator();
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
            g = Transform.map(g, mappedGraph);
            // new # number of nodes of the input graph without isolated nodes
            logger.info("Removed # {} nodes from graph", g.numNodes() - numNodes);
        } else {
            logger.info("The graph does not contain isolated vertices");
        }

        return g;
    }

    class IndexSorter<T extends Comparable<T>> implements Comparator<Integer> {

        private final T[] values;
        private final Integer[] indexes;

        public IndexSorter(T[] d) {
            this.values = d;
            indexes = new Integer[this.values.length];
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = i;
            }
        }

        /**
         * Sorts the underlying index array based upon the values provided in the constructor. The underlying value array is not sorted.
         */
        public void sort() {
            Arrays.sort(indexes, this);
        }

        /**
         * Retrieves the indexes of the array. The returned array is sorted if this object has been sorted.
         *
         * @return The array of indexes.
         */
        public Integer[] getIndexes() {
            return indexes;
        }

        /**
         * Compares the two values at index arg0 and arg0
         *
         * @param arg0 The first index
         * @param arg1 The second index
         * @return The result of calling compareTo on T objects at position arg0 and arg1
         */
        @Override
        public int compare(Integer arg0, Integer arg1) {
            T d1 = values[arg0];
            T d2 = values[arg1];
            return d1.compareTo(d2);
        }
    }
}
