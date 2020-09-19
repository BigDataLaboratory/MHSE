package it.bigdatalab.algorithm;

import edu.uci.ics.jung.algorithms.scoring.PageRank;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;


//import org.jgrapht.alg.scoring;

import java.io.IOException;
import java.util.*;


public class Clustering {

    private long [] clustersLables;
    private long [] clustersSignatures;
    private long [] clustersCentroids;
    private Int2ObjectOpenHashMap<long [] > clusterAssignments;


    public Clustering() throws IOException, MinHash.DirectionNotSetException {
        super();
        // Working on a new version of the clustering algorithm


    }
    public Int2ObjectOpenHashMap<Double> pagerank(ImmutableGraph G,float alpha , double[]  personalization, int max_iter,double tol, double[] nstart,double[] dangling ){
        Int2ObjectOpenHashMap<Double> Ranking ;
        // Create a copy in (right) stochastic form
        Dictionary W = stochastic_graph(G);
        Integer N = G.numNodes();
        double [] x = new double[N];
        double [] p = new double[N];
        double [] dangling_weights = new double[N];
        List<Integer> dangling_nodes = new ArrayList<Integer>();
        // Choose fixed starting vector if not given
        if(nstart.length == 0){
            for (int i = 0; i<N;i++){
                x[i] = 1.0/N;
            }
        }else{
            // Normalized nstart vector
            double s = 0;
            for (int i = 0; i<N;i++){
                s+= nstart[i];
            }
            for (int i = 0; i<N;i++){
                x[i] = nstart[i]/s;
            }
        }

        if(personalization.length == 0){
            // Assign uniform personalization vector if not given
            for (int i = 0; i<N;i++){
                p[i] = 1.0/N;
            }
        }else{
            // Normalized personalization vector
            double s = 0;
            for (int i = 0; i<N;i++){
                s+= personalization[i];
            }
            for (int i = 0; i<N;i++){
                p[i] = personalization[i]/s;
            }
        }
        if(dangling.length == 0){
            // Use personalization vector if dangling vector not specified
            dangling_weights = p;
        }else{
            // Normalized personalization vector
            double s = 0;
            for (int i = 0; i<N;i++){
                s+= dangling[i];
            }
            for (int i = 0; i<N;i++){
                dangling_weights[i] = dangling[i]/s;
            }
        }
        NodeIterator nodeIterator = G.nodeIterator();
        while(nodeIterator.hasNext()){
            int vertex = nodeIterator.nextInt();
            Integer outDeg = G.outdegree(vertex);
            if(outDeg == 0){
                dangling_nodes.add(vertex);
            }
        }


        // power iteration: make up to max_iter iterations

        for (int i = 0; i<max_iter;i++){
            double [] xlast = x;

        }


    }
    // Get the transition probability for each node
    public Dictionary stochastic_graph(ImmutableGraph G){

        Dictionary weights = new Hashtable();
        NodeIterator nodeIterator = G.nodeIterator();

        while(nodeIterator.hasNext()){
            int vertex = nodeIterator.nextInt();
            Integer outDeg = G.outdegree(vertex);
            if (outDeg == 0){
                System.out.println("zero out-degree for node: "+v);
                weights.put(v,0.0);
            }else{
                double archWeights = 1.0/outDeg;
                weights.put(v,archWeights);
            }
        }
        return(weights);

    }
}
