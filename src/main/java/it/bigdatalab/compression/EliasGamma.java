package it.bigdatalab.compression;

import it.unimi.dsi.bits.BitVector;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// MUST BE COMPLETED!!!
public class EliasGamma {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.compression.EliasFano");


    private int[][] offset;
    private byte[] compressedOffset;
    private byte[][] compressedAdjList;
    private byte[] compressedAdjListFlat;

    public EliasGamma(){

    }


    static long roundUp(long val, final long den) {

        val = val == 0 ? den : val;
        return (val % den == 0) ? val : val + (den - (val % den));

    }

    /**
     * Returns the number of lower bits required to encode each element in an
     * array of size {@code length} with maximum value {@code u}.
     *
     * @param u
     *            the max value in the array
     * @param length
     *            the length of the array
     * @return the number of lower bits
     */
    public static int getL(final int u, final int length) {

        long x = roundUp(u, length) / length;
        return Integer.SIZE - Integer.numberOfLeadingZeros((int) (x - 1));
    }

    /**
     * Compresses {@code length} elements of {@code in}, from {@code inOffset},
     * into the {@code out} array, from {@code outOffset}
     *
     * @param in
     *            the array to compress (MONOTONICALLY INCREASING)
     * @param inOffset
     *            starting offset
     * @param length
     *            number of elements to compress
     * @return the compress values
     */
    public static byte[] compress(final int[] in, final int inOffset, final int length) {

        final int u = in[inOffset + length - 1];
        final int l = getL(u, length);

        final byte[] out = new byte[getCompressedSize(u, length)];
        long lowBitsOffset = 0;
        long highBitsOffset = roundUp(l * length, Byte.SIZE);

        int prev = 0;
        for (int i = 0; i < length; i++) {

            int low = Bits.VAL_TO_WRITE[l] & in[i + inOffset];
            Bits.writeBinary(out, lowBitsOffset, low, l);
            lowBitsOffset += l;
            int high = in[i + inOffset] >>> l;
            Bits.writeUnary(out, highBitsOffset, high - prev);
            highBitsOffset += (high - prev) + 1;
            prev = high;
        }

        return out;

    }

    /**
     * Decompress {@code length} elements from {@code in}, starting at
     * {@code inOffset}, into {@code out}, starting from {@outOffset}.
     * Each element is encoded using {@code l} lower bits.
     *
     * @param in
     *            the compressed array
     * @param inOffset
     *            starting offset
     * @param length
     *            number of elements to decompress
     * @param l
     *            number of lower bits for each element
     * @param out
     *            the uncompressed array
     * @param outOffset
     *            starting offset
     * @return the number of read bytes
     */
    public static int decompress(final byte[] in, final int inOffset, final int length, final int l,
                                 final int[] out, final int outOffset) {

        long lowBitsOffset = inOffset * Byte.SIZE;
        long highBitsOffset = roundUp(lowBitsOffset + (l * length), Byte.SIZE);

        int delta = 0;
        for (int i = 0; i < length; i++) {

            final int low = Bits.readBinary(in, lowBitsOffset, l);
            final int high = Bits.readUnary(in, highBitsOffset);
            delta += high;
            out[outOffset + i] = (delta << l) | low;
            lowBitsOffset += l;
            highBitsOffset += high + 1;
        }

        return (int) (roundUp(highBitsOffset, Byte.SIZE) / Byte.SIZE);
    }



    /**
     * Decompresses the idx-th element from the compressed array {@code in},
     * starting from {@code inOffset}. The uncompressed array has size
     * {@code length} and its elements are encoded using {@code l} lower bits
     * each.
     *
     * @param in
     *            the compressed array
     * @param inOffset
     *            starting offset
     * @param length
     *            the size of the uncompressed array
     * @param l
     *            number of lower bits
     * @param idx
     *            the index of the element to decompress
     * @return the value of the idx-th element
     */
    public static int get(final byte[] in, final int inOffset, final int length, final int l, final int idx) {

        final long lowBitsOffset = inOffset * Byte.SIZE;
        final long highBitsOffset = roundUp(lowBitsOffset + (l * length), Byte.SIZE);

        final int low = Bits.readBinary(in, lowBitsOffset + (l * idx), l);

        final int startOffset = (int) (highBitsOffset / Byte.SIZE);
        int offset = startOffset;
        int prev1Bits = 0;
        int _1Bits = 0;
        while (_1Bits < idx + 1) {

            prev1Bits = _1Bits;
            _1Bits += Integer.bitCount(in[offset++] & 0xFF);
        }
        offset--; // rollback
        int delta = ((offset - startOffset) * Byte.SIZE) - prev1Bits; // delta
        int readFrom = offset * Byte.SIZE;
        for (int i = 0; i < (idx + 1) - prev1Bits; i++) {

            int high = Bits.readUnary(in, readFrom);
            delta += high;
            readFrom += high + 1;

        }

        return (delta << l) | low;
    }

