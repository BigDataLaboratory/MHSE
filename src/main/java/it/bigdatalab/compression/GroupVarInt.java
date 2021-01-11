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
        return (int)(
                (0xff & data[0]) << 24  |
                        (0xff & data[1]) << 16  |
                        (0xff & data[2]) << 8   |
                        (0xff & data[3]) << 0
        );
    }

    private static byte[] intToBytes(final int data, int size) {
        // Trasformo int in byte
        byte [] toBitArray = new byte[] {
                (byte)((data >> 24) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff),
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
                //System.out.println("NUMERO "+ group[i]);
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
                byte [] encoded = new byte[numbers_prefix[i]];

                byte [] converted_resto;
                k = group[i];
                while(k>0){
                    j = 0;

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

                    for (l = 0; l<converted_resto.length; l++){

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
        //System.out.println("Lung S "+source.length+ " Lung D "+destination.length);
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
    int lastIteration = 0;
    int [] groupedSequence = new int [4];
    byte[] tmpArray = new byte[0];
    byte[] finalEncoding = new byte[0];
    int start,end;
    byte[] partialEncoding;

    if((sequence.length & 3) == 0) {
        lastIteration = 1;
    }

    start = 0;
    end = 0;

    for (i =0; i<sequence.length;i++){
        System.out.println("i+end "+ i + end);
        System.out.println("i "+i);
        System.out.println("end "+end);
        System.out.println("start "+start);
        System.out.println("Lunghezza input "+sequence.length);
        groupedSequence[end] =  sequence[i];
        end++;
        if(end == 4){

            System.out.println("lung compresso "+ groupedSequence.length);
//            for (int p =0 ; p< groupedSequence.length; p++){
//                System.out.println("nela lista "+groupedSequence[p]);
//            }
            partialEncoding = EncodeGroup(groupedSequence);

            tmpArray = byteArrayExtend(partialEncoding,tmpArray);

            start = i;
            end = 0;
        }
    }
    // Se la lunghezza della lista di elementi non è multipla di 4 dobbiamo eseguire un'ultima passata
    if(end<4){
        int[] remainingEncoding = new int[end];
        for(k = 0; k<end;k++){
            remainingEncoding[k] = groupedSequence[k];
        }
        partialEncoding = EncodeGroup(remainingEncoding);
        tmpArray = byteArrayExtend(partialEncoding,tmpArray);
    }
    return(tmpArray);
}
    public void encodeAdjList(int [][] AList){


        // Da mettere: Gestione della lista di adj
        // for each nodo nella lista di adiacenza
        // va deciso come rappresentare e gestire la lista di adj
    }

    private void writeEncoding(){

    }

}