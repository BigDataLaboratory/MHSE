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

/**
 * Implementation of the compressed graph data structure
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
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


    /**
     * Define a Compressed Graph instance loading the graph
     * @param inPath String input path
     * @param offPath String offset path
     * @param load_entire_graph Boolean
     * @throws IOException
     */
    public CompressedGraph(String inPath,String offPath,boolean load_entire_graph) throws IOException {
        if(load_entire_graph){

            load_compressed_graph(inPath,offPath);
        }
        // To do
    }


    /**
     * Return compressed graph
     * @return byte array compressed graph
     */
    public byte[] getCompressed_graph() {
        return compressed_graph;
    }

    /**
     * Loads the compressed file
     * @param inPath String input path file
     * @param offPath String offset path file
     * @throws IOException
     */
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


    /**
     * Loads the offset file
     * @param inPath offset file path
     * @throws IOException
     */
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
        sc2.close();


        } finally {


                logger.info("Offset loaded");

                // Getting the number of nodes of the compressed graph
                nNodes =  offset.length;
        }
    } catch (FileNotFoundException ex) {
    }


    }

    /**
     * Return the neighbours of a node
     * @param node Int node
     * @param differential Boolean differential compression?
     * @return Int array of neighbours
     */
    public int [] get_neighbours(int node,boolean differential){
        int [] neighbours,neighbours_array;
        byte [] toDecode ;
        int i,k;
        compressor = new GroupVarInt();
        GapCompressor = new DifferentialCompression();
        if(node == offset[0][0]){
            toDecode = new byte[offset[node][1]];
            k =0;
            for(i = 0;i<offset[0][1];i++){
                toDecode[k] = compressed_graph[i];
                k+=1;

            }

        }else if(node ==  offset[offset.length-1][0]){

            toDecode = new byte[compressed_graph.length -offset[offset.length-2][1]];
            k = 0;
            for(i = offset[offset.length-2][1];i<compressed_graph.length;i++){
                toDecode[k] = compressed_graph[i];
                k+=1;
            }
        }else{

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

        neighbours_array = new int[neighbours.length -1];
        for (i = 1; i < neighbours.length; i++) {
            neighbours_array[i - 1] = neighbours[i];
        }

        return (neighbours_array);
    }


    /**
     * Return the decoded graph
     * @return 2D array
     */
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

    /**
     * Returns the nodes of the graph
     * @return int array
     */
    public int [] get_nodes(){
        int [] nodes = new int[nNodes];
        int i,n;

        for (i = 0; i<nNodes;i++){
            nodes[i] = offset[i][0];
        }
        return(nodes);
    }

    /**
     * Returns the number of arcs
     * @return Int
     */
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

    /**
     * Returns the outdegree of a node
     * @param node Integer node
     * @param differential Boolean differential?
     * @return Int number of neighbours
     */
    public int outdegree(int node,boolean differential){
        int [] neig = get_neighbours(node,differential);
        if(neig != null) {
            return (get_neighbours(node, differential).length);
        }else{
            return 0;
        }
    }

    /**
     * Get the offset
     * @return 2D array
     */
    public int [][] get_offset(){
        return (offset);
    }

    /**
     * Set the offset
     * @param off 2D array offset
     */
    public void set_offset(int [][] off){
        offset = off;
    }

    /**
     * Set compressed graph
     * @param cG byte array
     */
    public void set_compressed_graph(byte [] cG){
        compressed_graph = cG;
    }
}