    /**
     * Returns the index of the first element equal or greater than {@code val}
     * from the compressed array {@code in}, starting from {@code inOffset}. The
     * uncompressed array has size {@code length} and its elements are encoded
     * using {@code l} lower bits each.
     *
     * @param in
     *            the compressed array
     * @param inOffset
     *            starting offset
     * @param length
     *            size of the uncompressed array
     * @param l
     *            number of lower bits
     * @param val
     *            value to select
     * @return the index of the first element equal or greater than {@code val}
     */
    public static int select(final byte[] in, final int inOffset, final int length, final int l, final int val) {

        final long lowBitsOffset = inOffset * Byte.SIZE;
        final long highBitsOffset = roundUp(lowBitsOffset + (l * length), Byte.SIZE);

        final int h = val >>> l;

        final int startOffset = (int) (highBitsOffset / Byte.SIZE);
        int offset = startOffset;
        int prev1Bits = 0;
        int _0Bits = 0;
        int _1Bits = 0;
        while (_0Bits < h && _1Bits < length) {

            prev1Bits = _1Bits;
            int bitCount = Integer.bitCount(in[offset++] & 0xFF);
            _1Bits += bitCount;
            _0Bits += Byte.SIZE - bitCount;
        }

        offset = Math.max(offset - 1, startOffset); //conditional rollback

        int low = Bits.readBinary(in, lowBitsOffset + (l * prev1Bits), l);
        int delta = ((offset - startOffset) * Byte.SIZE) - prev1Bits; // delta
        int readFrom = offset * Byte.SIZE;
        int high = Bits.readUnary(in, readFrom);
        delta += high;
        readFrom += high + 1;

        if (((delta << l) | low) >= val) {

            return prev1Bits;

        } else {

            for (int i = prev1Bits + 1; i < length; i++) {

                low = Bits.readBinary(in, lowBitsOffset + (l * i), l);
                high = Bits.readUnary(in, readFrom);
                delta += high;
                readFrom += high + 1;

                if (((delta << l) | low) >= val) return i;

            }
        }

        return -1 ;


    }


    /**
     * Returns the number of time {@code val} occurs in the compressed array
     * {@code in}, starting from {@code inOffset}. The uncompressed array has
     * size {@code length} and its elements are encoded using {@code l} lower
     * bits each.
     *
     * @param in
     *            the compressed array
     * @param inOffset
     *            starting offset
     * @param length
     *            size of the uncompressed array
     * @param l
     *            number of lower bits
     * @param val
     *            value to rank
     * @return number of occurences of {@code val}
     */
    public static int rank(final byte[] in, final int inOffset, final int length, final int l, final int val) {

        /* TODO:
         * this implementation is particularly inefficient and it should be rewritten
         */

        final int idx = select(in, inOffset, length, l, val);

        if (idx == -1) {

            return 0;

        } else {

            int cnt = 1;

            for (int i = idx + 1; i < length; i++) {

                if (get(in, inOffset, length, l, i) == val) {

                    cnt++;

                } else {

                    break;
                }

            }

            return cnt;
        }
    }

    /**
     * Returns the number of bytes required to compress an array of size
     * {@code length} and maximum value {@code u}.
     *
     * @param u
     *            the maximum value in the array to compress
     * @param length
     *            the size of the array to compress
     * @return the number of required bytes
     */
    public static int getCompressedSize(final int u, final int length) {

        final int l = getL(u, length);
        final long numLowBits = roundUp(l * length, Byte.SIZE);
        final long numHighBits = roundUp(2 * length, Byte.SIZE);
        return (int) ((numLowBits + numHighBits) / Byte.SIZE);
    }

    /**
     * Computes the length the log base 2 of an integer
     * @param x integer
     * @return log base 2
     */
    public static float log2(int x)
    {
        return (float) (Math.log(x) / Math.log(2));
    }


    /**
     * Computes the length (in number of bytes) of the binary representation of an integer
     * @param number integer
     * @return length of the binary representation
     */
    public int get_bits_number(int number){
        int count = 0;
        while(number >0){
            if ((number&1)>0) {
                count ++;
            }
            number = number >> 1;
        }
        return (count);

    }


