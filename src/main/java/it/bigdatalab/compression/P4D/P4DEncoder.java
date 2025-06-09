package it.bigdatalab.compression.P4D;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/*  Structure of the output list:
    [1 byte bitWitdh (max value: 31) (?)] +
    [1 VarInt compressed numValues (number of neighbors)] +
    [bitWidth x numValues bit-packed values] +
    [1 VarInt compressed numExceptions (number of exceptions, max 10% of numValues] +
    [numExceptions * 2 VarInt compressed values (pos + value)]
*/

public class P4DEncoder {
    private int degree;
    private int bitWidth;
    private int numExceptions;
    private int adjListSize;
    private static final double DEFAULT_EXCEPTION_RATE = 0.10;

    public P4DEncoder() {

    }

    public byte[] encodeAdjList(int[] inputAdjList) throws IOException {
        degree = inputAdjList.length;

        int[] deltas = deltaEncode(inputAdjList);

        bitWidth = selectBitWidth(deltas);

        int[] exceptionIndices = new int[numExceptions];
        int[] exceptionValues = new int[numExceptions];

        ByteArrayOutputStream adjStream = new ByteArrayOutputStream();
        BitOutputStream packer = new BitOutputStream(adjStream);

        int exceptionCounter = 0;
        for (int i = 0; i < degree; i++) {
            int v = deltas[i];
            if (bitsNeeded(v) <= bitWidth) {
                packer.writeBits(v, bitWidth);
            } else {
                packer.writeBits(0, bitWidth);
                exceptionIndices[exceptionCounter] = i;
                exceptionValues[exceptionCounter] = v;
                exceptionCounter++;
            }
        }
        packer.flush();

        ByteArrayOutputStream excStream = new ByteArrayOutputStream();

        for (int i = 0; i < numExceptions; i++) {
            VarIntCompression.writeVarInt(excStream, exceptionIndices[i]);
            VarIntCompression.writeVarInt(excStream, exceptionValues[i]);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bitWidth);
        VarIntCompression.writeVarInt(out, degree);
        out.write(adjStream.toByteArray());
        VarIntCompression.writeVarInt(out, numExceptions);
        out.write(excStream.toByteArray());
        adjListSize = out.size();

        return out.toByteArray();
    }

    public byte[] encodeEmptyAdjList() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1);
        out.write(0);
        degree = 0;
        adjListSize = 1;
        return out.toByteArray();
    }

    public int getDegree() {
        return degree;
    }

    public int getBitWidth() {
        return bitWidth;
    }

    public int getNumExceptions() {
        return numExceptions;
    }

    public int getAdjListSize() {
        return adjListSize;
    }

    public void reset() {
        degree = 0;
        bitWidth = 0;
        numExceptions = 0;
        adjListSize = 0;
    }

    private int[] deltaEncode(int[] inputList) {
        int[] out = new int[degree];
        out[0] = inputList[0];
        int prev = inputList[0];

        for (int i = 1; i < degree; i++) {
            int current = inputList[i];
            out[i] = current - prev;
            prev = current;
        }

        return out;
    }

    private int selectBitWidth(int[] deltas) {
        int[] bitHistogram = new int[33];
        for (int delta : deltas) {
            int bits = bitsNeeded(delta);
            bitHistogram[bits]++;
        }

        int cumulative = 0;
        numExceptions = degree - cumulative;

        for (int b = 1; b <= 32; b++) {
            cumulative += bitHistogram[b];
            numExceptions = degree - cumulative;

            if (numExceptions <= degree * DEFAULT_EXCEPTION_RATE)
                return b;
        }

        return 32;
    }

    private int bitsNeeded(int value) {
        if (value == 0) {
            return 1;
        } else {
            return 32 - Integer.numberOfLeadingZeros(value);
        }
    }

}
