package it.bigdatalab.compression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.SysexMessage;
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

        if (commonBitsFromBoth == 0) {
            System.out.println("AHUAHU");
            System.out.println("I = "+i+ " J = "+j);
            return uncommonBitsFromBoth;
        }
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
        int[] targets;
        // check if the sequence is sorted
        if(sequence.length>1) {
            targets = Arrays.copyOfRange(sequence, 1, sequence.length);
            if(!(isSorted(targets))){
                Arrays.sort(targets);
            }
            j = 0;
            encoded[0] = sequence[0];
            while(j<(targets.length-1)){

                //encoded[j+1] = sequence[j+1] - sequence[j];
                encoded[j+1] = bitwiseSubtraction(targets[j+1],targets[j]);
                // System.out.println("sequence[j+1] = " +sequence[j+1] + "- sequence[j] =  "+sequence[j]+ " encoded[j+1] = "+encoded[j+1]);
                j++;
            }
        }else{
            encoded = sequence;
        }
        /*
        if(!(isSorted(sequence))){
            Arrays.sort(sequence);
        }

        j = 0;
        encoded[0] = sequence[0];
        while(j<(sequence.length-1)){

            //encoded[j+1] = sequence[j+1] - sequence[j];
            encoded[j+1] = bitwiseSubtraction(sequence[j+1],sequence[j]);
           // System.out.println("sequence[j+1] = " +sequence[j+1] + "- sequence[j] =  "+sequence[j]+ " encoded[j+1] = "+encoded[j+1]);
            j++;
        }

         */
        return(encoded);
    }

    public int[] encodeSortedSequence(int [] sequence ){
        int[] encoded = new int[sequence.length];
        int j;
        if(sequence.length<3){
            return(sequence);
        }else{
            encoded[0] = sequence[0];
            encoded[1] = sequence[1];
            for(j = 2;j<sequence.length;j++){
                encoded[j] = bitwiseSubtraction(sequence[j],sequence[j-1]);
            }
            return(encoded);
        }
    }

    public int[] decodeSequence(int []encoded){
        int [] decoded = new int[encoded.length];
        int j;
        if(encoded.length<3){
            return (encoded);
        }else{
            decoded[0] = encoded[0];
            decoded[1] = encoded[1];
            for(j = 2;j<encoded.length;j++){
                decoded[j] = bitwiseAdd(encoded[j],decoded[j-1]);
            }
            return (decoded);
        }
    }

    public int[] decodeSequence_dep(int [] encoded){
        int [] decoded = new int[encoded.length];
        int j;
        j = 0;
        decoded[0] = encoded[0];
        System.out.println("DEC SEQ = "+encoded[0]);
        while(j<(encoded.length-1)){
            //decoded[j+1] = encoded[j+1] + decoded[j];
            decoded[j+1] = bitwiseAdd(encoded[j+1],decoded[j]);
            System.out.println("ADDIZIONE "+encoded[j+1] +" + "+decoded[j] + " = "+decoded[j+1]);
            j++;
        }
        for(int i = 0;i<decoded.length;i++){
            System.out.println("DECODED ["+i+"]= "+ decoded[i]);
        }
        return (decoded);
    }
    public int [][] ecnodeAdjList(int [][] matrix){
        int [][] encoded;
        int [] edgelist;
        int i,j;
        logger.info("Starting encoding the Adjacency List " );
        encoded = new int[matrix.length][];
        for (i = 0;i<matrix.length;i++){
            edgelist = new int[matrix[i].length];
            for(j=0;j<matrix[i].length;j++){
                edgelist[j] = matrix[i][j];

            encoded[i] = encodeSortedSequence(edgelist);
            }
        }
        return (encoded);
    }

    public int [][] decodeAdjList(int [][] matrix){
        int [][] decoded;
        int [] edgelist;
        int i,j;
        logger.info("Starting encoding the Adjacency List " );
        decoded = new int[matrix.length][];
        for (i = 0;i<matrix.length;i++){
            edgelist = new int[matrix[i].length];
            for(j=0;j<matrix[i].length;j++){
                edgelist[j] = matrix[i][j];

                decoded[i] = decodeSequence(edgelist);
            }
        }
        return (decoded);
    }
    public int[][] encodeAdjList_dep(int [][] matrix){
        int nodes,edges;
        int i,j,k;
        int [][] encoded;
        int [] row, column;
        int [] row_enc,column_enc,row_enc_b;
        int [] empty_set;
        empty_set = new int[1];
        nodes = matrix.length;
        encoded = new int [nodes][];
        column = new int [nodes];

        logger.info("Starting encoding the Adjacency List " );
        for (i=0;i<nodes;i++){
            edges = matrix[i].length;
            row = new int[edges-1];
            column[i] = matrix[i][0];
            if(row.length >1) {
                for (j = 1; j < edges; j++) {
                    row[j - 1] = matrix[i][j];
                }
                row_enc = encodeSequence(row);
                row_enc_b = new int[edges];
                row_enc_b[0] = 0;
                for (k = 1; k < edges; k++) {
                    row_enc_b[k] = row_enc[k - 1];
                }
                encoded[i] = row_enc_b;
            }
            else if(row.length == 1){
                row_enc = new int[2];
                row_enc[0] = 0;
                row_enc[1] = matrix[i][1];
                encoded[i] = row_enc;
            }
            else{
                encoded[i] =empty_set ;
            }
        }
        column_enc = encodeSequence(column);
        for (i = 0; i< nodes; i++){
            encoded[i][0] = column_enc[i];
        }
        logger.info("Encoding completed " );
        return (encoded);
    }

    public int[][] decodeAdjList_dep(int [][] encoded_matrix){
        int nodes,edges;
        int i,j,k;
        int [][] decoded;
        int [] row,column,decoded_row,decoded_column,decoded_b;

        nodes = encoded_matrix.length;
        decoded = new int[nodes][];
        column = new int[nodes];
        logger.info("Starting Decoding the Adjacency List " );

        for (i =0 ;i<nodes;i++) {
            edges = encoded_matrix[i].length;
            row = new int[edges - 1];
            column[i] = encoded_matrix[i][0];
            if (row.length > 1) {
                for (j = 1; j < edges; j++) {
                    row[j - 1] = encoded_matrix[i][j];
                }
                decoded_row = decodeSequence(row);
                decoded_b = new int[edges];
                decoded_b[0] = 0;
                for (k = 1; k < edges; k++) {
                    decoded_b[k] = decoded_row[k - 1];
                }
                decoded[i] = decoded_b;
            } else {
                decoded_b = new int[1];
                decoded_b[0] = 0;
                decoded[i] = decoded_b;
            }
        }
        decoded_column = decodeSequence(column);
        for(i = 0;i<nodes;i++){
            decoded[i][0] = decoded_column[i];
        }

        logger.info("Decoding completed " );

        return(decoded);
    }

}
