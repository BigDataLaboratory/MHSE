package it.bigdatalab.compression;
import java.lang.Math.*;
public class GroupVarInt {


    public  GroupVarInt(){
     // Da sviluppare
    }


    public static int log2(int x)
    {
        return (int) (Math.log(x) / Math.log(2));
    }

    public void EncodeList(int [] list)
    {


    }
    public int get_bytes_number(int number){
        return (int) (Math.floor(log2(number)+1)/8);
    }
    // Gets an array of at most 4 elements
    public int EncodeGroup(int [] group){
        /*
            Array for the number of bytes of each element of the group
            If the group has lenght strictly less than 4 the coordinates of number_prefix that are not in group are 0
        */
        int [] numbers_prefix = new int[4];

        /*
        int n0;
        int n1;
        int n2;
        int n3;
        */
        int [] partial_encoding = new int [group.length];
        int i;
        int k;
        int resto;
        int quotient;
        int encoded = 0;
        // va controllato
        int shift = 8;

        for (i = 0; i<4; i++){
            if(i<group.length){
                numbers_prefix[i] = get_bytes_number(group[i]);
                k = group[i];
                while(k>0){
                    resto = k & 0x100; // k % 256
                    quotient = k>>8; // k // 256
                    encoded = encoded | (resto<<shift);
                    k = quotient;

                    shift =

                }
            }else{
                numbers_prefix[i] = 1;
            }
        }
        // Da controllare bene il -1
        byte selector = (byte) (((numbers_prefix[0] - 1) << 6) | ((numbers_prefix[1] - 1) << 4) | ((numbers_prefix[2] - 1) << 2) | (numbers_prefix[3] - 1));



        return(1);
    }

}
