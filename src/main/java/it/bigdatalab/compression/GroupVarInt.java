package it.bigdatalab.compression;
import java.lang.Math.*;
public class GroupVarInt {


    public  GroupVarInt(){
     // Da sviluppare
    }


    public static float log2(int x)
    {
        return (float) (Math.log(x) / Math.log(2));
    }

    public void EncodeList(int [] list)
    {


    }
    public int get_bytes_number(int number){
        // controlla il +1 finale
        return (int) (Math.floor(log2(number)+1)/8)+1;
    }

    public int convertByteArrayToInt(byte[] data) {
        if (data == null ) return 0x0;
        if(data.length != 4){

            byte []completeArray = new byte[4];
            for(int i =0 ; i< 4-data.length;i++){
                completeArray[i] = 0x0;

            }

            int k = 0;
            for(int i =4-data.length ; i< 4;i++){
                completeArray[i] = data[k];

                k++;
            }
            return (int)(
                    (0xff & completeArray[0]) << 24  |
                            (0xff & completeArray[1]) << 16  |
                            (0xff & completeArray[2]) << 8   |
                            (0xff & completeArray[3]) << 0
            );
        }
        // ----------
        return (int)( // NOTE: type cast not necessary for int
                (0xff & data[0]) << 24  |
                        (0xff & data[1]) << 16  |
                        (0xff & data[2]) << 8   |
                        (0xff & data[3]) << 0
        );
    }
    private static byte[] intToBytes(final int data, int size) {
        // Trasformo l'int in byte
        byte [] toBitArray = new byte[] {
                (byte)((data >> 24) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff),
        };
        // prendo solo i bytes necessari per codificare l'intero in bytes
        byte [] converted = new byte[size];

        for (int i =0 ; i<toBitArray.length;i++){
            System.out.println(toBitArray[i]);
        }

        int i = 0;
        int j = toBitArray.length-1;
        while(i <size){
            converted[i] = toBitArray[j];
            i++;
            j--;
        }

        return(converted);

    }
    // Gets an array of at most 4 elements
    public byte[] EncodeGroup(int [] group){
        /*
            Array for the number of bytes of each element of the group
            If the group has lenght strictly less than 4 the coordinates of number_prefix that are not in group are 0
        */
        int [] numbers_prefix = new int[4];
        // Lunghezza dell'array di bytes che codifica il vettore in input
        // esso conta anche gli 8 bits del prefisso
        int byte_array_lenght = 1;
        //int [] partial_encoding = new int [group.length];
        int i,j,l;
        int k;
        int resto;
        int quotient;

        for (i = 0; i<4; i++){
            if(i<group.length){
                byte_array_lenght+=get_bytes_number(group[i]);
            }else{
                byte_array_lenght+=1;
            }
        }
        byte [] partial_encoding = new byte [byte_array_lenght];
        // va controllato
        //int shift = 8;
        // Index inizia da 1 perché l'indice 0 dell'array di bytes è riservato al prefisso
        int index = 1;
        for (i = 0; i<4; i++){
            if(i<group.length){
                numbers_prefix[i] = get_bytes_number(group[i]);

                byte [] encoded = new byte[numbers_prefix[i]];
                System.out.println("PREF " + numbers_prefix[i]);
                byte [] converted_resto;
                k = group[i];

                while(k>0){
                    j = 0;
                    System.out.println("kappa "+k);
                    resto = k & 0xFF; // k % 256
                    quotient = k>>8; // k // 256
                    // forse questo che sto per fare è inutile con gli interi e può essere bypassato usando gli shifts
                    converted_resto = intToBytes(resto,get_bytes_number(group[i]));
//                    System.out.println("RESTO");
//                    System.out.println(resto);
//                    System.out.println(converted_resto );
//                    System.out.println("lunghzza");
//                    System.out.println(converted_resto.length);
//                    System.out.println(converted_resto[0] & 0xff);
//
//
//
//                    System.out.println("ReSTO");
//                    System.out.println("Result           : " + convertByteArrayToInt(converted_resto));
//

                    System.out.println(converted_resto.length);
                    System.out.println(encoded.length);
                    for (l = 0; l<converted_resto.length; l++){
                        System.out.println("j "+j);
                        System.out.println("l "+l);
                        System.out.println("lol "+converted_resto[l]);
                        encoded[j] = converted_resto[l];

                        j++;
                    }
                    k = quotient;
                }

                // copio il risultato sull'array finale
                for(l = 0; l<encoded.length;l++){
                    partial_encoding[index] = encoded[l];
                    index+=1;
                }
                System.out.println(convertByteArrayToInt(encoded));

            }else{
                numbers_prefix[i] = 1;
            }
        }
        // Da controllare bene il -1
        byte selector = (byte) (((numbers_prefix[0] - 1) << 6) | ((numbers_prefix[1] - 1) << 4) | ((numbers_prefix[2] - 1) << 2) | (numbers_prefix[3] - 1));
        partial_encoding[0] = selector;
        System.out.println("SELECTOR");
        System.out.println(selector & 0xff);
        return(partial_encoding);
    }

}
