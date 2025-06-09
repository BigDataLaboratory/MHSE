package it.bigdatalab.structure.GraphProviders;

import it.bigdatalab.compression.EliasFano.EliasFano;
import it.bigdatalab.structure.AbstractGraphProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Note by niaBaldoni: this is the class CompressedEliasFanoGraph in branch compressionintegration
 * I changed its name and some of its field's names so that it fits the new GraphManager structure
 * Original logic should be untouched
 */

public class EliasFanoGraphProvider extends AbstractGraphProvider {
    private long compressed_graph_size;
    private byte[] compressed_graph;
    private int[][] decoded_graph;
    private int[][] offset;
    private EliasFano compressor;
    private boolean isUndirected = false;
    private int nNodes = -1;
    private long nArcs = -1;

    public EliasFanoGraphProvider(String inPath) throws IOException {
        load_compressed_graph(inPath + ".txt", inPath + "_offset.txt");
    }

    /**
     * Return compressed graph
     * @return byte array compressed graph
     */
    public byte[] getCompressed_graph() {
        return compressed_graph;
    }

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

    public void load_offset(String inPath) throws IOException {
        int n, i;

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

                offset = new int[n][4];

                while(sc2.hasNextLine()) {
                    for (i = 0; i < n; i++) {


                        String[] line = sc2.nextLine().split("\\t");

                        offset[i][0] = Integer.parseInt(line[0]);
                        offset[i][1] = Integer.parseInt(line[1]);
                        offset[i][2] = Integer.parseInt(line[2]);
                        offset[i][3] = Integer.parseInt(line[3]);
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

    @Override
    public int[] getNeighbors(int node) {
        int[] neighbours, neighbours_array;
        byte[] toDecode ;
        int i, k;

        compressor = new EliasFano();

        if (node == offset[0][0]) {
            toDecode = new byte[offset[node][1]];
            k = 0;
            for (i = 0; i < offset[0][1]; i++) {
                toDecode[k] = compressed_graph[i];
                k += 1;
            }

        } else if (node ==  offset[offset.length-1][0]) {
            toDecode = new byte[compressed_graph.length - offset[offset.length-2][1]];
            k = 0;
            for (i = offset[offset.length-2][1]; i < compressed_graph.length; i++) {
                toDecode[k] = compressed_graph[i];
                k += 1;
            }

        } else {

            toDecode = new byte[offset[node][1] - offset[node-1][1]];
            k = 0;
            for (i = offset[node-1][1]; i < offset[node][1]; i++) {
                toDecode[k] = compressed_graph[i];
                k += 1;
            }
        }

        neighbours = compressor.dec(toDecode, offset[node][2], offset[node][3]);

        if (neighbours.length > 1) {
            neighbours_array = new int[neighbours.length-1];
            boolean empty = false;

            for (i = 1; i < neighbours.length; i++) {
                neighbours_array[i-1] = neighbours[i];

                if (neighbours[i] == -1){
                    empty = true;
                }
            }
            if (!empty){
                return neighbours_array;
            } else {
                return new int[0];
            }

        } else {
            neighbours_array = new int[neighbours.length];
            boolean empty = false;

            for (i = 0; i < neighbours.length; i++) {
                neighbours_array[i] = neighbours[i];
                if (neighbours[i] == -1){
                    empty = true;
                }
            }
            if (!empty) {
                return neighbours_array;
            } else {
                return (new int[0]);
            }
        }
    }

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

                for(j = 0; j < edge_list.length; j++) {
                    if (edge_list[j] >nodes) {
                        nodes = edge_list[j];
                    }
                }
            }
            nNodes = nodes;
        }
        return nNodes;
    }

    @Override
    public int[] getNodes() {
        int[] nodes = new int[nNodes];
        int i, n;

        for (i = 0; i < nNodes; i++) {
            nodes[i] = offset[i][0];
        }
        return(nodes);
    }

    @Override
    public long numArcs() {
        if (nArcs == -1) {
            long arcs = 0;
            int i;
            for (i = 0; i < offset.length; i++) {
                arcs += outdegree(offset[i][0]);
            }
            if (isUndirected) {
                nArcs = (1 / 2) * arcs;
            } else {
                nArcs = arcs;
            }
        }

        return nArcs;
    }

    public int outdegree(int node) {
        int[] neig = getNeighbors(node);
        if (neig != null) {
            return (getNeighbors(node).length);
        } else {
            return 0;
        }
    }

    public int[][] get_offset() {
        return (offset);
    }

    public void set_offset(int[][] off) {
        offset = off;
    }

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
