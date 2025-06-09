package it.bigdatalab.structure.GraphProviders;

import it.bigdatalab.compression.VarIntGB.DifferentialCompression;
import it.bigdatalab.compression.VarIntGB.VarIntGB;
import it.bigdatalab.structure.AbstractGraphProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Note by niaBaldoni: this is the class CompressedGraph in branch compressionintegration
 * I changed its name and some of its field's names so that it fits the new GraphManager structure
 * Original logic should be untouched
 */

/**
 * Implementation of the compressed graph data structure
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */

public class VarIntGBGraphProvider extends AbstractGraphProvider {
    private long compressed_graph_size;
    private byte[] compressed_graph;
    private int[][] decoded_graph;
    private int[][] offset;
    private byte[] compressed_offset;
    private boolean in_memory;
    private boolean applyDifferential;
    private int nNodes = -1;
    private int nArcs = -1;
    private boolean isUndirected = false;
    private VarIntGB compressor;
    private DifferentialCompression gapCompressor;

    /**
     * Define a Compressed Graph instance loading the graph
     * @param inPath String input path
     * @param applyDifferential Boolean for differential compression
     * @throws IOException
     */
    public VarIntGBGraphProvider(String inPath, boolean applyDifferential) throws IOException {
        this.applyDifferential = applyDifferential;
        load_compressed_graph(inPath + ".txt", inPath + "_offset.txt");
    }

    /**
     * Return compressed graph
     * @return byte array compressed graph
     */
    public byte[] getCompressedGraph() {
        return compressed_graph;
    }

    /**
     * Loads the compressed file
     * @param inPath String input path file
     * @param offPath String offset path file
     * @throws IOException
     */
    public void load_compressed_graph(String inPath, String offPath) throws IOException {
        File file = new File(inPath);
        Path path = Paths.get(inPath);
        long size = Files.size(path);
        compressed_graph = new byte[(int) file.length()];

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
        try {
            Scanner sc = new Scanner(new BufferedReader(new FileReader(inPath)));

            try {
                n = 0;
                while (sc.hasNextLine()) {
                    n += 1;
                    sc.nextLine();
                }
                sc.close();

                Scanner sc2 = new Scanner(new BufferedReader(new FileReader(inPath)));
                offset = new int[n][2];

                while (sc2.hasNextLine()) {
                    for (i = 0; i < n; i++) {
                        String[] line = sc2.nextLine().split("\\t");

                        offset[i][0] = Integer.parseInt(line[0]);
                        offset[i][1] = Integer.parseInt(line[1]);
                    }
                }

                sc2.close();
            } finally {
                // Getting the number of nodes of the compressed graph
                nNodes =  offset.length;
            }
        } catch (FileNotFoundException ex) {

        }
    }

    /**
     * Return the neighbours of a node
     * @param node Int node
     * @return Int array of neighbours
     */
    @Override
    public int[] getNeighbors(int node) {
        int[] neighbours,neighbours_array;
        byte[] toDecode;
        int i,k;

        compressor = new VarIntGB();
        gapCompressor = new DifferentialCompression();

        if (node == offset[0][0]) {
            toDecode = new byte[offset[node][1]];
            k = 0;

            for (i = 0; i < offset[0][1]; i++) {
                toDecode[k] = compressed_graph[i];
                k+=1;
            }

        } else if (node ==  offset[offset.length-1][0]) {
            toDecode = new byte[compressed_graph.length -offset[offset.length-2][1]];
            k = 0;

            for (i = offset[offset.length-2][1]; i < compressed_graph.length; i++) {
                toDecode[k] = compressed_graph[i];
                k+=1;
            }

        } else {
            toDecode = new byte[offset[node][1] -offset[node-1][1]];
            k = 0;

            for (i = offset[node-1][1]; i < offset[node][1]; i++) {
                toDecode[k] = compressed_graph[i];
                k+=1;
            }
        }

        neighbours = compressor.dec(toDecode);

        if (applyDifferential) {
            neighbours = gapCompressor.decodeSequence(neighbours);
        }

        neighbours_array = new int[neighbours.length-1];
        for (i = 1; i < neighbours.length; i++) {
            neighbours_array[i-1] = neighbours[i];
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

    @Override
    public int numNodes() {
        if (nNodes == -1) {
            int i, j, nodes, node;
            int[] edge_list;
            nodes = 0;

            for (i = 0; i < offset.length; i++) {
                node = offset[i][0];

                edge_list = getNeighbors(node);
                for (j = 0; j < edge_list.length; j++) {
                    if (edge_list[j] > nodes) {
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
    @Override
    public int[] getNodes() {
        int[] nodes = new int[nNodes];
        int i, n;

        for (i = 0; i < nNodes; i++) {
            nodes[i] = offset[i][0];
        }
        return(nodes);
    }

    /**
     * Returns the number of arcs
     * @return Int
     */
    @Override
    public long numArcs() {
        if (nArcs == -1) {
            int arcs,i;
            arcs = 0;

            for(i = 0; i < offset.length; i++) {
                arcs += outdegree(offset[i][0]);
            }

            if (isUndirected) {
                nArcs = (1/2) * arcs;
            } else {
                nArcs = arcs;
            }

            return nArcs;
        } else {
            return nArcs;
        }
    }

    /**
     * Returns the outdegree of a node
     * @param node Integer node
     * @return Int number of neighbours
     */
    public int outdegree(int node) {
        int[] neig = getNeighbors(node);
        if (neig != null) {
            return (getNeighbors(node).length);
        } else {
            return 0;
        }
    }

    /**
     * Get the offset
     * @return 2D array
     */
    public int[][] get_offset() {
        return offset;
    }

    /**
     * Set the offset
     * @param off 2D array offset
     */
    public void set_offset(int[][] off){
        offset = off;
    }

    /**
     * Set compressed graph
     * @param cG byte array
     */
    public void set_compressed_graph(byte[] cG) {
        compressed_graph = cG;
    }

    public void writeGraphToFile(String outPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outPath))) {
            for (int i = 0; i < offset.length; i++) {
                int node = offset[i][0];
                int[] neighbors = getNeighbors(node);

                writer.write(String.valueOf(node));
                for (int neighbor : neighbors) {
                    writer.write("\t" + neighbor);
                }
                writer.newLine();
            }
        }
    }

}
