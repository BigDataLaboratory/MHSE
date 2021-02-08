package it.bigdatalab.compression;
import java.lang.Math.*;
public class GroupVarInt {
    private int[] indexes;
    private byte[] compressedAdjList;

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
        if(number != 0) {
            return (int) (Math.floor(log2(number)) / 8) +1;
        }
        return(1);
    }

    public int convertByteArrayToInt(byte[] data) {
        if (data == null ) return 0x0;
        if(data.length == 1){
            return ((data[0] & 0xFF) << 0 );
        }else if(data.length == 2){
            return   ((data[0] & 0xFF) << 8 ) |
                    ((data[1] & 0xFF) << 0 );
        }else if(data.length == 3){
            return ((data[0] & 0xFF) << 16) |
                    ((data[1] & 0xFF) << 8 ) |
                    ((data[2] & 0xFF) << 0 );
        }
        return  ((data[0] & 0xFF) << 24) |
                ((data[1] & 0xFF) << 16) |
                ((data[2] & 0xFF) << 8 ) |
                ((data[3] & 0xFF) << 0 );
    }

    private static byte[] intToBytes(final int data, int size) {
        // Trasformo int in byte
        byte [] toBitArray = new byte[] {
                (byte)((data >> 24) ),
                (byte)((data >> 16) ),
                (byte)((data >> 8) ),
                (byte)((data ) ),
        };
        // prendo solo i bytes necessari per codificare l'intero
        byte [] converted = new byte[size];
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

        for (i = 0; i<group.length; i++){
                byte_array_lenght+=get_bytes_number(group[i]);
        }

        //System.out.println("BAL "+byte_array_lenght);
        byte [] partial_encoding = new byte [byte_array_lenght];

        // va controllato
        //int shift = 8;
        // Index inizia da 1 perché l'indice 0 dell'array di bytes è riservato al prefisso
        int index = 1;
        for (i = 0; i<4; i++){
            if(i<group.length){
                numbers_prefix[i] = get_bytes_number(group[i]);
                //System.out.println("NUmero "+ group[i] +" Bytes "+get_bytes_number(group[i]));
                byte [] encoded = new byte[numbers_prefix[i]];

                byte [] converted_resto;
                k = group[i];
                j = numbers_prefix[i]-1;
                while(k>0){


                    resto = k & 0xFF; // k % 256
                    quotient = k>>8; // k // 256

                    converted_resto = intToBytes(resto, get_bytes_number(resto));
                    for (l = 0; l<converted_resto.length; l++){
                        encoded[j] = converted_resto[l];
                        j--;
                    }

                    k = quotient;
                }
                // copio il risultato sull'array finale
                for(l = 0; l<encoded.length;l++){
                    partial_encoding[index] = encoded[l];
                    index+=1;
                }
            }else{
                numbers_prefix[i] = 1;
            }
        }

        byte selector = (byte) (((numbers_prefix[0] - 1) << 6) | ((numbers_prefix[1] - 1) << 4) | ((numbers_prefix[2] - 1) << 2) | (numbers_prefix[3] - 1));
        partial_encoding[0] = selector;

        return(partial_encoding);
    }

    private byte[] byteArrayExtend(byte[] source,byte[] destination){
            byte[] copied = new byte[destination.length+source.length];
            int i,j;

            for(i =0; i<destination.length;i++){
                copied[i] = destination[i];
            }
            j = 0;
            for (i = destination.length; i<destination.length+source.length; i++){
                copied[i] = source[j];
                j++;
            }
            return(copied);
    }

    private int[] intArrayExtend(int[] source,int[] destination){
        int[] copied = new int[destination.length+source.length];
        int i,j;

        for(i =0; i<destination.length;i++){
            copied[i] = destination[i];
        }
        j = 0;
        for (i = destination.length; i<destination.length+source.length; i++){
            copied[i] = source[j];
            j++;
        }
        return(copied);
    }

    public byte[] sequenceEncoding(int []sequence){
        int k,i,j,l;
        int [] groupedSequence = new int [4];
        byte[] tmpArray = new byte[0];
        byte[] finalEncoding = new byte[0];
        int end;
        byte[] partialEncoding;

        end = 0;
        for (i =0; i<sequence.length;i++){

            groupedSequence[end] =  sequence[i];
            end++;
            if(end == 4){
                partialEncoding = EncodeGroup(groupedSequence);

                tmpArray = byteArrayExtend(partialEncoding,tmpArray);

                end = 0;
            }
        }

        // Se la lunghezza della lista di elementi non è multipla di 4 dobbiamo eseguire un'ultima passata
        if(end > 0 && end<4){
            int[] remainingEncoding = new int[end];
            k = 0;
            while(k< end){
                remainingEncoding[k] = groupedSequence[k];
                k+=1;
            }

            partialEncoding = EncodeGroup(remainingEncoding);

            tmpArray = byteArrayExtend(partialEncoding,tmpArray);

        }
        return(tmpArray);
    }
    private int get_len(byte [] array, int start,int end){
        int len = 0;

        for (int i = start; i<end; i++){
            len+=1;
        }
        return(len);
    }

    public int[] decode(byte encoded []){
        int i,j,k,p,r,l,q;
        int s1,s2,s3,s4;
        int [] prefix_lenghts;
        int sum_of_prefix_numbers = 0;
        int group_encoding_lenght = 0;
        int[] tmpArray = new int[0];
        int[] partial_decoding;

        i=0;
        while(i < encoded.length) {



            s1 = (encoded[i] >>> 6);
            s2 =  0x3 & (encoded[i]  >>> 4);
            s3 = 0x3 & (encoded[i]  >>> 2);
            s4 = 0x3 & encoded[i] ;


            prefix_lenghts = new int[]{s1 + 1, s2 + 1, s3 + 1, s4 + 1};
            sum_of_prefix_numbers = s1 + s2 + s3 + s4 + 4;
            l = i +1;

            if (encoded.length - i -1 <= 4  ) {
                partial_decoding = new int[sum_of_prefix_numbers-(i+sum_of_prefix_numbers-encoded.length+1)];
                r = 0;
                p = 0;

                byte[] a = new byte[prefix_lenghts[r]];
                while (p < prefix_lenghts[r]) {
                    a[p] = encoded[l];
                    p += 1;
                    l += 1;
                }
                partial_decoding[r] = convertByteArrayToInt(a);
                r += 1;

            }else{
                partial_decoding = new int [4];
                r = 0;
                for (k = 0; k < 4; k++) {
                    p = 0;
                    byte[] a = new byte[prefix_lenghts[r]] ;
                    while (p != prefix_lenghts[r]) {
                        a[p] = encoded[l];
                        p += 1;
                        l += 1;
                    }

                    partial_decoding[r] = convertByteArrayToInt(a);
                    r += 1;
                }

            }

            tmpArray = intArrayExtend(partial_decoding,tmpArray);



            j = i;
            group_encoding_lenght = 0;
            while (j < i + sum_of_prefix_numbers + 1) {
                group_encoding_lenght += 1;
                j += 1;
            }
            group_encoding_lenght -= 1;

            i += sum_of_prefix_numbers +1;
        }
        return(tmpArray);
        }


    public void encodeAdjList(int [][] AList){

        

        // Da mettere: Gestione della lista di adj
        // for each nodo nella lista di adiacenza
        // va deciso come rappresentare e gestire la lista di adj
    }

    // Il file va come variabile privata del metodo
 /*   public int[] decodeSequence(byte [] File,int offsetStart, int offsetEnd){

    }*/

    private void writeEncoding(){

    }

    private void loadEncoding(){

    }

}