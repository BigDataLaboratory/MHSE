package it.bigdatalab.structure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Scanner;

public class UncompressedGraph {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.UncompressedGraph");


    //private int n = Integer.MAX_VALUE;
    private int[][] graph;

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

    public void setGraph(int[][] graph) {
        this.graph = graph;
    }
}
