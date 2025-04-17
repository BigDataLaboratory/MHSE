package it.bigdatalab.compression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.SysexMessage;
import java.util.Arrays;


/**
 * Implementation of gap compression algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class DifferentialCompression {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.compression.DifferentialCompression");


    /**
     * Creates a new Differential Compression instance
     */
    public DifferentialCompression(){

    }


    /**
     * Compute bitwise addition between two numbers
     * @param i
     * @param j
     * @return the sum i+j
     */
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

    /**
     * Compute bitwise difference between two numbers
     * @param x
     * @param y
     * @return the difference x-y
     */
    private int bitwiseSubtraction(int x,int y){

        while (y != 0){

            int borrow = (~x) & y;
            x = x ^ y;
            y = borrow << 1;
        }

        return x;
    }

    /**
     * Compute the difference gap compression of a sequence of integers
     * @param sequence Array of a sequence of integers (MUST BE SORTED IN INCREASING WAY)
     * @return encoded sequence
     */
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

    /**
     * Compute original sequence given the encoded sequence as input
     * @param encoded Array of a encoded integers
     * @return decoded sequence
     */
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


    /**
     * Compute the encoding of an adjacency list
     * @param matrix Adjacency list of a graph
     * @return encoded adjacency list
     */
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

    /**
     * Compute original adjacency list given the encoded adjacency list as input
     * @param matrix Encoded adjacency list of a graph
     * @return decoded adjacency list
     */
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
