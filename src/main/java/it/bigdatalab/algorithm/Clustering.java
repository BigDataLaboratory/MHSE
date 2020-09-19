package it.bigdatalab.algorithm;

import edu.uci.ics.jung.algorithms.scoring.PageRank;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;


//import org.jgrapht.alg.scoring;

import java.io.IOException;
import java.util.*;


public class Clustering {

    private long [] clustersLables;
    private long [] clustersSignatures;
    private long [] clustersCentroids;
    private Dictionary<Integer,Double> PageRank;
    private Int2ObjectOpenHashMap<long [] > clusterAssignments;


    public Clustering() throws IOException, MinHash.DirectionNotSetException {
        super();
        // Working on a new version of the clustering algorithm


    }
    public void pagerank(ImmutableGraph G,float alpha , Dictionary personalization, int max_iter,double tol, Dictionary nstart,Dictionary dangling ){
        Int2ObjectOpenHashMap<Double> Ranking ;
        // Create a copy in (right) stochastic form
        Dictionary <Integer,Double>  W = stochastic_graph(G);
        Integer N = G.numNodes();
        Dictionary <Integer,Double> x = new Hashtable <Integer, Double>();
        Dictionary <Integer,Double> p = new Hashtable <Integer, Double>();
        Dictionary <Integer, Double> dangling_weights = new Hashtable <Integer, Double>();
        List<Integer> dangling_nodes = new ArrayList<Integer>();
        // Choose fixed starting vector if not given
        if(nstart.isEmpty()){
            NodeIterator nodeIterator = G.nodeIterator();
            while(nodeIterator.hasNext()){
                int vertex = nodeIterator.nextInt();
                x.put(vertex,1.0/N);
            }

        }else{
            // Normalized nstart vector
            double s = 0;
            Enumeration<Integer> keys = x.keys();

            while( keys.hasMoreElements() ){
                s+= x.get(keys.nextElement());
            }
            Enumeration<Integer> keysX = x.keys();
            while( keysX.hasMoreElements() ){
                int key = keysX.nextElement();
                x.put(key,x.get(key)/s);

            }
        }

        if(personalization.isEmpty()){
            // Assign uniform personalization vector if not given
            NodeIterator nodeIterator = G.nodeIterator();
            while(nodeIterator.hasNext()){
                int vertex = nodeIterator.nextInt();
                p.put(vertex,1.0/N);
            }

        }else{
            // Normalized personalization vector
            double s = 0;
            Enumeration<Integer> keys = p.keys();

            while( keys.hasMoreElements() ){
                s+= p.get(keys.nextElement());
            }

            Enumeration<Integer> keysX = p.keys();
            while( keysX.hasMoreElements() ){
                int key = keysX.nextElement();
                p.put(key,p.get(key)/s);

            }


        }
        if(dangling.isEmpty()){
            // Use personalization vector if dangling vector not specified
            dangling_weights = p;
        }else{
            // Normalized personalization vector
            double s = 0;
            Enumeration<Integer> keys = dangling_weights.keys();

            while( keys.hasMoreElements() ){
                s+= dangling_weights.get(keys.nextElement());
            }

            Enumeration<Integer> keysX = dangling_weights.keys();
            while( keysX.hasMoreElements() ){
                int key = keysX.nextElement();
                dangling_weights.put(key,dangling_weights.get(key)/s);

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
            Dictionary <Integer,Double> xlast = x;

            Enumeration<Integer> keysX = x.keys();
            while( keysX.hasMoreElements() ){
                int key = keysX.nextElement();
                x.put(key,0.0);

            }

            double s = 0;

            for(int n=0;n< dangling_nodes.size();n++){
                s+= xlast.get(n);
            }
            double danglesum = alpha * s;

            Enumeration<Integer> keys = x.keys();

            while( keys.hasMoreElements() ){
                int n = keys.nextElement();

                LazyIntIterator successors = G.successors(n);
                int nbr;
                while((nbr=successors.nextInt())!=-1){
                    x.put(nbr,x.get(nbr)+(alpha*xlast.get(n) * W.get(n)) );
                }
                x.put(n,x.get(n)+ danglesum * dangling_weights.get(n) *(1.0-alpha)*p.get(n));

            }
            // Check the L1 norm
            double err = 0;

            Enumeration<Integer> k = x.keys();

            while( k.hasMoreElements() ){
                int c = keys.nextElement();
                err += Math.abs(x.get(c)-xlast.get(c));
            }
            if(err<N*tol){
                this.PageRank = x;
            }

        }
        System.out.println("Error ! Pagerank not converged");
    }

    // Get the transition probability for each node
    public Dictionary stochastic_graph(ImmutableGraph G){

        Dictionary<Integer,Double> weights = new Hashtable<Integer,Double> ();
        NodeIterator nodeIterator = G.nodeIterator();

        while(nodeIterator.hasNext()){
            int vertex = nodeIterator.nextInt();
            Integer outDeg = G.outdegree(vertex);
            if (outDeg == 0){
                System.out.println("zero out-degree for node: "+vertex);
                weights.put(vertex,0.0);
            }else{
                double archWeights = 1.0/outDeg;
                weights.put(vertex,archWeights);
            }
        }
        return(weights);

    }
}
