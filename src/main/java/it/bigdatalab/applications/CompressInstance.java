package it.bigdatalab.applications;
import com.google.errorprone.annotations.Var;
import it.bigdatalab.compression.GroupVarInt;
import it.bigdatalab.compression.DifferentialCompression;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.UncompressedGraph;
import it.bigdatalab.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.SysexMessage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class CompressInstance {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.UncompressedGraph");
    private String inputFilePath;
    public String outputFilePath;
    public boolean VarIntGB;
    public boolean d_gaps;
    private int[][] adjList;

    public CompressInstance() {
        initialize();
    }

    public void initialize() {
        inputFilePath = PropertiesManager.getPropertyIfNotEmpty("test.compressInstance.inputAdjListPath");
        outputFilePath = PropertiesManager.getPropertyIfNotEmpty("test.compressInstance.outputFolderPath");
        //outputFilePath = "/home/antoniocruciani/Desktop/TESTVGB/";
        VarIntGB = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("test.compressInstance.VarintGB"));
        d_gaps = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("test.compressInstance.differential"));
    }

    public void load_adjList() throws FileNotFoundException {
        // IDEA LISTA DI ADJ HA COME PRIMA RIGA IL NUMERO DI NODI
        // VA TESTATA
        // !!! TOGLIERE LA PRIMA RIGA CON LE INFORMAZIONI DEL GRAFO E TROVARE UN MODO PER LEGGERE TUTTA LA LISTA
        Scanner sc;
        String[] line;
        int[] edge_list;
        int n, m, i, j;
        logger.info("Loading Adj List ");
        sc = new Scanner(new BufferedReader(new FileReader(inputFilePath)));
        line = sc.nextLine().trim().split("\t");
        n = Integer.parseInt(line[0]);
        adjList = new int[n][];
        while (sc.hasNextLine()) {
            for (i = 0; i < n; i++) {
                line = sc.nextLine().trim().split("\t");
                m = line.length;
                edge_list = new int[m];
                for (j = 0; j < m; j++) {
                    edge_list[j] = Integer.parseInt(line[j]);
                }
                adjList[i] = edge_list;
            }
        }
        logger.info("Adj List loaded");
    }

    public void encode_adjList() {
        GroupVarInt VarintGB = new GroupVarInt();
        DifferentialCompression DiffCompr = new DifferentialCompression();

        VarintGB.encodeAdjList(DiffCompr.encodeAdjList(adjList), false);

    }

    public static void test_compression_matrix_differential(int[][] test_matrix) {
        int nodes, edges;
        int i, j;
        int[][] converted;
        int[][] decoded;
        int[] row;
        int[] comp_row;
        DifferentialCompression differential = new DifferentialCompression();
        System.out.println("DIFFERENTIAL TEST ON MATRIX");

        nodes = test_matrix.length;
        converted = new int[nodes][];
        decoded = new int[nodes][];

        for (i = 0; i < nodes; i++) {
            edges = test_matrix[i].length;
            row = new int[edges];
            for (j = 0; j < edges; j++) {
                row[j] = test_matrix[i][j];
            }
            converted[i] = differential.encodeSequence(row);
        }
        for (i = 0; i < nodes; i++) {
            edges = converted[i].length;
            comp_row = new int[edges];
            for (j = 0; j < edges; j++) {
                comp_row[j] = converted[i][j];
            }
            decoded[i] = differential.decodeSequence(comp_row);
        }

        for (i = 0; i < nodes; i++) {
            edges = test_matrix[i].length;
            for (j = 0; j < edges; j++) {
                System.out.println(test_matrix[i][j] + "    " + decoded[i][j]);
                if (test_matrix[i][j] != decoded[i][j]) {
                    System.out.println("ERRORE MISMATCH IN RIGA " + i + " COLONNA " + j + " ORIGINAL = " + test_matrix[i][j] + " DECODED " + decoded[i][j]);
                    System.exit(1);
                }
            }
        }
        System.out.println("DIFFERENTIAL TEST ON CORRECTLY COMPLETED");


    }

    public static void test_compression_matrix_varintGB(int[][] test_matrix) {
        int nodes, edges;
        int i, j;
        byte[][] converted;
        int[][] decoded;
        int[] row;
        byte[] comp_row;
        GroupVarInt VarintGB = new GroupVarInt();
        System.out.println("VarintGB TEST ON MATRIX");


        nodes = test_matrix.length;
        converted = new byte[nodes][];
        decoded = new int[nodes][];

        for (i = 0; i < nodes; i++) {
            edges = test_matrix[i].length;
            row = new int[edges];
            for (j = 0; j < edges; j++) {
                row[j] = test_matrix[i][j];
            }
            converted[i] = VarintGB.listEncoding(row);
        }
        for (i = 0; i < nodes; i++) {
            edges = converted[i].length;
            comp_row = new byte[edges];
            for (j = 0; j < edges; j++) {
                comp_row[j] = converted[i][j];
            }
            decoded[i] = VarintGB.decode(comp_row);
        }

        for (i = 0; i < nodes; i++) {
            edges = test_matrix[i].length;
            for (j = 0; j < edges; j++) {
                System.out.println(test_matrix[i][j] + "    " + decoded[i][j]);
                if (test_matrix[i][j] != decoded[i][j]) {
                    System.out.println("ERRORE MISMATCH IN RIGA " + i + " COLONNA " + j + " ORIGINAL = " + test_matrix[i][j] + " DECODED " + decoded[i][j]);
                    System.exit(1);
                }
            }
        }
        System.out.println("VarintGB TEST CORRECTLY COMPLETED");


    }


    public void test_compression(int test_number) {
        Random rand = new Random();
        GroupVarInt VarintGB = new GroupVarInt();
        DifferentialCompression diff = new DifferentialCompression();

        int i, j;
        int array_size;

        for (i = 0; i < test_number; i++) {
            System.out.println("TEST NUMBER " + i);
            // random size
            array_size = rand.nextInt(500) + 1;
            int[] test_list = new int[array_size];
            // populating array with random number
            for (j = 0; j < array_size; j++) {
                test_list[j] = rand.nextInt(65535);
            }
            Arrays.sort(test_list);
            System.out.println("DIFFERENTIAL TEST");
            int[] differential_test = diff.encodeSequence(test_list);
            int[] differential_decompression_test = diff.decodeSequence(differential_test);

            // checking the integrety of the data

            for (j = 0; j < array_size; j++) {
                if (test_list[j] != differential_decompression_test[j]) {
                    System.out.println("ERROR DIFFERENTIAL Mismach at index " + j + " real value:" + test_list[j] + " Decompressed " + differential_decompression_test[j]);
                    System.exit(1);
                }
            }
            System.out.println(" DIFFERENTIAL OK");

            // Testing the group varint
            System.out.println("VARINTGB");
            //System.out.println("LUNGHEZZA SAMPLE "+ test_list.length);
            byte[] group_compression = VarintGB.listEncoding(test_list);
            //System.out.println("LISTA ENCODED");
            int[] group_decompression = VarintGB.decode(group_compression);
            //System.out.println("LISTA DECODED");
            //System.out.println("TEST SIZE " +test_list.length);
            //System.out.println("ZIZE "+group_decompression.length);
            // checking the integrety of the data

            for (j = 0; j < array_size; j++) {


                if (test_list[j] != group_decompression[j]) {
                    System.out.println("ERROR GROUP VARINT Mismach at index " + j + " real value:" + test_list[j] + " Decompressed " + group_decompression[j]);
                    for (int k = 0; k < array_size; k++) {
                        System.out.println("ORIGINALE " + test_list[k] + " DECOMPRESSO " + group_decompression[k]);
                        System.out.println("Lunghezza array " + array_size);
                    }
                    System.exit(1);
                }
            }
            System.out.println("VARINT OK");


            System.out.println("TEST " + i + " completed");

        }


    }

    public void compress() throws FileNotFoundException {
        GroupVarInt VarintGB = new GroupVarInt();
        DifferentialCompression diff = new DifferentialCompression();
        CompressedGraph Graph;
        UncompressedGraph UGraph;
        UGraph = new UncompressedGraph();
        UGraph.load_graph(inputFilePath, "\t");
        int[][] provaMat = UGraph.getGraph();
        String[] split = inputFilePath.split("/");
        String name = split[split.length - 1];
        VarintGB.encodeAdjList(diff.encodeAdjList(provaMat), d_gaps);
        VarintGB.saveEncoding(outputFilePath,name,VarintGB.getCompressedAdjList(),VarintGB.getCompressedOffset());

    }
    public void prova_lista() throws IOException {
        CompressedGraph Graph ;
        String name = "/home/antoniocruciani/Desktop/TESTVGB/compressed/uk-2007-05@100000.adjlist.txt";
        String off = "/home/antoniocruciani/Desktop/TESTVGB/compressed/uk-2007-05@100000.adjlist_offset.txt";
        Graph = new CompressedGraph(name,off,true);
        int [] nei = Graph.get_neighbours(15,true);
        System.out.println("Vicini nodo 15");
        for(int y = 0;y<nei.length;y++){
            System.out.println(nei[y]);
        }

    }

    public static void main(String[] args) throws IOException {
        CompressInstance t = new CompressInstance();
        t.compress();
        //t.prova_lista();

    }
}
//
//        CompressInstance tests = new CompressInstance();
//        GroupVarInt VarintGB = new GroupVarInt();
//        DifferentialCompression diff = new DifferentialCompression();
//        CompressedGraph Graph;
//        UncompressedGraph UGraph;
//        //tests.test_compression(1000);
//        /*
//        int [][] provaMat = {
//                {1,2,89,99,256,546},
//                {70,91,100,400},
//                {38,90,129}
//                           };
//
//         */
//        //int [][] provaMat = {{0,2,3,5,6,10,11,12,14,15,16,17,20,24,26,28,30,32,33,34,35,37,38,39,40,42,49,50,54,56,57,58,59,68,71,75,78,79,80,82,83,85,87,88,91,94,97,98,99},{1,2,3,4,5,9,12,14,16,20,21,22,23,24,26,27,28,29,31,32,33,37,38,40,43,44,45,46,47,48,49,52,58,59,64,66,69,70,72,74,75,76,78,79,80,82,85,86,87,88,89,90,91,92,95,96,97,99},{2,3,4,7,12,16,19,21,22,24,25,27,28,29,30,31,32,34,36,38,39,40,42,43,45,49,52,53,60,63,65,66,67,70,71,73,74,75,76,83,84,85,88,92,99},{3,4,6,7,10,12,13,14,16,20,22,24,25,27,28,30,31,32,35,37,38,39,43,44,45,46,47,50,53,54,56,58,62,63,64,66,71,72,76,79,81,83,86,91,94,95,97},{4,7,8,9,11,14,17,19,25,26,28,29,31,32,34,37,40,42,43,44,47,49,50,52,53,54,55,56,57,58,59,61,62,67,69,70,71,73,74,77,79,80,81,84,85,87,88,89,96,99},{5,7,11,12,13,14,15,19,21,25,28,29,31,33,34,38,39,40,45,49,51,54,56,58,60,61,63,66,67,69,71,73,75,79,80,83,86,88,89,91,95,97,98},{6,7,8,11,16,17,19,22,24,27,33,34,35,36,37,38,39,42,44,46,48,50,52,55,58,59,60,61,62,63,64,67,69,70,72,75,77,78,79,82,83,84,85,86,88,89,90,91,92,93,95},{7,9,12,14,15,19,21,22,24,27,29,30,34,35,37,38,41,42,43,46,50,51,52,53,56,57,61,62,66,68,71,74,75,76,78,81,84,85,87,89,90,97,98},{8,9,13,16,19,23,25,35,36,39,40,41,44,51,52,53,56,57,58,59,60,61,63,64,65,69,71,72,74,78,79,80,82,83,86,87,88,92,95,96,97,98},{9,10,11,13,17,18,21,22,23,24,27,28,29,32,33,34,35,39,41,45,46,49,51,52,53,55,59,60,61,62,65,66,67,69,70,71,75,77,78,79,80,81,82,83,84,88,89,90,91,93,94,96},{10,12,13,16,18,19,21,22,23,24,25,26,28,30,31,33,36,37,38,46,48,49,53,55,57,60,61,63,66,67,68,70,72,74,75,78,79,82,84,87,90,93,95,99},{11,12,13,14,15,21,22,24,25,26,28,29,31,32,33,34,36,38,39,40,42,43,44,46,47,48,49,51,55,58,61,62,63,64,65,68,70,75,76,77,78,82,84,87,88,89,92,96,97,98},{12,16,17,18,22,24,25,28,31,34,35,36,37,39,40,42,43,45,46,47,49,53,56,62,64,65,70,72,74,78,79,82,83,86,87,89,90,91,92,93,94,95,99},{13,14,15,20,22,23,26,27,30,34,35,38,40,43,44,45,46,48,50,51,54,58,61,62,67,69,71,73,74,76,78,80,85,86,88,89,92,96,97},{14,15,19,20,24,28,31,32,33,34,36,37,38,39,41,42,43,47,51,54,55,57,60,61,64,68,69,74,79,80,81,82,84,85,86,88,89,91,92,93,94,98,99},{15,16,19,24,25,27,30,35,36,37,38,40,42,44,48,49,51,52,53,54,57,58,59,60,61,62,63,64,67,69,71,72,77,78,80,81,84,85,87,93,97,98,99},{16,20,21,22,23,25,26,27,30,31,35,39,41,43,45,46,47,48,49,52,53,54,56,61,62,63,66,67,68,69,71,72,73,75,77,81,82,83,84,86,99},{17,18,21,22,25,29,30,31,32,33,34,36,38,42,43,45,47,49,50,51,53,56,57,59,60,61,65,66,68,71,73,74,79,82,84,85,86,87,89,91,92,93,95,96,97,99},{18,21,22,23,25,28,29,30,31,32,35,38,39,40,42,43,44,46,50,52,53,56,57,58,59,64,65,68,70,71,73,74,75,77,80,82,85,88,89,93,94,95,98,99},{19,21,23,24,25,27,29,30,33,34,37,41,43,45,46,48,49,55,57,58,60,63,64,69,75,76,77,78,79,80,84,85,86,87,88,89,90,91,93,94,95,96,97,99},{20,21,22,25,26,28,33,35,37,39,40,44,48,50,53,57,61,64,67,70,71,72,75,77,79,82,85,89,92,94,95,96,98,99},{21,22,28,30,31,32,33,34,35,36,38,44,47,48,49,52,53,54,55,57,58,59,61,64,65,69,75,76,77,78,80,81,85,87,88,90,91,93,94,95,97,99},{22,23,25,26,28,30,34,38,39,41,42,44,46,50,51,54,57,59,60,62,63,64,69,70,72,74,75,76,77,79,80,82,83,84,85,86,94,95,97},{23,26,27,30,31,32,33,35,36,42,43,44,46,47,48,49,51,54,56,57,60,62,64,65,66,67,68,70,72,75,76,85,86,88,89,90,92,93},{24,25,28,32,34,35,37,40,41,42,44,45,46,48,51,58,59,60,61,62,63,66,67,74,75,76,77,79,81,85,92,93,95,97,99},{25,26,28,30,32,33,36,42,45,47,48,52,54,57,59,60,61,63,64,66,67,68,71,73,74,76,82,83,84,87,91,92,93,94,95},{26,27,28,31,35,38,42,43,46,51,53,54,58,60,61,62,64,65,77,79,80,81,85,86,88,89,96,98},{27,28,29,30,32,34,35,37,40,43,45,47,48,52,57,60,61,68,69,71,72,76,77,78,79,80,83,84,91,92,93,94,95,97},{28,29,30,37,48,51,52,53,54,55,58,59,60,62,63,65,66,69,70,72,73,74,75,79,80,83,84,86,90,91,93,94,95,98},{29,30,31,32,33,36,37,38,41,42,43,46,47,49,50,51,53,54,57,58,59,60,64,65,68,69,70,74,77,81,82,84,85,86,88,90,91,93,94,97},{30,31,35,36,39,41,45,52,54,55,57,59,61,62,64,65,66,68,70,71,72,73,75,77,78,81,88,89,93,95,97,98},{31,35,36,37,38,39,44,45,46,47,48,49,51,55,56,57,58,62,66,69,71,73,75,76,78,79,80,82,84,85,87,89,90,92,95,97,98},{32,33,35,36,37,38,39,42,44,45,47,49,50,56,57,61,62,63,64,72,75,76,78,80,81,84,89,90,91,92,93,94,96,97,98,99},{33,34,36,38,39,41,42,43,47,50,53,54,55,56,58,59,60,61,63,66,70,71,73,74,76,78,79,80,83,84,85,87,88,89,90,91,92,95,96,99},{34,37,38,40,44,46,47,52,53,55,56,58,59,60,62,66,67,68,69,70,71,73,77,79,80,81,82,83,85,86,88,92,94,98,99},{35,39,44,46,50,51,52,53,54,55,56,59,63,64,65,66,67,69,70,71,75,77,79,84,85,89,90,91,92,93,94,96,98,99},{36,38,39,41,43,45,46,50,53,57,58,62,64,66,75,76,82,84,85,86,87,88,92,93,95},{37,40,42,44,46,48,53,54,55,57,58,61,62,63,67,70,73,74,75,76,78,79,80,81,82,84,85,90,91,94,95,96,97},{38,39,40,41,42,44,47,48,50,57,58,59,61,62,66,67,68,72,73,75,76,77,79,80,81,82,85,87,89,95},{39,42,43,44,46,48,49,50,52,55,57,60,61,62,69,70,71,73,76,79,80,81,84,86,94,96,97},{40,41,42,43,44,48,50,51,52,54,55,57,61,62,64,66,68,69,70,71,74,80,81,82,85,87,89,90,91,92,93,94,95,98,99},{41,43,44,46,50,52,53,55,56,59,60,63,64,66,67,70,74,75,76,78,79,85,86,88,91,92,93,94,96,97},{42,43,47,48,49,50,51,52,54,56,59,60,62,63,64,65,66,68,69,71,72,75,76,77,78,79,80,82,83,85,88,89,90,91},{43,44,45,47,48,49,50,51,52,53,56,57,58,59,60,61,62,68,71,72,75,76,79,80,82,83,87,88,89,93,95,96,97,99},{44,45,51,54,55,56,57,58,59,62,66,67,69,70,71,72,74,76,77,80,83,84,87,88,91,94,95,96,97,99},{45,46,47,49,53,56,57,59,60,62,64,68,69,71,75,76,77,78,79,80,85,88,89,91,94,96,97},{46,50,55,56,57,61,63,64,65,69,70,73,74,75,76,79,86,87,88,92,95,97,99},{47,49,50,51,54,56,57,59,60,61,62,65,67,68,72,73,74,76,79,80,81,87,92,95,96,98},{48,51,52,53,59,60,62,64,66,69,71,73,74,75,76,78,82,83,89,90,93,94,96,97,99},{49,51,53,54,57,60,61,64,65,66,67,69,70,72,74,75,77,80,83,84,86,87,89,91,95,97,98},{50,51,53,55,57,58,59,62,63,64,66,67,68,72,74,75,76,80,81,82,86,88,89,90,92,93,94,95,96,97,98},{51,52,61,63,72,79,80,81,85,86,87,88,91,92,93,95,96,98,99},{52,54,55,59,61,62,63,64,74,77,82,83,85,87,88,90,93,94,96,98},{53,55,56,57,60,61,62,66,68,69,70,71,72,75,77,78,79,80,84,85,87,88,90,91,93,94,95,98,99},{54,55,57,58,60,61,62,64,67,68,71,75,76,81,84,88,89,92,93,95},{55,57,61,62,65,66,68,70,71,74,75,77,80,81,82,83,84,89,90,91,92,95,96,97},{56,57,60,61,62,63,65,67,69,73,74,75,79,80,85,87,92,94,95,96,99},{57,59,61,65,66,67,70,71,73,74,75,77,79,80,82,85,87,89,90,93,94,95,96,98},{58,59,60,61,63,65,67,69,71,73,75,76,80,81,82,83,84,87,90,91,92,93,95,96,98,99},{59,60,61,63,68,70,71,72,73,75,76,77,78,79,80,82,83,84,86,89,90,92,93,97},{60,61,62,63,65,67,69,70,71,72,74,76,77,78,79,81,84,87,89,90,91,93,96,97,98,99},{61,63,64,66,67,69,76,77,78,79,80,81,86,88,93,96,97},{62,63,65,66,71,72,73,74,78,81,83,85,86,87,89,92,94,95,98,99},{63,66,70,72,73,77,78,79,80,81,83,84,85,86,88,91,95,97,98},{64,67,68,69,72,73,74,76,77,80,82,83,84,88,90,91,93,94,96,98},{65,67,68,69,70,77,78,79,81,82,84,85,86,87,89,91,92,93,95},{66,67,69,70,71,72,73,75,76,78,82,83,84,86,91,92,93,94,95,96},{67,68,70,73,75,78,81,83,88,90,93,96},{68,69,72,73,76,77,82,83,84,85,86,87,88,89,92,93,95,96,97,99},{69,73,74,75,76,78,84,86,88,89,90,95,97,98},{70,71,73,74,77,80,82,84,85,86,89,90,92,97,98},{71,74,75,76,77,83,84,85,88,94,97,98,99},{72,73,75,81,82,84,86,87,88,91,93,95,96,98,99},{73,74,75,76,77,79,80,86,88,89,90,92,97,98},{74,75,76,77,78,79,80,82,84,85,87,88,90,91,92,94,95,98,99},{75,76,77,78,79,80,81,84,86,87,88,89,91,93,94,95,96,99},{76,77,80,81,83,86,88,89,91,93,94,95,96},{77,79,80,81,83,86,94,95,96,99},{78,79,80,81,83,84,86,89,90,91,95,96,97,98,99},{79,82,83,85,86,88,89,92,96,97,98,99},{80,81,82,83,85,90,92,93,94,95},{81,86,87,88,89,92,94,95,96,99},{82,84,85,86,91,93,94,97,99},{83,84,85,86,87,88,90,95,97,98},{84,86,90,91,93,94,96,97,98,99},{85,87,88,89,90,92,93,94,97},{86,87,90,95,96,97,98},{87,91,92,94,95,96,98,99},{88,90,91,92,93,94,95,96},{89,93,98,99},{90,91,95,98,99},{91,92,94,95},{92,93,95,96,98},{93,96,99},{94,98},{95,97},{96,97},{97,99},{98},{99}};
//        String outputFilePath= PropertiesManager.getPropertyIfNotEmpty("compressInstance.outputFolderPath");
//        String inputFilePath = PropertiesManager.getPropertyIfNotEmpty("compressInstance.inputAdjListPath");
//        String separator = PropertiesManager.getPropertyIfNotEmpty("compressInstance.separator");
//        String outPath = outputFilePath;
//
//        UGraph = new UncompressedGraph();
//        UGraph.load_graph(inputFilePath,"\t");
//        int [][] provaMat = UGraph.getGraph();
//        //CompressInstance.test_compression_matrix_differential(provaMat);
//
//        //CompressInstance.test_compression_matrix_varintGB(provaMat);
//
//        VarintGB.encodeAdjList(diff.encodeAdjList(provaMat),true);
//        //VarintGB.encodeAdjList(provaMat,false);
//        String[] split = inputFilePath.split("/");
//        String name = split[split.length-1];
//        VarintGB.saveEncoding(outPath,name,VarintGB.getCompressedAdjList(),VarintGB.getCompressedOffset());
//
          //Graph = new CompressedGraph(outPath + name+".txt",true);
//        Graph.load_offset(outPath+name+"_offset.txt");
//        Graph.load_compressed_graph(outPath+name+".txt");
//        byte [] grafo = Graph.getCompressed_graph();
//        //System.out.println("GRAFO COMPRESSO LEN "+grafo.length);
//        int[] decomp = VarintGB.decode(grafo);
        //System.out.println("LUNGHEZZA DECOMRPESSO "+decomp.length);
        /*
        for (int j = 0; j<decomp.length;j++){
            System.out.println(decomp[j]);
        }
        System.out.println("QUERY ");
        System.out.println("NODO 1");
        int [] neig = Graph.get_neighbours(1,true);
        for (int h = 0;h<neig.length;h++){
            System.out.println("Vicino "+neig[h]);
        }
        System.out.println("NODO 2");

        Graph.get_neighbours(38,true);
        System.out.println("NODO 3");

        Graph.get_neighbours(70,true);
        Graph.decode_graph();
        */

        //Graph.decode_graph();
        //UGraph.setGraph(Graph.getDecoded_graph());

//        int [] nei = Graph.get_neighbours(15,true);
//        System.out.println("Vicini nodo 15");
//        for(int y = 0;y<nei.length;y++){
//            System.out.println(nei[y]);
//        }
//    }
//}