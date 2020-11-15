package it.bigdatalab.algorithm;


import it.bigdatalab.model.GraphMeasure;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.util.Dictionary;

//import it.unimi.dsi.law.rank.PageRank;
//import it.unimi.dsi.law.rank.PageRankGaussSeidel;
//import it.unimi.dsi.law.rank.PageRankPowerSeries;
//import it.unimi.dsi.law.rank.SpectralRanking;
//import org.jgrapht.alg.scoring;


public class Clustering extends MinHash {
    private int threadNumber;
    private long [] clustersLables;
    private long [] clustersSignatures;
    private long [] clustersCentroids;
    //private Dictionary<Integer,Double> PageRank;
    private Dictionary<Integer,Double> APX_PageRank;
    private Int2ObjectOpenHashMap<long [] > clusterAssignments;


    public Clustering() throws IOException, MinHash.DirectionNotSetException {
        //super();
        // Working on a new version of the clustering algorithm


    }

    @Override
    public GraphMeasure runAlgorithm() throws IOException {

//
//        PageRankPowerSeries sr = new PageRankPowerSeries(mGraph);
//
//
//        SpectralRanking.StoppingCriterion stop1 = new SpectralRanking.IterationNumberStoppingCriterion(SpectralRanking.DEFAULT_MAX_ITER);
//        SpectralRanking.StoppingCriterion stop2 = new SpectralRanking.NormStoppingCriterion(SpectralRanking.DEFAULT_THRESHOLD);
//        SpectralRanking.StoppingCriterion stoppingCriterion = sr.or(stop1,stop2);
//        sr.stepUntil(stoppingCriterion);
//        System.out.println(sr.rank);
        return null;
    }
//    public void pagerank(ImmutableGraph G,double alpha , Dictionary personalization, int max_iter,double tol, Dictionary nstart,Dictionary dangling){
//        Int2ObjectOpenHashMap<Double> Ranking ;
//        // Create a copy in (right) stochastic form
//        Dictionary <Integer,Double>  W = stochastic_graph(G);
//        Integer N = G.numNodes();
//        Dictionary <Integer,Double> x = new Hashtable <Integer, Double>();
//        Dictionary <Integer,Double> p = new Hashtable <Integer, Double>();
//        Dictionary <Integer, Double> dangling_weights = new Hashtable <Integer, Double>();
//        List<Integer> dangling_nodes = new ArrayList<Integer>();
//        boolean converged = false;
//        // Choose fixed starting vector if not given
//        if(nstart.isEmpty()){
//            NodeIterator nodeIterator = G.nodeIterator();
//            while(nodeIterator.hasNext()){
//                int vertex = nodeIterator.nextInt();
//                x.put(vertex,1.0/N);
//            }
//
//        }else{
//            // Normalized nstart vector
//            double s = 0;
//            Enumeration<Integer> keys = x.keys();
//
//            while( keys.hasMoreElements() ){
//                s+= x.get(keys.nextElement());
//            }
//            Enumeration<Integer> keysX = x.keys();
//            while( keysX.hasMoreElements() ){
//                int key = keysX.nextElement();
//                x.put(key,x.get(key)/s);
//
//            }
//        }
//
//        if(personalization.isEmpty()){
//            // Assign uniform personalization vector if not given
//            NodeIterator nodeIterator = G.nodeIterator();
//            while(nodeIterator.hasNext()){
//                int vertex = nodeIterator.nextInt();
//                p.put(vertex,1.0/N);
//            }
//
//        }else{
//            // Normalized personalization vector
//            double s = 0;
//            Enumeration<Integer> keys = p.keys();
//
//            while( keys.hasMoreElements() ){
//                s+= p.get(keys.nextElement());
//            }
//
//            Enumeration<Integer> keysX = p.keys();
//            while( keysX.hasMoreElements() ){
//                int key = keysX.nextElement();
//                p.put(key,p.get(key)/s);
//
//            }
//
//
//        }
//        if(dangling.isEmpty()){
//            // Use personalization vector if dangling vector not specified
//            dangling_weights = p;
//        }else{
//            // Normalized personalization vector
//            double s = 0;
//            Enumeration<Integer> keys = dangling_weights.keys();
//
//            while( keys.hasMoreElements() ){
//                s+= dangling_weights.get(keys.nextElement());
//            }
//
//            Enumeration<Integer> keysX = dangling_weights.keys();
//            while( keysX.hasMoreElements() ){
//                int key = keysX.nextElement();
//                dangling_weights.put(key,dangling_weights.get(key)/s);
//
//            }
//
//        }
//        NodeIterator nodeIterator = G.nodeIterator();
//        while(nodeIterator.hasNext()){
//            int vertex = nodeIterator.nextInt();
//            Integer outDeg = G.outdegree(vertex);
//            if(outDeg == 0){
//                dangling_nodes.add(vertex);
//            }
//        }
//
//
//        // power iteration: make up to max_iter iterations
//        int i =0;
//        while((converged == false ) && (i<max_iter)){
//        //for (int i = 0; i<max_iter;i++){
//            Dictionary <Integer,Double> xlast = x;
//
//            Enumeration<Integer> keysX = x.keys();
//            while( keysX.hasMoreElements() ){
//                int key = keysX.nextElement();
//                x.put(key,xlast.get(key));
//
//            }
//
//            double s = 0;
//
//            for(int n=0;n< dangling_nodes.size();n++){
//                s+= xlast.get(n);
//            }
//            double danglesum = alpha * s;
//
//            Enumeration<Integer> keys = x.keys();
//
//            while( keys.hasMoreElements() ){
//                int n = keys.nextElement();
//
//                LazyIntIterator successors = G.successors(n);
//                int nbr;
//                while((nbr=successors.nextInt())!=-1){
//                    x.put(nbr,x.get(nbr)+(alpha*xlast.get(n) * W.get(n)) );
//                }
//                x.put(n,x.get(n)+ danglesum * dangling_weights.get(n) *(1.0-alpha)*p.get(n));
//
//            }
//            // Check the L1 norm
//            double err = 0;
//
//            Enumeration<Integer> k = x.keys();
//
//            while( k.hasMoreElements() ){
//                int c = k.nextElement();
//                err += Math.abs(x.get(c)-xlast.get(c));
//            }
//            if(err<N*tol){
//                this.PageRank = x;
//                converged = true;
//                System.out.println("Converged");
//
//            }
//        i+=1;
//        }
//        if(converged == false){
//            System.out.println("Error ! Pagerank not converged");
//        }
//
//    }

