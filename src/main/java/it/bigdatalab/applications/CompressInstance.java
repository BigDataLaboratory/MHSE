package it.bigdatalab.applications;
import it.bigdatalab.compression.GroupVarInt;
import it.bigdatalab.compression.DifferentialCompression;
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
        d_gaps = Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("compressInstance.GapCompression"));
        transposed = Boolean.parseBoolean((PropertiesManager.getPropertyIfNotEmpty("compressInstance.transposed")));
    }








    public void compress() throws FileNotFoundException {
        GroupVarInt VarintGB = new GroupVarInt();
        DifferentialCompression diff = new DifferentialCompression();
        UncompressedGraph UGraph;
        UGraph = new UncompressedGraph();
        UGraph.load_graph(inputFilePath, separator);
        int[][] provaMat = UGraph.getGraph();
        int [][] differentialMatrix;
        String[] split = inputFilePath.split("/");
        String name = split[split.length - 1];

        if(d_gaps){
            differentialMatrix = diff.ecnodeAdjList(provaMat);
            VarintGB.encodeAdjListFlat(differentialMatrix,d_gaps);
        }else {
            VarintGB.encodeAdjListFlat(provaMat, d_gaps);
        }
        VarintGB.saveEncoding(outputFilePath,name);

        if(transposed){
            UGraph.transpose_graph();
            GroupVarInt VarintGBTransposed = new GroupVarInt();
            if(d_gaps){
                VarintGBTransposed.encodeAdjListFlat(diff.ecnodeAdjList(UGraph.getTGraph()), d_gaps);

            }else {
                VarintGBTransposed.encodeAdjListFlat(UGraph.getTGraph(), d_gaps);
            }
            String[] splitTrans = name.split("[.]");
            String nameTrans = splitTrans[0]+"_transposed."+splitTrans[1];
            VarintGBTransposed.saveEncoding(outputFilePath,nameTrans);


        }
        logger.info("Compressed instances and offsets saved in "+outputFilePath);

    }






    public static void compress_test_instances() throws FileNotFoundException {
        String inPath = "/home/antoniocruciani/IdeaProjects/MHSE/src/test/data/g_undirected_compressed_dgaps/";
        //String inPath = "/home/antoniocruciani/IdeaProjects/MHSE/src/test/data/g_directed_compressed/";
        //String [] names = {"32-cycle.adjlist","32-cycle_transposed.adjlist","32-path.adjlist","32-path_transposed.adjlist",
        //"32in-star.adjlist","32in-star_transposed.adjlist","32out-star.adjlist","32out-star_transposed.adjlist","32t-path.adjlist"
        //,"32t-path_transposed.adjlist"};
        String [] names = {"32-complete.adjlist","32-complete_transposed.adjlist","32-cycle.adjlist","32-cycle_transposed.adjlist","32-wheel.adjlist",
       "32-wheel_transposed.adjlist"};
        GroupVarInt VarintGB = new GroupVarInt();
        DifferentialCompression diff = new DifferentialCompression();
        CompressedGraph Graph;
        UncompressedGraph UGraph;
        UGraph = new UncompressedGraph();
        for(int i = 0;i< names.length;i++){
            String inputFile = inPath+names[i];
            UGraph.load_graph(inputFile, "\t");
            int[][] provaMat = UGraph.getGraph();
            String[] split = inputFile.split("/");
            String name = split[split.length - 1];
            if(d_gaps){
                VarintGB.encodeAdjListFlat(diff.ecnodeAdjList(provaMat), false);
            }else{
                VarintGB.encodeAdjListFlat(provaMat, false);

            }
            VarintGB.saveEncoding(inPath,name);


        }




        logger.info("Compressed instances and offsets saved in "+inPath);

    }


    public static void main(String[] args) throws IOException {
        CompressInstance t = new CompressInstance();

        t.compress();

       // t.compress_test_instances();

    }
}
