package it.bigdatalab.structure;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;

import java.io.IOException;

public class GraphManager {
    private CompressedGraph cGraph;
    private ImmutableGraph mGraph;
    private boolean  webGraph = false;
    private boolean compressedGraph = false;
    private boolean differentialCompression = true;

    private int [] nodes;

    public GraphManager(boolean WG, boolean CG,String inputFilePath) throws IOException {
        int i;

        if(WG){
            webGraph = true;
            // load the graph in mgraph MUST BE DEFINED
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
            String[] SplitInputFilePath = inputFilePath.split(".");
            cGraph = new CompressedGraph(inputFilePath,SplitInputFilePath[0]+"_offset.txt",true);
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








}
