package it.bigdatalab.compression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

// Interfaccia per una futura lambda expression
//interface Difference {
//    public String subtract(int a, int b);
//}
public class DifferentialCompression {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.compression.DifferentialCompression");



    public DifferentialCompression(){
        // Da definire
    }


    private int bitwiseAdd(int i, int j)
    {
        int uncommonBitsFromBoth = i ^ j;
        int commonBitsFromBoth   = i & j;

        if (commonBitsFromBoth == 0)
            return uncommonBitsFromBoth;

        return bitwiseAdd (
                uncommonBitsFromBoth,
                commonBitsFromBoth << 1
        );
    }

    private int bitwiseSubtraction(int x,int y){

        while (y != 0){

            int borrow = (~x) & y;
            x = x ^ y;
            y = borrow << 1;
        }

        return x;
    }


    private static boolean isSorted(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                return false;
            }
        }
        return true;
    }
    public int[] encodeSequence(int [] sequence ){
        int[] encoded = new int[sequence.length];
        int j;
        // check if the sequence is sorted
        if(!(isSorted(sequence))){
            Arrays.sort(sequence);
        }
        j = 0;
        encoded[0] = sequence[0];
        while(j<(sequence.length-1)){
            //encoded[j+1] = sequence[j+1] - sequence[j];
            encoded[j+1] = bitwiseSubtraction(sequence[j+1],sequence[j]);
            j++;
        }
        return(encoded);
    }

    public int[] encodeSortedSequence(int [] sequence ){
        int[] encoded = new int[sequence.length];
        int j;
        j = 0;
        encoded[0] = sequence[0];
        while(j<(sequence.length-1)){
            //encoded[j+1] = sequence[j+1] - sequence[j];
            encoded[j+1] = bitwiseSubtraction(sequence[j+1],sequence[j]);
            j++;
        }
        return(encoded);
    }

    public int[] decodeSequence(int [] encoded){
        int [] decoded = new int[encoded.length];
        int j;
        j = 0;
        decoded[0] = encoded[0];
        while(j<(encoded.length-1)){
            //decoded[j+1] = encoded[j+1] + decoded[j];
            decoded[j+1] = bitwiseAdd(encoded[j+1],decoded[j]);
            j++;
        }
        return (decoded);
    }

    public int[][] encodeAdjList(int [][] matrix){
        int nodes,edges;
        int i,j;
        int [][] encoded;
        int [] row;

        nodes = matrix.length;
        encoded = new int [nodes][];
        logger.info("Starting decoding the Adjacency List " );
        for (i=0;i<nodes;i++){
            edges = matrix[i].length;
            row = new int[edges];
            for (j=0;j<edges;j++){
                row[j] = matrix[i][j];
            }
            encoded[i] = encodeSequence(row);
        }
        logger.info("Encoding completed " );

        return (encoded);
    }

    public int[][] decodeAdjList(int [][] encoded_matrix){
        int nodes,edges;
        int i,j;
        int [][] decoded;
        int [] row;

        nodes = encoded_matrix.length;
        decoded = new int[nodes][];
        logger.info("Starting Decoding the Adjacency List " );

        for (i =0 ;i<nodes;i++){
            edges = encoded_matrix[i].length;
            row = new int[edges];
            for (j=0;j<edges;j++){
                row[j] = encoded_matrix[i][j];
            }
            decoded[i] = decodeSequence(row);
        }
        logger.info("Decoding completed " );

        return(decoded);
    }
//    public int[][] encodeADJList(int [][] adj){
//
//    }
}
