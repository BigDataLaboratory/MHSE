package it.bigdatalab.compression.VarIntGB;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

public class VarIntGBGraphCompressor {
    private boolean applyDifferential;
    private VarIntGB compressor;
    private DifferentialCompression gapCompressor;
    private int[][] uncompressedGraph;
    private int[][] uncompressedDifferentialMatrix;

    public VarIntGBGraphCompressor(String inPath, boolean applyDifferential) throws FileNotFoundException {
        loadUncompressedGraph(inPath);
        this.applyDifferential = applyDifferential;
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
        compressor = new VarIntGB();

        if (applyDifferential) {
            gapCompressor = new DifferentialCompression();
            uncompressedDifferentialMatrix = gapCompressor.encodeAdjList(uncompressedGraph);
            compressor.encodeAdjListFlat(uncompressedDifferentialMatrix);
        } else {
            compressor.encodeAdjListFlat(uncompressedGraph);
        }
    }

    public void writeCompressedGraphToFile(String outputFilePath) {
        compressor.saveEncoding(outputFilePath);
    }


}
