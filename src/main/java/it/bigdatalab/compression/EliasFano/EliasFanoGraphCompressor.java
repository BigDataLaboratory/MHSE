package it.bigdatalab.compression.EliasFano;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class EliasFanoGraphCompressor {
    private EliasFano compressor;
    private int[][] uncompressedGraph;

    public EliasFanoGraphCompressor(String inPath) throws FileNotFoundException {
        loadUncompressedGraph(inPath);
        compressGraph();
    }

    public void loadUncompressedGraph(String inPath) throws FileNotFoundException {
        int[][] tmp_graph;
        int[] edges;
        Scanner sc;
        String[] line;
        int i, j, m, n;

        sc = new Scanner(new BufferedReader(new FileReader(inPath)));
        i = 0;
        n = 0;
        while (sc.hasNextLine()) {
            sc.nextLine();
            n += 1;
        }
        tmp_graph = new int[n][];
        sc = new Scanner(new BufferedReader(new FileReader(inPath)));

        while (sc.hasNextLine()) {
            line = sc.nextLine().trim().split("\t");
            m = line.length;
            edges = new int[m];
            for (j = 0; j < m; j++) {
                edges[j] = Integer.parseInt(line[j]);
            }
            tmp_graph[i] = edges;
            i += 1;
        }
        uncompressedGraph = new int[i][];
        for (j = 0; j < i; j++) {
            uncompressedGraph[j] = tmp_graph[j];
        }

    }

    public void compressGraph() {
        compressor = new EliasFano();
        compressor.encodeAdjListFlat(uncompressedGraph);
    }

    public void writeCompressedGraphToFile(String outputFilePath) throws IOException {
        compressor.saveEncoding(outputFilePath);
    }

}
