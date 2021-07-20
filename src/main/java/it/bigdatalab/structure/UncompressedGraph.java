package it.bigdatalab.structure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.synth.SynthEditorPaneUI;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

public class UncompressedGraph {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.UncompressedGraph");


    //private int n = Integer.MAX_VALUE;
    private int[][] graph;
    private int [][] TGraph;
    public UncompressedGraph() {

    }

    public void load_graph(String inPath, String separator) throws FileNotFoundException {
        int[][] tmp_graph;
        int[] edges;
        Scanner sc;
        String[] line;
        int i, j, m, n;


        sc = new Scanner(new BufferedReader(new FileReader(inPath)));
        i = 0;
        logger.info("Loading Adiacency List");
        n = 0;
        while (sc.hasNextLine()) {
            sc.nextLine();
            n += 1;
        }
        tmp_graph = new int[n][];
        sc = new Scanner(new BufferedReader(new FileReader(inPath)));

        while (sc.hasNextLine()) {
            line = sc.nextLine().trim().split(separator);
            m = line.length;
            edges = new int[m];
            for (j = 0; j < m; j++) {
                edges[j] = Integer.parseInt(line[j]);
            }
            tmp_graph[i] = edges;
            i += 1;
        }
        graph = new int[i][];
        for (j = 0; j < i; j++) {
            graph[j] = tmp_graph[j];
        }
        logger.info("Adiacency List loaded");

    }
    public void  transpose_graph(){

        ArrayList<ArrayList<Integer>> transposed_tmp = new ArrayList<ArrayList<Integer>>() ;
        int n,i,j,k;
        int [] edges;
        n = graph.length;
        logger.info("Transposing Graph");
        for (i = 0 ; i<n;i++){
            transposed_tmp.add(new ArrayList<Integer>());
        }
        for (i = 0; i<n;i ++){
            for(j = 0;j<graph[i].length;j++){
                transposed_tmp.get(j).add(graph[i][0]);
            }
        }
        k = 0;
        for (i = 0;i<n;i++){
            if(transposed_tmp.get(i).size()>0){
                k+=1;
            }
        }
        TGraph = new int[k][];
        for(i = 0;i<k;i++){
            edges = new int[transposed_tmp.get(i).size()];
            for (j = 0;j <transposed_tmp.get(i).size();j++){
                edges[j] = transposed_tmp.get(i).get(j);
            }
            TGraph[i] = edges;
        }
        Arrays.sort(TGraph, (a, b) -> a[0] - b[0]);

        logger.info("Graph Transposed");


    }

    public int[][] getTGraph(){
        return TGraph;
    }

    public int[][] getGraph() {
        return graph;
    }

    public int[] get_neighbours(int node) {
        int [] neigh = new int[graph[node].length-1];
        int i;
        for (i = 1; i<graph[node].length;i++){
            neigh[i-1] = graph[node][i];
        }
        return(neigh);
    }

    public void setTGraph(int [][] TGraph){
        this.TGraph = TGraph;
    }
    public void setGraph(int[][] graph) {
        this.graph = graph;
    }
}