    // Get the transition probability for each node
//    public Dictionary stochastic_graph(ImmutableGraph G){
//
//        Dictionary<Integer,Double> weights = new Hashtable<Integer,Double> ();
//        NodeIterator nodeIterator = G.nodeIterator();
//
//        while(nodeIterator.hasNext()){
//            int vertex = nodeIterator.nextInt();
//            Integer outDeg = G.outdegree(vertex);
//            if (outDeg == 0){
//                System.out.println("zero out-degree for node: "+vertex);
//                weights.put(vertex,0.0);
//            }else{
//                double archWeights = 1.0/outDeg;
//                weights.put(vertex,archWeights);
//            }
//        }
//        return(weights);
//
//    }

//    public void pageRank(){
//
//    }
//
//
//    public Dictionary get_pagerank(){
//        if(this.PageRank.isEmpty()){
//            System.out.println("You must calculate pagerank first!");
//            System.exit(-1);
//        }
//        return(this.PageRank);
//    }
//    // This is the approximate pagerank proposed by Andersen et al. in "Local Graph Partitioning using PageRank Vectors" paper.
//    public void apx_pagerank(ImmutableGraph G,double alpha ,int v,double epsilon){
//        System.out.println("Transposing");
//        ImmutableGraph transposedG = Transform.transpose(G);
//        System.out.println("Done");
//        Dictionary<Integer,Double> p = new Hashtable<>();
//        Dictionary<Integer,Double> r = new Hashtable<>();
//        List <Integer> ratio;
//
//        NodeIterator nodeIterator = G.nodeIterator();
//        while(nodeIterator.hasNext()){
//            int vertex = nodeIterator.nextInt();
//            p.put(vertex,0.0);
//            if(vertex == v){
//                r.put(vertex,1.0);
//
//            }else{
//                r.put(vertex,0.0);
//            }
//        }
//
//        ratio = get_max_ratio(G,r,epsilon);
//        while(ratio.size()>0){
//            //for (int i =0;i<ratio.size();i++){
//                Random rand = new Random();
//                int randomElement = ratio.get(rand.nextInt(ratio.size()));
//                List <Dictionary<Integer,Double>> pushed = push(p,r,G,transposedG,randomElement,alpha);
//                p = pushed.get(0);
//                r = pushed.get(1);
//
//            //}
//            ratio = get_max_ratio(G,r,epsilon);
//
//        }
//        this.APX_PageRank = p;
//
//    }
//
//    public List <Dictionary<Integer,Double>>  push( Dictionary<Integer,Double> p, Dictionary<Integer,Double> r,ImmutableGraph G,ImmutableGraph transposedG,int u,double alpha){
//        Dictionary<Integer,Double> p1 = p;
//        Dictionary<Integer,Double> r1 = r;
//        p1.put(u,p.get(u)+alpha*r.get(u));
//        r1.put(u,(1-alpha)*r.get(u)/2);
//
//        LazyIntIterator successors = G.successors(u);
//        int nbr;
//        while((nbr=successors.nextInt())!=-1){
//            r1.put(nbr,r.get(nbr)+(1-alpha)*(r.get(u)/(2*G.outdegree(u))));
//        }
//        LazyIntIterator successorsTransposed = transposedG.successors(u);
//        int nbrT;
//        while((nbrT=successorsTransposed.nextInt())!=-1){
//            r1.put(nbrT,r.get(nbrT)+(1-alpha)*(r.get(u)/(2*transposedG.outdegree(u))));
//        }
//        List <Dictionary<Integer,Double>> result = new ArrayList<>();
//        result.add(p1);
//        result.add(r1);
//        return(result);
//    }
//
//    public List<Integer> get_max_ratio(ImmutableGraph G,Dictionary<Integer,Double> r,double epsilon) {
//        List<Integer> max_ratio = new ArrayList();
//        NodeIterator nodeIterator = G.nodeIterator();
//        while (nodeIterator.hasNext()) {
//            int vertex = nodeIterator.nextInt();
//            if (r.get(vertex) / G.outdegree(vertex) >= epsilon) {
//                max_ratio.add(vertex);
//            }
//        }
//        return (max_ratio);
//    }

    /*public void print_pagerank(){
        Enumeration<Integer> k = this.APX_PageRank.keys();

        while( k.hasMoreElements() ){
            int c = k.nextElement();
            System.out.println("Nodo "+c+ " rank "+this.APX_PageRank.get(c));
        }
    }*/
}
