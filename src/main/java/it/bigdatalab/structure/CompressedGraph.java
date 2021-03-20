package it.bigdatalab.structure;

import com.google.common.io.Files;

import java.io.*;

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

//    public static int [] get_neighbours(int node){
//
//        int [] neighbours;
//
//
//
//        return(neighbours);
//    }

}
