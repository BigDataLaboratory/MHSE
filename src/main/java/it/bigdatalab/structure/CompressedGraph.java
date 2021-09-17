package it.bigdatalab.structure;

import it.bigdatalab.compression.DifferentialCompression;
import it.bigdatalab.compression.GroupVarInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class CompressedGraph {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.CompressedGraph");

    private long compressed_graph_size;
    private byte[] compressed_graph;
    private int[][] decoded_graph;
    private int[][] offset;
    private byte[] compressed_offset;
    private boolean in_memory;
    private boolean diff = true;
    private boolean gVarint;
    private int nNodes = -1;
    private int nArcs = -1;
    private boolean isUndirected = false;
    private GroupVarInt compressor ;
    private DifferentialCompression Dcompressor;
    private DifferentialCompression GapCompressor;
    private boolean differentialCompression = false;
//    private int i = 0;
//    private int [] neigh;

    public CompressedGraph(String inPath,String offPath,boolean load_entire_graph) throws IOException {
        if(load_entire_graph){

            load_compressed_graph(inPath,offPath);
        }
        // Da sviluppare

    }



    public byte[] getCompressed_graph() {
        return compressed_graph;
    }

    public void load_compressed_graph(String inPath, String offPath) throws IOException {
        logger.info("Loading the compressed Graph");
        File file = new File(inPath);
        Path path = Paths.get(inPath);
        long size = Files.size(path);
        compressed_graph = new byte[(int) file.length()];
        System.out.println("ALLOCATED MEMORY "+compressed_graph.length);
        System.out.println("FILE LENGTH "+file.length());
        System.out.println("SIZE "+size);
        compressed_graph_size =  file.length();

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
        load_offset(offPath);

    }


    public void load_offset(String inPath) throws IOException {
        int n,i;
        logger.info("Loading offset");
        try {
            System.out.println(inPath);
            Scanner sc = new Scanner(new BufferedReader(new FileReader(inPath)));

        try{
        n = 0;
        while(sc.hasNextLine()) {
            n+=1;
            sc.nextLine();
        }
        System.out.println(n);
        sc.close();
        Scanner sc2 = new Scanner(new BufferedReader(new FileReader(inPath)));

        offset = new int[n][2];

        while(sc2.hasNextLine()) {
            for (i =0;i<n;i++) {


                String[] line = sc2.nextLine().split("\\t");

                offset[i][0] = Integer.parseInt(line[0]);
                offset[i][1] = Integer.parseInt(line[1]);
            }
        }
        //offset = offset_reader;
        sc2.close();


        } finally {


                logger.info("Offset loaded");

                // Getting the number of nodes of the compressed graph
                nNodes =  offset.length;
            //logger.info("Compressed offset loaded");
        }
    } catch (FileNotFoundException ex) {
    }
//        try {
//            InputStream input = null;
//            try {
//                int totalBytesRead = 0;
//                input = new BufferedInputStream(new FileInputStream(file));
//                while (totalBytesRead < compressed_offset.length) {
//                    int bytesRemaining = compressed_offset.length - totalBytesRead;
//                    //input.read() returns -1, 0, or more :
//                    int bytesRead = input.read(compressed_offset, totalBytesRead, bytesRemaining);
//                    if (bytesRead > 0) {
//                        totalBytesRead = totalBytesRead + bytesRead;
//                    }
//
//                }
//            } finally {
//                input.close();
//                logger.info("Compressed offset loaded");
//            }
//        } catch (FileNotFoundException ex) {
//        }
//
//        logger.info("Decompressing offset");
//        decomrpessed_offset = compressor.decode(compressed_offset);
//        n = decomrpessed_offset.length/2;
//        gap_encoding_nodes = new int [n];
//        gap_encoding_bytes = new int [n];
       // off = new int[n][2];
//        for(i = 0; i<n;i++){
//            gap_encoding_bytes[i] = decomrpessed_offset[i];
//            gap_encoding_nodes[i] = decomrpessed_offset[i+n];
//        }
//        if(differentialCompression) {
//            gap_decoded_bytes = GapCompressor.decodeSequence(gap_encoding_bytes);
//            gap_decoded_nodes = GapCompressor.decodeSequence(gap_encoding_nodes);
//        }else{
//            gap_decoded_bytes = gap_encoding_bytes;
//            gap_decoded_nodes = gap_encoding_nodes;
//        }
//
//        for (i = 0;i<n;i++){
//            off[i][0] = offset_reader[i][0];
//            off[i][1] = offset_reader[i][1];
//
//        }
      //  offset = off;

//        try {
//            int j, m;
//            BufferedWriter bw = new BufferedWriter(new FileWriter(inPath + "_offset_non_comp_test.txt"));
//
//            for (i = 0; i < n; i++) {
//                m = offset[i].length;
//                for (j = 0; j < m; j++) {
//                    bw.write(offset[i][j] + ((j == offset[i].length - 1) ? "" : "\t"));
//                }
//                bw.newLine();
//            }
//            bw.flush();
//        } catch (IOException e) {
//        }



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
    public int [] get_neighbours(int node,boolean differential){
        int [] neighbours,neighbours_array;
        byte [] toDecode ;
        int i,k;
        compressor = new GroupVarInt();
        GapCompressor = new DifferentialCompression();
        //System.out.println("NODE "+node+ " !!!");
        if(node == offset[0][0]){
            //System.out.println("SONO 0");
            toDecode = new byte[offset[node][1]];
            k =0;
            for(i = 0;i<offset[0][1];i++){
                toDecode[k] = compressed_graph[i];
                //System.out.println(toDecode[k]);
                k+=1;

            }

        }else if(node ==  offset[offset.length-1][0]){
//            System.out.println("SONO ULTIMO");
//            System.out.println("COMP G LEN "+compressed_graph.length);
//            System.out.println("OFF "+offset[offset.length-2][1]);
            toDecode = new byte[compressed_graph.length -offset[offset.length-2][1]];
            k = 0;
            for(i = offset[offset.length-2][1];i<compressed_graph.length;i++){
                toDecode[k] = compressed_graph[i];
                k+=1;
            }
        }else{
            //System.out.println("STO NEL MEZZO ");

            toDecode = new byte[offset[node][1] -offset[node-1][1]];
            k = 0;
            for(i = offset[node-1][1] ;i<offset[node][1];i++){
                toDecode[k] = compressed_graph[i];
                k+=1;
            }
        }

        neighbours = compressor.dec(toDecode);
        if(differential){
            neighbours = GapCompressor.decodeSequence(neighbours);
        }
        //System.out.println("TESTJSDA");
//        for(int p=0;p<neighbours.length;p++) {
//            System.out.println(neighbours[p]);
//        }
        neighbours_array = new int[neighbours.length -1];
        for (i = 1; i < neighbours.length; i++) {
            neighbours_array[i - 1] = neighbours[i];
        }

        return (neighbours_array);
    }
    public int [] get_neighbours_dep(int node, boolean differential){
        int i,j,k,y;
        int [] neighbours = new int[0];
        int [] neighbours_array;
        byte [] portion;
        if(decoded_graph == null) {
            compressor = new GroupVarInt();
            Dcompressor = new DifferentialCompression();
            //for (i = 0; i < offset.length; i++) {
                //if (offset[i][0] == node) {
            /*
                    System.out.println("OFFSET ");
                    for (int o = 0; o<offset.length;o++){
                        System.out.println("NODEO "+offset[o][0]+ "   BITES "+offset[o][1]);
                    }
                    System.out.println(node);
                    System.out.println("Uhsfdah");
                    System.out.println(offset.length);

             */
                    System.out.println("NODE "+node + " offset length "+offset.length);
                    if (node !=  offset[offset.length-1][0]) {

                        if (node == 0) {
                            System.out.println("SONO 0");
                            System.out.println("OFFSET 0 = "+offset[node][1]);
                            portion = new byte[offset[node][1]];
                            //k = 0;
                            for (j = 0; j < offset[node][1]; j++) {
                                portion[j] = compressed_graph[j];
                                //System.out.println("PORT lol "+portion[k]);
                                //k += 1;
                            }
                        } else {
                            System.out.println("NON SONO  0");
                            int offDiff = offset[node][1] - offset[node - 1][1];
                            System.out.println("OFFSET NON 0 = "+offDiff);
                            portion = new byte[offset[node][1] - offset[node - 1][1]];
                            k = 0;
                            for (j = offset[node - 1][1]; j < offset[node][1]; j++) {
                                portion[k] = compressed_graph[j];
                                k += 1;
                            }
                        }
                    } else {
                        System.out.println("SONO ULTIMO ");
                        int offDiff = offset[node][1] - offset[node - 1][1];
                        System.out.println("OFFSET NON 0 = "+offDiff);

                        portion = new byte[offset[node][1] - offset[node - 1][1]];

                        k = 0;
                        for (j = offset[node - 1][1]; j < offset[node][1]; j++) {
                            portion[k] = compressed_graph[j];
                            k += 1;
                        }
                    }

                    System.out.println("DIEEF = "+differential);

                    if (differential) {
                            System.out.println("LUNG PORTION "+portion.length);
                            for (int b = 0; b<portion.length;b++) {
                                System.out.println("PORT " + portion[b]);
                            }
                            int [] decoded_negihbours = compressor.dec(portion);
                                for (int b = 0; b<decoded_negihbours.length;b++) {
                                    System.out.println("PORT " + decoded_negihbours[b]);
                                }

                            int[] to_decode = new int[decoded_negihbours.length];
                            to_decode[0] = 0;
                            for (y = 1; y < decoded_negihbours.length; y++) {
                                to_decode[y] = decoded_negihbours[y];
                            }
                            neighbours = Dcompressor.decodeSequence(to_decode);

                    } else {
                        System.out.println("port LEN "+portion.length);

                        neighbours = compressor.dec(portion);
                        System.out.println("NEG LEN "+neighbours.length);
                        for(int o = 0; o<neighbours.length;o++){
                            System.out.println("NEIG = "+neighbours[o]);
                        }
                    }

                //}
            //}
        }else{
            // Da implementare c'è da decidere che struttura dati utilizzare per il grafo decompresso
        }

            neighbours_array = new int[neighbours.length -1];
            for (j = 1; j < neighbours.length; j++) {
                neighbours_array[j - 1] = neighbours[j];
            }

            return (neighbours_array);

    }




    public int[][] getDecoded_graph() {
        return decoded_graph;
    }


    public int numNodes(){
        if(nNodes == -1){
            int i,j,nodes,node;
            int [] edge_list;
            nodes = 0;
            for (i = 0;i<offset.length;i++){
                node = offset[i][0];

                edge_list = get_neighbours(node,diff);
                for(j =0 ;j<edge_list.length;j++){
                    if(edge_list[j] >nodes){
                        nodes = edge_list[j];
                    }
                }
            }
            nNodes = nodes;
        }

        return nNodes;
    }

    public int [] get_nodes(){
        int [] nodes = new int[nNodes];
        int i,n;

        for (i = 0; i<nNodes;i++){
            nodes[i] = offset[i][0];
        }
        return(nodes);
    }
    public int numArcs(){
        if(nArcs == -1){
            int arcs,i;
            arcs = 0;
            for(i = 0; i<offset.length;i++){
                arcs += outdegree(offset[i][0],diff);
            }
            if(isUndirected){
                nArcs =(1/2) * arcs;
            }else {
                nArcs = arcs;
            }
            return nArcs;
        }else{
            return nArcs;
        }
    }
    public int outdegree(int node,boolean differential){
        int [] neig = get_neighbours(node,differential);
        if(neig != null) {
            return (get_neighbours(node, differential).length);
        }else{
            return 0;
        }
    }
    public int [][] getOffset(){
        return offset;
    }

//    public void NodeIterator(int node,boolean diff){
//        neigh = get_neighbours(node,diff);
//    }
//    public boolean hasNext() {
//        return neigh.length > i;
//    }
//
//    public Integer next() {
//        return Integer.valueOf(neigh[i++]);
//    }
    public int [][] get_offset(){
        return (offset);
    }
   public void set_offset(int [][] off){
        offset = off;
    }

    public void set_compressed_graph(byte [] cG){
        compressed_graph = cG;
    }
}