package it.bigdatalab.compression;
import com.google.common.io.Closer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.SysexMessage;
import java.awt.*;
import java.io.*;
import java.lang.Math.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class GroupVarInt {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.compression.GroupVarint");


    private int[][] offset;

    private byte[] compressedOffset;
    private byte[][] compressedAdjList;
    private byte[] compressedAdjListFlat;
    private DifferentialCompression GapCompressor;
    public  GroupVarInt(){


    }


    public static float log2(int x)
    {
        return (float) (Math.log(x) / Math.log(2));
    }

    public int get_bytes_number(int number){
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

    public byte[] listEncoding(int [] sequence){
        int [] groupedSequence = new int [4];
        byte[] tmpArray = new byte[0];
        byte[] partialEncoding;

        int i,k,c;
        k = 0;
        c = 0;
        for(i = 0; i<sequence.length; i++){
            //System.out.println(k);
            groupedSequence[k] = sequence[i];
            k++;
            if((k & 3) == 0) {

                partialEncoding = EncodeGroup(groupedSequence);
                tmpArray = byteArrayExtend(partialEncoding, tmpArray);
                k = 0;
                c += 1;
            }

        }
        if(c< sequence.length){
            int[] remainingEncoding = new int[k];
            for (int y = 0; y<k;y++){
                remainingEncoding[y] = groupedSequence[y];
            }
            partialEncoding = EncodeGroup(remainingEncoding);
            tmpArray = byteArrayExtend(partialEncoding,tmpArray);


        }
        return(tmpArray);
    }


    public byte[] sequenceEncoding(int []sequence){
        int k,i,j,l;
        int [] groupedSequence = new int [4];
        byte[] tmpArray = new byte[0];
        byte[] finalEncoding = new byte[0];
        int end;
        byte[] partialEncoding;
        int check = 0;
        end = 0;
        for (i =0; i<sequence.length;i++){

            groupedSequence[end] =  sequence[i];
            end++;
            if(end == 4){
                partialEncoding = EncodeGroup(groupedSequence);

                tmpArray = byteArrayExtend(partialEncoding,tmpArray);

                end = 0;
            }
            check+=1;
        }
        //System.out.println("END "+ end);
        //System.out.println(sequence.length);
        // Se la lunghezza della lista di elementi non è multipla di 4 dobbiamo eseguire un'ultima passata
        if(end > 0 && end<4){
            int[] remainingEncoding = new int[end];
            k = 0;
            while(k< end){
                remainingEncoding[k] = groupedSequence[k];
                //System.out.println("RIMANENTE "+groupedSequence);
                k+=1;
                check+=1;
            }
            //System.out.println("CHECK "+ check);
            //System.exit(1);
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

    //Arrays.copyOfRange(array, 1, array.length);
    public int[] dec(byte encoded []){
        int decodedBytes;
        int k,c;
        int index;
        int s1,s2,s3,s4;
        int prefix_sum;
        int [] prefixes;
        int [] final_decoding = new int[0];
        byte [] partial_decoding = new byte[0];
        int [] decoded = new int[0];

        decodedBytes = 0;
        while(decodedBytes<encoded.length){
            // Reading prefixes
            s1 = (0xC0 & encoded[decodedBytes]) >>> 6;
            s2 = (0x30 & encoded[decodedBytes]) >>> 4;
            s3 = (0xC & encoded[decodedBytes]) >>> 2;
            s4 = 0x3 & encoded[decodedBytes];

            prefix_sum = s1+s2+s3+s4 +4;

            prefixes = new int[]{s1 + 1, s2 + 1, s3 + 1, s4 + 1};


            if(decodedBytes +prefix_sum < encoded.length){

                decodedBytes+=1; // excluding prefix
                decoded = new int[4];
                for (k = 0;k<decoded.length;k++){
                    partial_decoding = new byte[prefixes[k]];
                    for (c = 0;c< partial_decoding.length;c++){
                        partial_decoding[c] = encoded[decodedBytes];
                        decodedBytes+=1;
                    }
                    decoded[k] = convertByteArrayToInt(partial_decoding);
                }

            }else{

                decodedBytes +=1; // excluding prefix
                index = 0;
                if(decodedBytes+prefix_sum -encoded.length == 0){
                    index = 4;
                }else if(decodedBytes+prefix_sum -encoded.length == 1){
                    index =3;
                }else if(decodedBytes+prefix_sum -encoded.length == 2){
                    index = 2;
                }else if(decodedBytes+prefix_sum -encoded.length == 3){
                    index = 1;
                }
                decoded = new int[index];
                for (k = 0;k<index;k++){
                    partial_decoding = new byte[prefixes[k]];
                    for(c = 0; c< partial_decoding.length;c++){
                        partial_decoding[c] = encoded[decodedBytes];
                        decodedBytes+=1;
                    }
                    decoded[k] = convertByteArrayToInt(partial_decoding);
                }
            }

            final_decoding = intArrayExtend(decoded, final_decoding);
        }
    return (final_decoding);

    }


    public static byte[] getSliceOfArray(byte[] arr,
                                        int startIndex, int endIndex)
    {

        // Get the slice of the Array
        byte[] slice = Arrays
                .copyOfRange(

                        // Source
                        arr,

                        // The Start index
                        startIndex,

                        // The end index
                        endIndex);

        // return the slice
        return slice;
    }

    public int [][] getOffset(){
        return (offset);
    }
    public byte[] getCompressedOffset() {
        return compressedOffset;
    }

    public byte[][] getCompressedAdjList(){
        return (compressedAdjList);
    }

  public byte [] encodeAdjListFlat(int [][] matrix,boolean d_compression){
        int node,edge,bytes,k;
        byte [] encodedFlat;
        byte [][] encoded;
        byte [] edgeListEnc;
        int [] edgeListToEnc;
        int [][] off;
        logger.info("Encoding the Adjacency list using VarInt GB");
        encoded = new byte[matrix.length][];
        off = new int[matrix.length][2];
        bytes = 0;
        for(node = 0;node<matrix.length;node++){
            edgeListToEnc = new int[matrix[node].length];
            for(edge = 0;edge< matrix[node].length;edge++){
                edgeListToEnc[edge] = matrix[node][edge];
            }
            edgeListEnc = listEncoding(edgeListToEnc);
            bytes+=edgeListEnc.length;
            encoded[node] = edgeListEnc;
            off[node][0] = matrix[node][0];
            off[node][1] = bytes;
        }
        encodedFlat = new byte[bytes];
        k = 0;
        for(node =0;node<encoded.length;node++){
            for (edge = 0;edge<encoded[node].length;edge++){
                encodedFlat[k] = encoded[node][edge];
                k+=1;
            }
        }

        offset = off;
        compressedAdjList = encoded;
        compressedAdjListFlat = encodedFlat;
        return (encodedFlat);
  }





    public void saveEncoding(String outPath,String instance) {

        int n, m, i, k, q, j;
        m = 0;
        n = offset.length;
        logger.info("Writing the encoded Graph and the offset file " );


        // Writing flattered encoding
        try {
            File f = new File(outPath + instance + ".txt");
            if (f.createNewFile()) {
                logger.info("File {} created ", f.getName());

            } else {
                logger.error("File already exists.");
            }

        } catch (IOException e) {
            logger.error("An error occurred.");
            e.printStackTrace();
        }
        try {
            FileUtils.writeByteArrayToFile(new File(outPath+ instance + ".txt"), compressedAdjListFlat);

            logger.info("Successfully written data to the file ");

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Writing offset file

        try {
            File off = new File(outPath + instance + "_offset.txt");
            if (off.createNewFile()) {
                logger.info("File {} created ", off.getName());

            } else {
                logger.error("File already exists.");
            }
        } catch (IOException e) {
            logger.error("An error occurred.");
            e.printStackTrace();
        }

        try {

            BufferedWriter bw = new BufferedWriter(new FileWriter(outPath + instance + "_offset.txt"));

            for (i = 0; i < n; i++) {
                m = offset[i].length;
                for (j = 0; j < m; j++) {
                    bw.write(offset[i][j] + ((j == offset[i].length - 1) ? "" : "\t"));
                }
                bw.newLine();
            }
            bw.flush();


        } catch (IOException e) {
        }
        logger.info("Encoded Graph and offset files properly written " );

    }

    public byte[] get_compressedAdjListFlat(){
        return(compressedAdjListFlat);
    }

}