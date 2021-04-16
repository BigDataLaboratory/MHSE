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
    private boolean in_memory;
    private GroupVarInt compressor ;
    private DifferentialCompression Dcompressor;

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




    public void load_offset(String path) throws FileNotFoundException {
        Scanner sc ;
        String[] line;
        int [][] off;
        int n,m,i,j;
        logger.info("Loading offset file");
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


}