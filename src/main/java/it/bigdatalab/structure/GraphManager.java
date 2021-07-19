package it.bigdatalab.structure;

import it.bigdatalab.utils.Constants;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GraphManager {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.GraphManager");

    private CompressedGraph cGraph;
    private ImmutableGraph mGraph;
    private boolean  webGraph = false;
    private boolean compressedGraph = false;
    private boolean differentialCompression = true;
    private boolean inMemory = true;

    private int [] nodes;

    public GraphManager(boolean WG, boolean CG,String inputFilePath, boolean transpose,String direction) throws IOException {
        int i;

        if(WG){
            webGraph = true;

            ImmutableGraph graph = inMemory ?
                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
                ImmutableGraph.load(inputFilePath);


            if (transpose) {
                if (direction.equals(Constants.IN_DIRECTION)) {
                    logger.info("Transposing graph cause direction is {}", direction);
                    graph = Transform.transpose(graph);
                    logger.debug("Transposing graph ended");
                }
            } else {
                    if (direction.equals(Constants.OUT_DIRECTION)) {
                        logger.info("Transposing graph cause direction is {}", direction);
                        graph = Transform.transpose(graph);
                        logger.debug("Transposing graph ended");
                    }
            }
            mGraph = graph;
            nodes = new int[mGraph.numNodes()];
            // populate the array of nodes
            NodeIterator nodeIter;
            nodeIter = mGraph.nodeIterator();
            i = 0;
            while(nodeIter.hasNext()) {
                nodes[i] = nodeIter.nextInt();
                i++;
            }



        }
        if(CG){
            compressedGraph = true;
            // Must manage the transpose loading!
            String[] SplitInputFilePath = inputFilePath.split("[.]");

            cGraph = new CompressedGraph(inputFilePath,SplitInputFilePath[0]+"."+SplitInputFilePath[1]+"_offset.txt",true);
            nodes = cGraph.get_nodes();
        }

    }
    public int[] get_neighbours(int node){
        if(webGraph){
            return mGraph.successorArray(node);
        }
        return cGraph.get_neighbours(node,differentialCompression);
    }

    public int get_degree(int node){
        if(webGraph){
            return mGraph.outdegree(node);
        }
        return cGraph.get_neighbours(node,differentialCompression).length;
    }

    public int []get_nodes(){
        return nodes;
    }

    public int numNodes(){
        return nodes.length;
    }
    public int numArcs(){
        if(webGraph){
            mGraph.numArcs();
        }
        return cGraph.numArcs();
    }







}
