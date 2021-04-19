package it.bigdatalab.structure;

import com.google.common.io.Files;
import it.bigdatalab.compression.DifferentialCompression;
import it.bigdatalab.compression.GroupVarInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class CompressedGraph {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.CompressedGraph");


    private byte[] compressed_graph;
    private int[][] decoded_graph;
    private int[][] offset;
    private byte[] compressed_offset;

    private boolean in_memory;
    private GroupVarInt compressor ;
    private DifferentialCompression Dcompressor;
    private DifferentialCompression GapCompressor;

    public CompressedGraph(String inPath,boolean load_entire_graph) throws IOException {
        if(load_entire_graph){
            load_compressed_graph(inPath);
        }
        // Da sviluppare
    }

    public byte[] getCompressed_graph() {
        return compressed_graph;
    }

    public void load_compressed_graph(String inPath) throws IOException {
        logger.info("Loading the compressed Graph");
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
                logger.info("Compressed Graph loaded");
            }
        } catch (FileNotFoundException ex) {
        }


    }


    public void load_offset(String inPath) throws IOException {
        int n,i;
        int [] gap_encoding_nodes,gap_encoding_bytes,gap_decoded_nodes,gap_decoded_bytes;
        int [][] off;

        logger.info("Loading offset");
        File file = new File(inPath);
        compressed_offset = new byte[(int) file.length()];
        compressor = new GroupVarInt();
        GapCompressor = new DifferentialCompression();
        int [] decomrpessed_offset;
        try {
            InputStream input = null;
            try {
                int totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                while (totalBytesRead < compressed_offset.length) {
                    int bytesRemaining = compressed_offset.length - totalBytesRead;
                    //input.read() returns -1, 0, or more :
                    int bytesRead = input.read(compressed_offset, totalBytesRead, bytesRemaining);
                    if (bytesRead > 0) {
                        totalBytesRead = totalBytesRead + bytesRead;
                    }
                }
            } finally {
                input.close();
                logger.info("Compressed offset loaded");
            }
        } catch (FileNotFoundException ex) {
        }


        decomrpessed_offset = compressor.decode(compressed_offset);
        n = decomrpessed_offset.length/2;
        gap_encoding_nodes = new int [n];
        gap_encoding_bytes = new int [n];
        off = new int[n][2];

        for(i = 0; i<n;i++){
            gap_encoding_bytes[i] = decomrpessed_offset[i];
            gap_encoding_nodes[i] = decomrpessed_offset[i+n];

        }
        gap_decoded_bytes = GapCompressor.decodeSequence(gap_encoding_bytes);
        gap_decoded_nodes = GapCompressor.decodeSequence(gap_encoding_nodes);
        for (i = 0;i<n;i++){
            off[i][0] = gap_decoded_nodes[i];
            off[i][1] = gap_decoded_bytes[i];
        }
        offset = off;
        logger.info("Offset loaded");


    }

    // DEPRECATED: Old offset
    public void load_offset_dep(String path) throws FileNotFoundException {
        Scanner sc ;
        String[] line;
        int [][] off;
        int [] gap_encoding_nodes,gap_encoding_bytes,gap_decoded_nodes,gap_decoded_bytes;
        int n,m,i,j;
        DifferentialCompression GapCompressor = new DifferentialCompression();
        logger.info("Loading offset file");
        sc = new Scanner(new BufferedReader(new FileReader(path)));
        n = 0;
        while(sc.hasNextLine()) {
            sc.nextLine();
            n+=1;
        }
        sc = new Scanner(new BufferedReader(new FileReader(path)));
        off = new int[n][2];
        gap_encoding_nodes = new int [n];
        gap_encoding_bytes = new int [n];
        while(sc.hasNextLine()) {
            for (i=0; i<n; i++) {
                line = sc.nextLine().trim().split("\t");
                    gap_encoding_nodes[i] = Integer.parseInt(line[0]);
                    gap_encoding_bytes[i] =  Integer.parseInt(line[1]);

            }
        }
        gap_decoded_bytes = GapCompressor.decodeSequence(gap_encoding_bytes);
        gap_decoded_nodes = GapCompressor.decodeSequence(gap_encoding_nodes);
        for (i = 0;i<n;i++){
            off[i][0] = gap_decoded_nodes[i];
            off[i][1] = gap_decoded_bytes[i];
        }
        offset = off;
        logger.info("Offset loaded");
    }



    public int [] get_neighbours(int node, boolean differential){
        int i,j,k;
        int [] neighbours = new int[0];
        int [] neighbours_array;
        byte [] portion = new byte[0];
        compressor = new GroupVarInt();
        Dcompressor = new DifferentialCompression();
        for (i = 0;i<offset.length;i++){
            if(offset[i][0] == node){
                if(i<offset.length-1){
                    if(i == 0){
                        portion = new byte[offset[i][1]];
                        k = 0;
                        for (j = 0;j<offset[i][1];j++){
                            portion[k] = compressed_graph[j];
                            k+=1;
                        }
                    }else {
                        portion = new byte[offset[i][1] - offset[i-1][1]];
                        k = 0;
                        for (j = offset[i-1][1]; j < offset[i][1]; j++) {
                            portion[k] = compressed_graph[j];
                            k += 1;
                        }
                    }
                }else{

                    portion = new byte[offset[i][1] - offset[i-1][1]];

                        k = 0;
                        for (j = offset[i-1][1];j<offset[i][1];j++){
                            portion[k] = compressed_graph[j];
                            k+=1;
                        }
                }
                if(differential){
                    neighbours = Dcompressor.decodeSequence(compressor.decode(portion));
                }else {
                    neighbours = compressor.decode(portion);
                }
            }
        }
        neighbours_array = new int[neighbours.length-1];
        for (j = 1; j<neighbours.length;j++){
            neighbours_array[j-1] = neighbours[j];
        }
        return(neighbours_array);
    }

    public void decode_graph(){
        logger.info("Decoding the whole Graph ");
        decoded_graph = new int[offset.length][];
        byte [] portion = new byte[0];
        int i,j,k;
        compressor = new GroupVarInt();
        Dcompressor = new DifferentialCompression();

        for (i = 0; i<offset.length;i++){
            if(i == 0){
                portion = new byte[offset[i][1]];
                k = 0;
                for (j = 0;j<offset[i][1];j++){
                    portion[k] = compressed_graph[j];
                    k+=1;
                }
            }else if(i<offset.length){
                portion = new byte[offset[i][1] - offset[i-1][1]];
                k = 0;
                for (j = offset[i-1][1]; j < offset[i][1]; j++) {
                    portion[k] = compressed_graph[j];
                    k += 1;
                }
            }else{
                portion = new byte[offset[i][1] - offset[i-1][1]];

                k = 0;
                for (j = offset[i-1][1];j<offset[i][1];j++){
                    portion[k] = compressed_graph[j];
                    k+=1;
                }

            }
            decoded_graph[i] = compressor.decode(portion);


        }
        logger.info("Decoding completed ");

//        for (i = 0;i < decoded_graph.length;i++){
//            for ( j = 0; j< decoded_graph[i].length;j++){
//                System.out.println(" "+decoded_graph[i][j]);
//            }
//            System.out.println("\n");
//        }
    }

    public int[][] getDecoded_graph() {
        return decoded_graph;
    }


    public int numNodes(){
        return offset.length;
    }
    public int outdegree(int node,boolean differential){
        return(get_neighbours(node,differential).length);
    }
}