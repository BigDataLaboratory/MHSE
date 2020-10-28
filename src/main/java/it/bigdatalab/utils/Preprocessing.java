package it.bigdatalab.utils;


import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;

public class Preprocessing {
    protected ImmutableGraph mProccessedGraph;
    protected ImmutableGraph mGraph;
    public Preprocessing(ImmutableGraph G){
        mGraph = G;
    }
    public void run(){
        ProgressLogger pl = new ProgressLogger();
        NodeIterator nodeIterator = mGraph.nodeIterator();
        int[] indegree = new int[mGraph.numNodes()];
        int [] outdegree = new int[mGraph.numNodes()];
        int [] mappedGraph = new int[mGraph.numNodes()];
        int numNodes = mGraph.numNodes();
        int d;
        int s;
        while(numNodes-- != 0) {
            int vertex = nodeIterator.nextInt();
            d = nodeIterator.outdegree();
            outdegree[vertex] = d;
            int[] neighbours = nodeIterator.successorArray();
            for (s = d; s-- != 0; ++indegree[neighbours[s]]) {}
        }
        d = 0;
        for (s = 0; s< indegree.length;s++){
            if((indegree[s] == 0) && (outdegree[s] ==0)){
                mappedGraph[s] = -1;
            }else{
                mappedGraph[s] = d;
                d+=1;
            }
        }
        mProccessedGraph = Transform.map(mGraph,mappedGraph,pl);
    }
    public  ImmutableGraph get_mProccessedGraph(){
        return(mProccessedGraph);
    }


}
