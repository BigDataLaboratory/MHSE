package it.bigdatalab.applications;

import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.algo.NeighbourhoodFunction;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 import it.unimi.dsi.webgraph.Stats;

import java.io.FileWriter;
import java.io.IOException;

public class GroundTruths {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHash");
    private String inputFilePath;
    private String outputFolderPath;
    private int threadNumber;
    private ImmutableGraph mGraph;
//    private double avgDistance;
//    private double diameter;
//    private double reachableCouples;
    //private ProgressLogger pl;

    public GroundTruths() throws IOException {

        initialize();

    }

    private void initialize() throws IOException{
        inputFilePath = PropertiesManager.getProperty("groundTruth.inputFilePath");
        outputFolderPath = PropertiesManager.getProperty("groundTruth.outputFilePath");
        logger.info("Loading graph at filepath {}", inputFilePath);
        mGraph = ImmutableGraph.load(inputFilePath);
        logger.info("Loading graph completed successfully");
        threadNumber =  Integer.parseInt(PropertiesManager.getProperty("goundTruth.threadNumber"));
        logger.info("Number of Threads "+threadNumber);

    }

    private void run() throws IOException{
//        long startTime;
//        long endTime;
//        long totalTime;
//        int max = 0;

        double visited_nodes = 0 ;
        double avg_distance = 0;
        double []NeighFunction ;
        double diameter;
        double eff_diameter;
        JSONObject bfsResults = new JSONObject();
        ProgressLogger pl = new ProgressLogger();
        //ParallelBreadthFirstVisit bfs = new ParallelBreadthFirstVisit(mGraph,4,false,pl);
        NeighbourhoodFunction neig = new NeighbourhoodFunction();
        NeighFunction = neig.compute(mGraph,threadNumber,pl);
        avg_distance = neig.averageDistance(NeighFunction);
        diameter = neig.effectiveDiameter(1,NeighFunction);
        eff_diameter = neig.effectiveDiameter(0.9,NeighFunction);
        visited_nodes = NeighFunction[NeighFunction.length-1];
        System.out.println("Average distance "+avg_distance);
        System.out.println("Diameter "+diameter);
        System.out.println("90% Effective Diameter "+eff_diameter);
        System.out.println("Reachable pairs "+visited_nodes);
        bfsResults.put("numNodes",mGraph.numNodes());
        bfsResults.put("numArcs",mGraph.numArcs());
        bfsResults.put("reachable_couples",visited_nodes);
        bfsResults.put("avg_distance", avg_distance);
        bfsResults.put("diameter", diameter);
        bfsResults.put("90EffectiveDiameter", eff_diameter);
        writeResults(bfsResults);
        //startTime = System.currentTimeMillis();
        // n times BFS
       /* NodeIterator nodeIterator = mGraph.nodeIterator();
        while(nodeIterator.hasNext()){
            int vertex = nodeIterator.nextInt();
            bfs.clear();
            int node_number = bfs.visit(vertex);
            visited_nodes += node_number;
            *//* Info about cutPoints
               queue and cutPoints, too, provide useful information. In particular, the nodes in queue
               from the d-th to the (d +1)-th cutpoint are exactly the nodes at distance d from the source.
             *//*
            String table;
            table = "{visitedNodes:"+Integer.toString(node_number);
            table += ",";
            table += "hops:[";
            for (int k=0;k<bfs.cutPoints.size();k++){
                table+=Integer.toString(bfs.cutPoints.getInt(k));
                if(k!=bfs.cutPoints.size()-1){
                    table+=",";
                }
            }
            table+="]}";
            bfsResults.put(Integer.toString(vertex),table);
            if(max < bfs.maxDistance()){
                max = bfs.maxDistance();
            }
            *//* Guardare link per capire cosa sto facendo :
                http://webgraph.di.unimi.it/docs-big/it/unimi/dsi/big/webgraph/algo/ParallelBreadthFirstVisit.html#marker
                d parte da 0 perché il primo elemento della coda è il nodo radice, ovvero il nodo a distanza 0
             *//*
            int d = 0;
            int a = 0;
            int b = 1;
            while(b<bfs.cutPoints.size()) {
                for (int q = bfs.cutPoints.getInt(a); q < bfs.cutPoints.getInt(b); q++) {
                    avg_distance += d;
                }
                d+=1;
                a+=1;
                b+=1;
            }

        }




        endTime = System.currentTimeMillis();
        totalTime = endTime - startTime;*/
        /*System.out.println("Somma di tutte le distanze = "+ avg_distance);
        System.out.println("Numero di nodi nel grafo = "+ mGraph.numNodes());

        double total_avg_distance =   ((double) avg_distance / ((double) mGraph.numNodes()*((double) (mGraph.numNodes()-1))));
        bfsResults.put("numNodes",mGraph.numNodes());
        bfsResults.put("numArcs",mGraph.numArcs());
        bfsResults.put("sum_distances",avg_distance);
        bfsResults.put("avg_distance", Double.toString(total_avg_distance));

        bfsResults.put("diameter",Integer.toString(max));
        bfsResults.put("reachableCouples",Long.toString(visited_nodes));

        System.out.println("Diametro " + max + " Numero di coppie raggiungibili "+ visited_nodes + " avg distance "+total_avg_distance);
        System.out.println("Tempo impiegato "+totalTime);
        */
    }

    private void writeResults(JSONObject jsonResults){

        try (FileWriter file = new FileWriter(outputFolderPath+"groundTruth.json")) {

            file.write(jsonResults.toJSONString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws IOException {
        GroundTruths main = new GroundTruths();
        main.run();

    }

}