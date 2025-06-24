package it.bigdatalab.compression.P4D;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Alpha version of the P4D encoding technique.
 * Decoder for the P4DEncoder class.
 * This version is not optimized, and it should NOT be used.
 * The newer version can be found in the P4D256 package.
 */

public class P4DDecoder {
    public static int[] decodeAdjList(byte[] compressed, int offset, int length) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(compressed, offset, length);

        int bitWidth = in.read();
        if (bitWidth < 1 || bitWidth > 32) throw new IOException("Invalid bit width: " + bitWidth);

        int numValues = VarIntCompression.readVarInt(in);

        int totalBits = bitWidth * numValues;
        int totalBytes = (totalBits + 7) / 8;

        byte[] packedBits = new byte[totalBytes];
        int read = in.read(packedBits);

        BitInputStream bitIn = new BitInputStream(new ByteArrayInputStream(packedBits));

        int[] decoded = new int[numValues];
        for (int i = 0; i < numValues; i++) {
            decoded[i] = bitIn.readBits(bitWidth);
        }

        int numExceptions = VarIntCompression.readVarInt(in);

        for (int i = 0; i < numExceptions; i++) {
            int index = VarIntCompression.readVarInt(in);
            int value = VarIntCompression.readVarInt(in);
            decoded[index] = value;
        }

        return deltaDecode(decoded);

    }

    private static int[] deltaDecode(int[] inputList) {
        int numValues = inputList.length;
        int[] out = new int[numValues];

        int sum = 0;

        for (int i = 0; i < numValues; i++) {
            sum = sum + inputList[i];
            out[i] = sum;
        }
        return out;
    }

}
