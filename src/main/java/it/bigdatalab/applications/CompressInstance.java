package it.bigdatalab.applications;
import it.bigdatalab.compression.GroupVarInt;
import it.bigdatalab.compression.DifferentialCompression;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.utils.PropertiesManager;

import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class CompressInstance {
    public CompressInstance() {
        String inputFilePath = PropertiesManager.getPropertyIfNotEmpty("compressInstance.inputAdjListPath");
        String outputFilePath = PropertiesManager.getPropertyIfNotEmpty("compressInstance.outputFolderPath");


    }

    public static void test_compression_matrix_differential(int[][] test_matrix){
        int nodes,edges;
        int i,j;
        int [][] converted;
        int [][] decoded;
        int [] row;
        int [] comp_row;
        DifferentialCompression differential = new DifferentialCompression();
        System.out.println("DIFFERENTIAL TEST ON MATRIX");

        nodes = test_matrix.length;
        converted = new int [nodes][];
        decoded = new int[nodes][];

        for (i=0;i<nodes;i++){
            edges = test_matrix[i].length;
            row = new int[edges];
            for (j=0;j<edges;j++){
                row[j] = test_matrix[i][j];
            }
            converted[i] = differential.encodeSequence(row);
        }
        for (i =0 ;i<nodes;i++){
            edges = converted[i].length;
            comp_row = new int[edges];
            for (j=0;j<edges;j++){
                comp_row[j] = converted[i][j];
            }
            decoded[i] = differential.decodeSequence(comp_row);
        }

        for (i =0;i<nodes;i++){
            edges = test_matrix[i].length;
            for (j = 0;j<edges;j++){
                System.out.println(test_matrix[i][j]+ "    "+decoded[i][j]);
                if(test_matrix[i][j] != decoded[i][j]){
                    System.out.println("ERRORE MISMATCH IN RIGA "+i +" COLONNA "+j+" ORIGINAL = "+test_matrix[i][j] +" DECODED "+decoded[i][j]);
                    System.exit(1);
                }
            }
        }
        System.out.println("DIFFERENTIAL TEST ON CORRECTLY COMPLETED");


    }

    public static void test_compression_matrix_varintGB(int[][] test_matrix){
        int nodes,edges;
        int i,j;
        byte [][] converted;
        int [][] decoded;
        int [] row;
        byte [] comp_row;
        GroupVarInt VarintGB = new GroupVarInt();
        System.out.println("VarintGB TEST ON MATRIX");


        nodes = test_matrix.length;
        converted = new byte[nodes][];
        decoded = new int[nodes][];

        for (i=0;i<nodes;i++){
            edges = test_matrix[i].length;
            row = new int[edges];
            for (j=0;j<edges;j++){
                row[j] = test_matrix[i][j];
            }
            converted[i] = VarintGB.listEncoding(row);
        }
        for (i =0 ;i<nodes;i++){
            edges = converted[i].length;
            comp_row = new byte[edges];
            for (j=0;j<edges;j++){
                comp_row[j] = converted[i][j];
            }
            decoded[i] = VarintGB.decode(comp_row);
        }

        for (i =0;i<nodes;i++){
            edges = test_matrix[i].length;
            for (j = 0;j<edges;j++){
                System.out.println(test_matrix[i][j]+ "    "+decoded[i][j]);
                if(test_matrix[i][j] != decoded[i][j]){
                    System.out.println("ERRORE MISMATCH IN RIGA "+i +" COLONNA "+j+" ORIGINAL = "+test_matrix[i][j] +" DECODED "+decoded[i][j]);
                    System.exit(1);
                }
            }
        }
        System.out.println("VarintGB TEST CORRECTLY COMPLETED");


    }


    public void test_compression(int test_number){
        Random rand = new Random();
        GroupVarInt VarintGB = new GroupVarInt();
        DifferentialCompression diff = new DifferentialCompression();

        int i,j;
        int array_size;

        for (i = 0; i<test_number;i++){
            System.out.println("TEST NUMBER "+i);
            // random size
            array_size = rand.nextInt(500)+1;
            int [] test_list = new int[array_size];
            // populating array with random number
            for (j = 0 ; j<array_size;j++){
                test_list[j] = rand.nextInt(65535);
            }
            Arrays.sort(test_list);
            System.out.println("DIFFERENTIAL TEST");
            int [] differential_test = diff.encodeSequence(test_list);
            int [] differential_decompression_test = diff.decodeSequence(differential_test);

            // checking the integrety of the data

            for (j = 0; j<array_size; j++){
                if(test_list[j] != differential_decompression_test[j]){
                    System.out.println("ERROR DIFFERENTIAL Mismach at index "+j+" real value:"+test_list[j]+" Decompressed "+ differential_decompression_test[j]);
                    System.exit(1);
                }
            }
            System.out.println(" DIFFERENTIAL OK");

            // Testing the group varint
            System.out.println("VARINTGB");
            //System.out.println("LUNGHEZZA SAMPLE "+ test_list.length);
            byte [] group_compression = VarintGB.listEncoding(test_list);
            //System.out.println("LISTA ENCODED");
            int [] group_decompression = VarintGB.decode(group_compression);
            //System.out.println("LISTA DECODED");
            //System.out.println("TEST SIZE " +test_list.length);
            //System.out.println("ZIZE "+group_decompression.length);
            // checking the integrety of the data

            for (j = 0; j<array_size; j++){


                if(test_list[j] != group_decompression[j]){
                    System.out.println("ERROR GROUP VARINT Mismach at index "+j+" real value:"+test_list[j]+" Decompressed "+ group_decompression[j]);
                    for (int k= 0; k<array_size;k++){
                        System.out.println("ORIGINALE "+ test_list[k]+ " DECOMPRESSO "+group_decompression[k]);
                        System.out.println("Lunghezza array "+array_size);
                    }
                    System.exit(1);
                }
            }
            System.out.println("VARINT OK");



        System.out.println("TEST "+i + " completed");

        }


    }


    public static void main(String[] args) throws IOException {
        //CompressInstance prova = new CompressInstance();
        CompressInstance tests = new CompressInstance();
        GroupVarInt VarintGB = new GroupVarInt();
        DifferentialCompression diff = new DifferentialCompression();
        CompressedGraph Graph;

        //tests.test_compression(1000);
        int [][] provaMat = {
                {1,2,89,99,256,546},
                {70,91,100,400},
                {38,90,129}
                           };
        CompressInstance.test_compression_matrix_differential(provaMat);

        CompressInstance.test_compression_matrix_varintGB(provaMat);

        //VarintGB.encodeAdjList(diff.encodeAdjList(provaMat));
        VarintGB.encodeAdjList(provaMat);

        String outPath = "/home/antoniocruciani/Desktop/TESTVGB/";
        VarintGB.saveEncoding(outPath,"Test",VarintGB.getCompressedAdjList(),VarintGB.getOffset());

        Graph = new CompressedGraph(outPath + "Test.txt",true);
        Graph.load_offset(outPath+"Test_offset.txt");
        Graph.load_compressed_graph(outPath+"Test.txt");
        byte [] grafo = Graph.getCompressed_graph();
        System.out.println("GRAFO COMPRESSO LEN "+grafo.length);
        int[] decomp = VarintGB.decode(grafo);
        System.out.println("LUNGHEZZA DECOMRPESSO "+decomp.length);
        for (int j = 0; j<decomp.length;j++){
            System.out.println(decomp[j]);
        }
        System.out.println("QUERY ");
        System.out.println("NODO 1");
        int [] neig = Graph.get_neighbours(1);
        for (int h = 0;h<neig.length;h++){
            System.out.println("Vicino "+neig[h]);
        }
        System.out.println("NODO 2");

        Graph.get_neighbours(38);
        System.out.println("NODO 3");

        Graph.get_neighbours(70);
        Graph.decode_graph();



    }
}