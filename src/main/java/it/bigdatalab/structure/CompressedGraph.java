package it.bigdatalab.structure;

import com.google.common.io.Files;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class CompressedGraph {
    private byte[] compressed_graph;
    private int[][] offset;
    private boolean in_memory;

    public  CompressedGraph(String inPath,boolean load_entire_graph) throws IOException {
        if(load_entire_graph){
            load_compressed_graph(inPath);
        }
        // Da sviluppare
    }

    public void load_compressed_graph(String inPath) throws IOException {
        File file = new File(inPath);
        compressed_graph = new byte[(int) file.length()];
        try {
            InputStream input = null;
            try {
                int totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                while (totalBytesRead < compressed_graph.length) {
                    int bytesRemaining = compressed_graph.length - totalBytesRead;
                    //input.read() returns -1, 0, or more :
                    int bytesRead = input.read(compressed_graph, totalBytesRead, bytesRemaining);
                    if (bytesRead > 0) {
                        totalBytesRead = totalBytesRead + bytesRead;
                    }
                }
            } finally {
                input.close();
            }
        } catch (FileNotFoundException ex) {
        }



    }
    public void load_offset(String path) throws FileNotFoundException {
        Scanner sc ;
        String[] line;
        int [][] off;
        int n,m,i,j;
        sc = new Scanner(new BufferedReader(new FileReader(path)));
        line = sc.nextLine().trim().split("\t");
        n =  Integer.parseInt(line[0]);
        m = Integer.parseInt(line[1]);
        off = new int[n][m];
        while(sc.hasNextLine()) {
            for (i=0; i<n; i++) {
                line = sc.nextLine().trim().split("\t");
                for (j=0; j<m; j++) {
                    off[i][j] = Integer.parseInt(line[j]);
                }
            }
        }
        //System.out.println(Arrays.deepToString(off));
        offset = off;
        System.out.println("Offset loaded");
    }

    }

//    public static int [] get_neighbours(int node){
//
//        int [] neighbours;
//
//
//
//        return(neighbours);
//    }


