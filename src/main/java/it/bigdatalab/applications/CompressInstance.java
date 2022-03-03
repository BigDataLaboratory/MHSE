package it.bigdatalab.applications;
import it.bigdatalab.compression.EliasGamma;
import it.bigdatalab.compression.GroupVarInt;
import it.bigdatalab.compression.DifferentialCompression;
import it.bigdatalab.structure.CompressedEliasFanoGraph;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.UncompressedGraph;
import it.bigdatalab.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public String separator;
    public boolean VarIntGB;
    public static boolean EliasFano;
    public static boolean d_gaps;
    public boolean transposed;
    private int[][] adjList;

    public CompressInstance() {
        initialize();
    }

    public void initialize() {
        inputFilePath = PropertiesManager.getPropertyIfNotEmpty("compressInstance.inputAdjListPath");
        outputFilePath = PropertiesManager.getPropertyIfNotEmpty("compressInstance.outputFolderPath");
        separator = PropertiesManager.getPropertyIfNotEmpty("compressInstance.separator");
        VarIntGB = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("compressInstance.VarintGB"));
        EliasFano = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("compressInstance.EliasFano"));
        d_gaps = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("compressInstance.GapCompression"));
        transposed = Boolean.parseBoolean((PropertiesManager.getPropertyIfNotEmpty("compressInstance.transposed")));
    }








    public void compress() throws FileNotFoundException {
        GroupVarInt VarintGB = new GroupVarInt();
        EliasGamma EliasF = new EliasGamma();
        DifferentialCompression diff = new DifferentialCompression();
        UncompressedGraph UGraph;
        UGraph = new UncompressedGraph();
        UGraph.load_graph(inputFilePath, separator);
        int[][] provaMat = UGraph.getGraph();
        int[][] differentialMatrix;
        String[] split = inputFilePath.split("/");
        String name = split[split.length - 1];
        if (VarIntGB){
                if (d_gaps) {
                    differentialMatrix = diff.ecnodeAdjList(provaMat);
                    VarintGB.encodeAdjListFlat(differentialMatrix, d_gaps);
                } else {
                    VarintGB.encodeAdjListFlat(provaMat, d_gaps);
                }
            VarintGB.saveEncoding(outputFilePath, name);

            if (transposed) {
                UGraph.transpose_graph();
                GroupVarInt VarintGBTransposed = new GroupVarInt();
                if (d_gaps) {
                    VarintGBTransposed.encodeAdjListFlat(diff.ecnodeAdjList(UGraph.getTGraph()), d_gaps);

                } else {
                    VarintGBTransposed.encodeAdjListFlat(UGraph.getTGraph(), d_gaps);
                }
                String[] splitTrans = name.split("[.]");
                String nameTrans = splitTrans[0] + "_transposed." + splitTrans[1];
                VarintGBTransposed.saveEncoding(outputFilePath, nameTrans);


            }
        }else if(EliasFano){
            if (d_gaps) {
                differentialMatrix = diff.ecnodeAdjList(provaMat);
                EliasF.encodeAdjListFlat(differentialMatrix, d_gaps);
            } else {
                EliasF.encodeAdjListFlat(provaMat, d_gaps);
            }
            //EliasF.saveEncoding(outputFilePath, name);

            if (transposed) {
                UGraph.transpose_graph();
                EliasGamma EliasFGBTransposed = new EliasGamma();
                if (d_gaps) {
                    EliasFGBTransposed.encodeAdjListFlat(diff.ecnodeAdjList(UGraph.getTGraph()), d_gaps);

                } else {
                    EliasFGBTransposed.encodeAdjListFlat(UGraph.getTGraph(), d_gaps);
                }
                String[] splitTrans = name.split("[.]");
                String nameTrans = splitTrans[0] + "_transposed." + splitTrans[1];
                //EliasFGBTransposed.saveEncoding(outputFilePath, nameTrans);


            }
        }
        logger.info("Compressed instances and offsets saved in "+outputFilePath);

    }






    public static void compress_test_instances() throws FileNotFoundException {
       String inPath = "/home/antoniocruciani/IdeaProjects/MHSE/src/test/data/g_undirected_compressed_ef/";
        //String inPath = "/home/antoniocruciani/IdeaProjects/MHSE/src/test/data/g_directed_compressed_ef/";
        //String [] names = {"32-cycle.adjlist","32-cycle_transposed.adjlist","32-path.adjlist","32-path_transposed.adjlist",
         //"32in-star.adjlist","32in-star_transposed.adjlist","32out-star.adjlist","32out-star_transposed.adjlist","32t-path.adjlist"
        //,"32t-path_transposed.adjlist"};
        String [] names = {"32-complete.adjlist","32-complete_transposed.adjlist","32-cycle.adjlist","32-cycle_transposed.adjlist","32-wheel.adjlist",
       "32-wheel_transposed.adjlist"};
        EliasGamma EFano = new EliasGamma();
        GroupVarInt VarintGB = new GroupVarInt();
        DifferentialCompression diff = new DifferentialCompression();
        CompressedGraph Graph;
        UncompressedGraph UGraph;
        EliasFano = true;
        UGraph = new UncompressedGraph();
        for(int i = 0;i< names.length;i++){
            System.out.println("ENCODING "+names[i]);
            String inputFile = inPath+names[i];
            UGraph.load_graph(inputFile, "\t");
            int[][] provaMat = UGraph.getGraph();
            String[] split = inputFile.split("/");
            String name = split[split.length - 1];
            if(EliasFano == false) {
                if (d_gaps) {
                    VarintGB.encodeAdjListFlat(diff.ecnodeAdjList(provaMat), false);
                } else {
                    VarintGB.encodeAdjListFlat(provaMat, false);

                }
                VarintGB.saveEncoding(inPath, name);
            }else{

                    EFano.encodeAdjListFlat(provaMat, false);

                }
                EFano.saveEncoding(inPath, name);
                logger.info("Compressed instances and offsets saved in "+inPath);

        }









    }

    public void test_elias_gamma() throws IOException {
        EliasGamma Ecomp = new EliasGamma();
        CompressedEliasFanoGraph G;
        int[][] adjtest = {{100, 2000, 4353859}, {4, 5, 6},{9,10}};
        Ecomp.encodeAdjListFlat(adjtest, false);
        byte [] enc = Ecomp.getCompressedAdjListFlat();
        int[][] off = Ecomp.getOffset();
        for (int i = 0; i < off.length; i++) {
            for (int j = 0; j < off[i].length; j++) {
                System.out.println("OFF["+i+"]["+j+"] = "+off[i][j]);
            }
        }
        long [] toDecode = new long[off[0][1]];
        int k = 0;
        for(int i = 0;i<off[0][1];i++){
            //toDecode[k] = enc[i];
            k+=1;

        }
        /*
        int[] decomp = Ecomp.dec(toDecode,off[0][2],off[0][3]);
        for (int i = 0;i<decomp.length;i++){
            System.out.println("LINEA "+decomp[i]);
        }
        for (int i = 0; i < off.length; i++) {
            for (int j = 0; j < off[i].length; j++) {
                System.out.println("OFF["+i+"]["+j+"] = "+off[i][j]);
            }
        }

         */
        String IN = "/home/antoniocruciani/Desktop/Research/MHSE/TESTVGB/32in-star.adjlist_elias_.txt";
        String OFFIN = "/home/antoniocruciani/Desktop/Research/MHSE/TESTVGB/32in-star.adjlist_offset_elias.txt";
        String OT = "/home/antoniocruciani/Desktop/Research/MHSE/TESTVGB/";
        UncompressedGraph UGraph;
        EliasGamma EFano = new EliasGamma();
        UGraph = new UncompressedGraph();

        int[][] provaMat = UGraph.getGraph();
        System.out.println(IN);
        System.out.println("OFFSET "+OFFIN);
        CompressedEliasFanoGraph eGraph = new CompressedEliasFanoGraph(IN,OFFIN,true);
        for (int o = 0;o<eGraph.numNodes();o++){
            int [] neig = eGraph.get_neighbours(eGraph.get_nodes()[o]);
            System.out.println("LEN NEIGH "+neig.length);
            System.out.println("LEN NODES "+eGraph.get_nodes().length);
            for (int u = 0 ; u<neig.length;u++){
                System.out.println("NODE "+eGraph.get_nodes()[o]+ " NEIG "+neig[u]);

            }
        }


    }


    public static void main(String[] args) throws IOException {
        CompressInstance t = new CompressInstance();

        //t.compress();
        //t.test_elias_gamma();
        t.compress_test_instances();

    }
}
