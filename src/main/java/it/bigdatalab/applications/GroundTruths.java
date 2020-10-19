package it.bigdatalab.applications;

import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.algo.ParallelBreadthFirstVisit;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;

public class GroundTruths {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHash");
    private String inputFilePath;
    private String outputFolderPath;
    private ImmutableGraph mGraph;
    private double avgDistance;
    private double diameter;
    private double reachableCouples;
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

    }

    private void run() throws IOException{
        long startTime;
        long endTime;
        long totalTime;
        int max = 0;
        long visited_nodes = 0 ;
        long avg_distance = 0;
        JSONObject bfsResults = new JSONObject();
        LongBigArrayBigList coda = new LongBigArrayBigList();
        LongBigArrayBigList queue = new LongBigArrayBigList();
        ProgressLogger pl = new ProgressLogger();
        ParallelBreadthFirstVisit bfs = new ParallelBreadthFirstVisit(mGraph,4,false,pl);
        startTime = System.currentTimeMillis();
        System.out.println("Inizio calcolo del diametro in modo esaustivo ");


        // n times BFS
        NodeIterator nodeIterator = mGraph.nodeIterator();

        //int distanza_check = 0;
        while(nodeIterator.hasNext()){
            int vertex = nodeIterator.nextInt();
            bfs.clear();
            int node_number = bfs.visit(vertex);
            visited_nodes += node_number;
            /* Info about cutPoints
               queue and cutPoints, too, provide useful information. In particular, the nodes in queue
               from the d-th to the (d +1)-th cutpoint are exactly the nodes at distance d from the source.
             */

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

                // Guardare link per capire cosa sto facendo :
                // http://webgraph.di.unimi.it/docs-big/it/unimi/dsi/big/webgraph/algo/ParallelBreadthFirstVisit.html#marker
                // d parte da 0 perché il primo elemento della coda è il nodo radice, ovvero il nodo a distanza 0
                if(bfs.cutPoints.size() >0) {
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
//                    System.out.println(a);
//                    System.out.println(b);
//                    for (int p=0;p<bfs.queue.size();p++){
//                        for(int q = )
//                        if(bfs.cutPoints.getInt(f) == p){
//                            f+=1;
//                            d+=1;
//                        }else{
//                            avg_distance+=d;
//                        }
//                    }

//                    if (distanza_check < d) {
//                          distanza_check = d;
//                    }

//                    int u = 0;
//                    int a = bfs.cutPoints.getInt(u);
//                    u+=1;
//                    int b = bfs.cutPoints.getInt(u);
//
//                    int len = bfs.cutPoints.size() - 1;
//
//                    while (b <= (bfs.cutPoints.getInt(len)-1)) {
//
//                        while (a < b) {
//                            avg_distance += d;
//                            a += 1;
//                        }
//                        d += 1;
//                        u+=1;
//                        b = bfs.cutPoints.getInt(u);
//
//                    }
//                    System.out.println(a);
//                    System.out.println(b);
//                    System.exit(-1);
//                    while(a < b ){
//                        avg_distance += d;
//                        a += 1;
//                    }
//                    if (distanza_check < d) {
//                        distanza_check = d;
//                    }
             }


            }




        endTime = System.currentTimeMillis();
        totalTime = endTime - startTime;
        System.out.println("Somma di tutte le distanze ="+ avg_distance);
        System.out.println("Numero di nodi nel grago = "+ mGraph.numNodes());
        double total_avg_distance =  avg_distance / (mGraph.numNodes()*(mGraph.numNodes()-1));
        bfsResults.put("numNodes",mGraph.numNodes());
        bfsResults.put("numArcs",mGraph.numArcs());
        bfsResults.put("avg_distance",Double.toString(total_avg_distance));
        bfsResults.put("diameter",Integer.toString(max));
        bfsResults.put("reachableCouples",Long.toString(visited_nodes));
        writeResults(bfsResults);
        System.out.println("Diametro " + max + " Numero di coppie raggiungibili "+ visited_nodes + " avg distance "+total_avg_distance);
        System.out.println("Tempo impiegato "+totalTime);
        //System.out.println("CONTROLLO Diametro "+distanza_check);
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