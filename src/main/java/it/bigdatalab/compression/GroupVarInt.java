package it.bigdatalab.compression;
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
    private DifferentialCompression GapCompressor;
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
            System.out.println("PREFISSI SENZA SHIFT");
            System.out.println(encoded[decodedBytes]);
            System.out.println("PREFISSI");
            System.out.println(s1+" "+s2+" "+s3+" "+s4);
            System.out.println("Lung enc");
            System.out.println(encoded.length);
            //System.exit(-1);

            if(decodedBytes +prefix_sum < encoded.length){
                System.out.println("MIAO ");

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
                System.out.println("BAU ");

                // if decodedBytes +prefix_sum >= encoded.length
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
                System.out.println("INDEX = "+index);
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
            decodedBytes+=1; // jump to the next prefix
            final_decoding = intArrayExtend(decoded, final_decoding);
        }
        System.out.println("FINAL DEC LN " +final_decoding.length);
    return (final_decoding);

    }

    public int[] decode(byte encoded []){
        int i,j,k,p,r,l,q;
        int s1,s2,s3,s4;
        int [] prefix_lenghts;
        int sum_of_prefix_numbers = 0;
        int group_encoding_lenght = 0;
        int[] tmpArray = new int[0];
        int [] odd_tmpArray = new int [0];
        int[] partial_decoding;
        int decoded_sofar;
        int remaining = 0;
        int remaining_prefix;
        byte [] cop;
        i=0;

        while(i < encoded.length) {


            s1 = (0xC0 & encoded[i]) >>> 6;
            s2 = (0x30 & encoded[i]) >>> 4;
            s3 = (0xC & encoded[i]) >>> 2;
            s4 = 0x3 & encoded[i];
            System.out.println("ENCODED["+i+"] = "+encoded[i]);

            prefix_lenghts = new int[]{s1 + 1, s2 + 1, s3 + 1, s4 + 1};
            sum_of_prefix_numbers = s1 + s2 + s3 + s4 + 4;
            for(int g = 0; g<prefix_lenghts.length;g++){
                System.out.println("PREFISSO["+g+"] = "+prefix_lenghts[g]);
            }
            System.out.println("LUNGHEZZA ENCODING "+encoded.length);
            System.out.println("Index i = "+i);
            cop = Arrays.copyOfRange(encoded, i+1,i+1+sum_of_prefix_numbers);

            System.out.println("LENGH OF COPIED "+cop.length+ " SOMMA PREFISSI "+sum_of_prefix_numbers);
            l = cop.length;

            if(sum_of_prefix_numbers - l  == 0){
                remaining = 4;

            }else if(sum_of_prefix_numbers - l == 1){
                remaining = 3;
            }else if(sum_of_prefix_numbers - l == 2){
                remaining = 2;
            }else if(sum_of_prefix_numbers - l == 3){
                remaining = 1;
            }

            partial_decoding = new int [remaining];
            remaining_prefix = 0;

            for ( r = 0; r<remaining;r++){
                p = 0;
                byte[] a = new byte[prefix_lenghts[r]];
                remaining_prefix += prefix_lenghts[r];
                while (p != prefix_lenghts[r]) {

                    a[p] = encoded[l];
                    p += 1;
                    l += 1;
                }
                partial_decoding[r] = convertByteArrayToInt(a);


            }
            tmpArray = intArrayExtend(partial_decoding, tmpArray);
            i += sum_of_prefix_numbers + 1;

        }


        return(tmpArray);
    }

    public int[] decode_dep(byte encoded []){
        int i,j,k,p,r,l,q;
        int s1,s2,s3,s4;
        int [] prefix_lenghts;
        int sum_of_prefix_numbers = 0;
        int group_encoding_lenght = 0;
        int[] tmpArray = new int[0];
        int [] odd_tmpArray = new int [0];
        int[] partial_decoding;
        int decoded_sofar;
        int remaining = 0;
        int remaining_prefix;

        i=0;
        decoded_sofar = 0;

        while(i < encoded.length) {


            s1 = 0x3 & (encoded[i] >>> 6);
            s2 = 0x3 & (encoded[i] >>> 4);
            s3 = 0x3 & (encoded[i] >>> 2);
            s4 = 0x3 & encoded[i];


            prefix_lenghts = new int[]{s1 + 1, s2 + 1, s3 + 1, s4 + 1};
            sum_of_prefix_numbers = s1 + s2 + s3 + s4 + 4;
            l = i + 1;

            int obs = decoded_sofar + sum_of_prefix_numbers;
            //System.out.println("OBS "+obs);
            System.out.println("LUNG ENC "+ encoded.length);
            //k = 0;
            /*
            for (int u = 0;u<prefix_lenghts.length;u++){
                System.out.println("PREF " +prefix_lenghts[u]);
            }
            System.out.println("SUM OF PREF "+sum_of_prefix_numbers);
            */

            //System.out.println("Lunghezza lista "+encoded.length+ " index i "+i+" somma dei prefissi "+sum_of_prefix_numbers);
            int dif = l+  sum_of_prefix_numbers -encoded.length-2;
            System.out.println("DIFF "+dif);
            System.out.println();
            if (l + sum_of_prefix_numbers >= encoded.length) {
                byte [] cop = Arrays.copyOfRange(encoded,l,encoded.length);
                dif = sum_of_prefix_numbers-(cop.length +2);
                System.out.println("-------------");
                System.out.println(sum_of_prefix_numbers);
                System.out.println(cop.length);
                System.out.println("-------------");

                System.out.println("DIEFFWE "+dif);
                if( sum_of_prefix_numbers -(cop.length +2 )== 0){
                    remaining = 4;
                }else if( sum_of_prefix_numbers -(cop.length +2)== 1){
                    remaining = 3;
                }else if(  sum_of_prefix_numbers -(cop.length +2 )== 2){
                    remaining = 2;
                }else if ( sum_of_prefix_numbers -(cop.length +2 ) == 3){
                    remaining = 1;
                }

                int diff = l+  sum_of_prefix_numbers - encoded.length;
                //System.out.println("DIFF "+diff+ "  ENC LENG "+encoded.length+ " SUM OF PREFIXS "+sum_of_prefix_numbers +" Remaining "+remaining);
                decoded_sofar = l;

                //System.out.println("RIMANGONO "+remaining);

                //odd_tmpArray = new int[1];
                System.out.println("REMAINING "+remaining);
                partial_decoding = new int[remaining];
                remaining_prefix = 0;
//                for(int e = 0; e< prefix_lenghts.length;e++){
//                    System.out.println("PREF "+prefix_lenghts[e]);
//                }
                //int missing_bytes = i + sum_of_prefix_numbers - encoded.length;
                //System.out.println("MISSING BYTES "+missing_bytes);
                //for (int h = 0; h<prefix_lenghts.length; h++){
                 //   System.out.println("PREF "+ prefix_lenghts[h]);
                //}
                //partial_decoding = new int[missing_bytes];
//                System.out.println( "ENC "+encoded.length);
//                System.out.println("DEC SOFAR "+decoded_sofar);
                //r = 0;
                //for(int h = 0;h<prefix_lenghts.length;h++){
                 //   System.out.println("PREF LENG "+prefix_lenghts[h]);
                //}
                //System.out.println("REMAINING "+remaining);
                for ( r = 0; r<remaining; r++ ){
                //while(missing_bytes < encoded.length){
                    //System.out.println("MISSING BYTES "+missing_bytes);

                    p = 0;
                    System.out.println("PL "+prefix_lenghts[r]);
                    byte[] a = new byte[prefix_lenghts[r]];
                    remaining_prefix += prefix_lenghts[r];

                    while (p != prefix_lenghts[r]) {

                        // System.out.println("L = "+l);
                        a[p] = encoded[l];
                        p += 1;
                        l += 1;
                    }
                    //odd_tmpArray[0] = convertByteArrayToInt(a);
                    partial_decoding[r] = convertByteArrayToInt(a);
                    //for (int h = 0;h<partial_decoding.length;h++){
                     //   System.out.println("PART DEC "+partial_decoding[h]);
                    //}
                    //missing_bytes+=prefix_lenghts[r];
                    //r += 1;

                    //partial_decoding = intArrayExtend(partial_decoding, odd_tmpArray);

                }
                //decoded_sofar += remaining_prefix + 1;

                //System.out.println("VAIAUD");


            } else {
                //System.out.println("uuuu");

                partial_decoding = new int[4];
                r = 0;
                for (k = 0; k < 4; k++) {
                    p = 0;

                    byte[] a = new byte[prefix_lenghts[r]];
                    while (p != prefix_lenghts[r]) {
                        a[p] = encoded[l];
                        p += 1;
                        l += 1;
                    }

                    partial_decoding[r] = convertByteArrayToInt(a);
                    r += 1;
                }
                decoded_sofar += sum_of_prefix_numbers + 1;

            }

            tmpArray = intArrayExtend(partial_decoding, tmpArray);
            i += sum_of_prefix_numbers + 1;

        }

/*
            // ERRORE: DEVI GESTIRE I CASI IN CUI È 3 2 1 0
            // SE GESTISCI SOLO <= 4 OTTIENI DEGLI ERRORI
            //if (encoded.length - i -1 <= 4  ) {7
            byte [] slice = getSliceOfArray(encoded, i+1, encoded.length + 1);
            //System.out.println("SLICE SIZE "+ slice.length + " Prefix lenght "+sum_of_prefix_numbers);
//            for(int u = 0; u<prefix_lenghts.length;u++){
//                System.out.println("Prefix "+u+" : "+prefix_lenghts[u]);
//            }
            if(slice.length  <= sum_of_prefix_numbers){
                int diff = slice.length - sum_of_prefix_numbers;
                int lung = 0;
                if(diff == 0 ){
                    lung = 4;
                }else if(diff == 1){
                    lung = 3;
                }else if(diff == 2){
                    lung = 2;
                }else{
                    lung = 1;
                }
                //partial_decoding = new int[sum_of_prefix_numbers-(i+sum_of_prefix_numbers-encoded.length+1)];
                partial_decoding = new int[lung+1];
                r = 0;
                p = 0;
                int prova = sum_of_prefix_numbers - slice.length;
                //System.out.println("DIFFERENZA "+prova);
                //System.out.println(lung);
                for (k = 0; k<lung; k++) {


                    byte[] a = new byte[prefix_lenghts[r]];
                    while (p < prefix_lenghts[r]) {
                        a[p] = encoded[l];
                        //System.out.println(encoded[l]);
                        p += 1;
                        l += 1;

                    }
                    //System.out.println(" k "+k);
                    //System.out.println("CIAO BELLO");
                    partial_decoding[r] = convertByteArrayToInt(a);
                    //System.out.println(partial_decoding[r]);
                    r += 1;
                }

            }else{
//                for(int w = 0; w<prefix_lenghts.length;w++){
//                    System.out.println(prefix_lenghts[w]);
//                }

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

            i += sum_of_prefix_numbers +1;*/
        //}


        return(tmpArray);
        }



        


    // Il file va come variabile privata del metodo
 /*   public int[] decodeSequence(byte [] File,int offsetStart, int offsetEnd){

    }*/

    private void writeEncoding(){

    }

    private void loadEncoding(){

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

  

    public byte[][] encodeAdjList(int [][] matrix,boolean d_compression) {
        int nodes,edges;
        int i,j;
        int off;
        byte [][] encoded;
        int [] diffEncoded;
        int [] row;
        int [] offset_bytes,offset_nodes,gap_offset_bytes,gap_offset_nodes,offset_and_bytes;
        GapCompressor = new DifferentialCompression();
        logger.info("Starting encoding the Adjacency List " );

        nodes = matrix.length;
        encoded = new byte[nodes][];
        offset = new int [nodes][2];
        offset_nodes = new int [nodes];
        offset_bytes = new int [nodes];
        off = 0;
        for (i=0;i<nodes;i++){
            edges = matrix[i].length;
            row = new int[edges];
            for (j=0;j<edges;j++) {
                row[j] = matrix[i][j];
            }
            if(d_compression) {

                //diffEncoded = GapCompressor.encodeSortedSequence(row);
                diffEncoded = GapCompressor.encodeSequence(row);
                for(int o = 0;o<row.length;o++){
                    System.out.println("VECCHIO = "+row[o]+ " COMPR DIFF = "+diffEncoded[o]);
                }
                encoded[i] = listEncoding(diffEncoded);
            }else{
                encoded[i] = listEncoding(row);
            }
            //
            off += encoded[i].length;
            //offset[i][0] = matrix[i][0];
            //offset[i][1] =off;
            offset_nodes[i] = matrix[i][0];
            offset_bytes[i] = off;

        }
        if(d_compression == false){

            for (i = 0;i<nodes;i++){
                offset[i][0] = offset_nodes[i];
                offset[i][1] = offset_bytes[i];
                System.out.println("Offset " + offset[i][0] + "  "+offset[i][1]);

            }

        }else{

            gap_offset_nodes = GapCompressor.encodeSequence(offset_nodes);
            gap_offset_bytes  = GapCompressor.encodeSequence(offset_bytes);
            offset_and_bytes = new int[gap_offset_bytes.length+gap_offset_nodes.length];
            for (i = 0;i<gap_offset_bytes.length;i++){

                offset_and_bytes[i+gap_offset_bytes.length] = gap_offset_nodes[i];

                offset_and_bytes[i] = gap_offset_bytes[i];

            }
            compressedOffset = sequenceEncoding(offset_and_bytes);

            for (i = 0;i<nodes;i++){
                offset[i][0] = gap_offset_nodes[i];
                offset[i][1] = gap_offset_bytes[i];
            }

        }

        
        compressedAdjList = encoded;
        logger.info("Encoding completed " );

        return encoded;
    }

    public int [][] decodeAdjList(byte [][] encoded_matrix){
        int nodes,edges;
        int i,j;
        int [][] decoded;
        byte [] row;
        logger.info("Starting decoding the Adjacency List " );

        nodes = encoded_matrix.length;
        decoded = new int[nodes][];

        for (i =0 ;i<nodes;i++){
            edges = encoded_matrix[i].length;
            row = new byte[edges];
            for (j=0;j<edges;j++){
                row[j] = encoded_matrix[i][j];
            }
            decoded[i] = decode(row);
        }
        logger.info("Decoding completed " );

        return(decoded);
    }

    public static void saveEncoding(String outPath,String instance, byte [][] encoded,int [][] COffset) {

        byte[] flattered_encoding;
        int n, m, i, k, q, j;
        n = encoded.length;
        m = 0;
        logger.info("Writing the encoded Graph and the offset file " );

        for (i = 0; i < n; i++) {
            m += encoded[i].length;
        }

        flattered_encoding = new byte[m];

        // Flattering Matrix
        k = 0;
        for (i = 0; i < n; i++) {
            q = encoded[i].length;
            for (j = 0; j < q; j++) {
                flattered_encoding[k] = encoded[i][j];
                k += 1;
            }
        }

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
            Files.write(Paths.get(outPath+ instance + ".txt"), flattered_encoding);
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
        // Compressed offset
        //n = offset.length;
        try {
            //Files.write(Paths.get(outPath+ instance + "_offset.txt"), COffset);

            BufferedWriter bw = new BufferedWriter(new FileWriter(outPath + instance + "_offset.txt"));

            for (i = 0; i < n; i++) {
                m = COffset[i].length;
                for (j = 0; j < m; j++) {
                    bw.write(COffset[i][j] + ((j == COffset[i].length - 1) ? "" : "\t"));
                }
                bw.newLine();
            }
            bw.flush();


        } catch (IOException e) {
        }
        logger.info("Encoded Graph and offset files properly written " );

    }


    }