    public byte [] encodeAdjListFlat(int [][] matrix,boolean d_compression){
        int node,edge,bytes,k;
        byte [] encodedFlat;
        byte [][] encoded;
        byte [] edgeListEnc;
        int [] edgeListToEnc;
        int [][] off;
        logger.info("Encoding the Adjacency list using Elias Fano");
        encoded = new byte[matrix.length][];
        // node, offset, length list, lowerbits
        off = new int[matrix.length][4];
        bytes = 0;
        for(node = 0;node<matrix.length;node++){
            edgeListToEnc = new int[matrix[node].length];
            for(edge = 0;edge< matrix[node].length;edge++){
                edgeListToEnc[edge] = matrix[node][edge];
            }
            edgeListEnc = compress(edgeListToEnc,0,edgeListToEnc.length);
            bytes+=edgeListEnc.length;
            encoded[node] = edgeListEnc;
            off[node][0] = matrix[node][0];
            off[node][1] = bytes;
            off[node][2] = edgeListToEnc.length;
            off[node][3] = getL(edgeListToEnc[edgeListToEnc.length-1],edgeListToEnc.length);
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


    /**
     * Save on the disk the encoded adjacency list and its ofsset
     * @param outPath String of the output path
     * @param instance String of the name of the file
     */
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
            FileUtils.writeByteArrayToFile(new File(outPath+ instance + "elias_.txt"), compressedAdjListFlat);

            logger.info("Successfully written data to the file ");

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Writing offset file

        try {
            File off = new File(outPath + instance + "_offset_elias.txt");
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

            BufferedWriter bw = new BufferedWriter(new FileWriter(outPath + instance + "_offset_elias.txt"));

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

    /**
     * Return the offset 2D-Array
     * @return offset
     */
    public int [][] getOffset(){
        return (offset);
    }

    /**
     * Compute the decoded int array
     * @param encoded byte array
     * @return Int array of the decoded sequence
     */
    public int[] dec(byte encoded [],int len,int lowerBit){
       int [] final_decoding = new int[len];

        decompress(encoded,0,len,lowerBit,final_decoding,0);
        return (final_decoding);

    }

    public byte[] getCompressedAdjListFlat() {
        return compressedAdjListFlat;
    }

    /**
     * Compute the byte representation of a integer
     * @param data integer
     * @param size length of the byte representation of the integer
     * @return byte array representation of the integer
     */
    private static byte[] intToBytes(final int data, int size) {
        byte [] toBitArray = new byte[] {
                (byte)((data >> 24) ),
                (byte)((data >> 16) ),
                (byte)((data >> 8) ),
                (byte)((data ) ),
        };
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

    /**
     * Extend a byte array
     * @param source Byte array
     * @param destination Byte array
     * @return extended byte array
     */
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

    /**
     * The encoding size for the value if encoded using Elias Gamma encoding.
     *
     * @param value The value to encode.
     * @return The size of the encoding.
     */
    public static int Size(int value) {
        return (int) log2(value);
    }




    public byte[] encodeSequence( int [] sequence){
        byte [] encoding,tmp,converted;
        int numberOfBits,numberOfBytes;
        int i,j;
        encoding = new byte[0];
        for (i = 0; i< sequence.length;i++) {
            System.out.println("SIZE "+i+ " SZE "+Size(sequence[i]));
            numberOfBytes = (int) Math.floor((2 * get_bits_number(sequence[i])) / 8) + 1;
            System.out.println("BYTE "+numberOfBytes);
            int shifted = (0<<Size(sequence[i])) | sequence[i];
            System.out.println("SHIFTED "+shifted);
            if (sequence[i] == 0) {
                tmp = new byte[1];
                tmp[0] = 0;
            } else {
                numberOfBits = get_bits_number(sequence[i]);
                numberOfBytes = (int) Math.floor((2 * get_bits_number(sequence[i])) / 8) + 1;
                tmp = new byte[numberOfBytes];

                for (j = 0; j < numberOfBits; j++) {
                    tmp[j] = 0;
                }
                System.out.println(" " + tmp.length);

                converted = intToBytes(sequence[i], (int) Math.floor(get_bits_number(sequence[i]) / 8) + 1);
                System.out.println(converted.length);
                for (j = 0; j < converted.length; j++) {
                    tmp[j + numberOfBits] = converted[j];
                }

                encoding = byteArrayExtend(tmp, encoding);

            }
        }
        return encoding;
    }



}
