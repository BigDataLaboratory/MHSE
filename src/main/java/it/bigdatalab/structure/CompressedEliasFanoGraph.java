package it.bigdatalab.structure;

import it.bigdatalab.compression.DifferentialCompression;
import it.bigdatalab.compression.EliasGamma;
import it.bigdatalab.compression.GroupVarInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class CompressedEliasFanoGraph {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.CompressedEliasFanoGraph");
    private long compressed_graph_size;
    private byte[] compressed_graph;
    private int[][] decoded_graph;
    private int[][] offset;
    private EliasGamma compressor ;
    private boolean isUndirected = false;
    private int nNodes = -1;
    private int nArcs = -1;

    public CompressedEliasFanoGraph(String inPath,String offPath,boolean load_entire_graph) throws IOException {

        if(load_entire_graph){

            load_compressed_graph(inPath,offPath);
        }
    }

    /**
     * Return compressed graph
     * @return byte array compressed graph
     */
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

                offset = new int[n][4];

                while(sc2.hasNextLine()) {
                    for (i =0;i<n;i++) {


                        String[] line = sc2.nextLine().split("\\t");

                        offset[i][0] = Integer.parseInt(line[0]);
                        offset[i][1] = Integer.parseInt(line[1]);
                        offset[i][2] = Integer.parseInt(line[2]);
                        offset[i][3] = Integer.parseInt(line[3]);
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


    public int [] get_neighbours(int node){
        int [] neighbours,neighbours_array;
        byte [] toDecode ;
        int i,k;
        compressor = new EliasGamma();
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
        //System.out.println("NOOOODE  "+node);
        //System.out.println("TODECODE LEN "+toDecode.length);
        neighbours = compressor.dec(toDecode,offset[node][2],offset[node][3]);
        //System.out.println("NEIG LEN "+neighbours.length);
        if (neighbours.length>1) {
            neighbours_array = new int[neighbours.length-1];
            boolean empty = false;

            for (i = 1; i < neighbours.length; i++) {
                neighbours_array[i-1] = neighbours[i];
                //System.out.println("N "+neighbours[i]);
                if (neighbours[i] == -1){
                    empty = true;
                }
            }
            if (!empty){
                return (neighbours_array);
            }else{
                return (new int[0]);
            }
        }else{
            neighbours_array = new int[neighbours.length];
            boolean empty = false;
            //System.out.println("ICII");
            for (i = 0; i < neighbours.length; i++) {
                neighbours_array[i] = neighbours[i];
                //System.out.println("N "+neighbours[i]);
                if (neighbours[i] == -1){
                    empty = true;
                }
            }
            if (!empty){
                return (neighbours_array);
            }else{
                return (new int[0]);
            }
        }


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

                edge_list = get_neighbours(node);
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
                arcs += outdegree(offset[i][0]);
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

    public int outdegree(int node){
        int [] neig = get_neighbours(node);
        if(neig != null) {
            return (get_neighbours(node).length);
        }else{
            return 0;
        }
    }

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
