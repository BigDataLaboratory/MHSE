package it.bigdatalab.compression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.SysexMessage;
import java.util.Arrays;


public class DifferentialCompression {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.compression.DifferentialCompression");



    public DifferentialCompression(){

    }


    private int bitwiseAdd(int i, int j)
    {
        int uncommonBitsFromBoth = i ^ j;
        int commonBitsFromBoth   = i & j;

        if (commonBitsFromBoth == 0) {

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


}
