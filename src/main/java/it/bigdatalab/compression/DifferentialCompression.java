package it.bigdatalab.compression;
import java.util.Arrays;

// Interfaccia per una futura lambda expression
//interface Difference {
//    public String subtract(int a, int b);
//}
public class DifferentialCompression {

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
            encoded[j+1] = sequence[j+1] - sequence[j];
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
//    public int[][] encodeADJList(int [][] adj){
//
//    }
}
