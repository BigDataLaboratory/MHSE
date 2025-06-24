package it.bigdatalab.compression.P4D256.utils;

import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream {
    private final OutputStream out;
    private int currentByte;
    private int numBitsFilled;

    public BitOutputStream(OutputStream out) {
        this.out = out;
        this.currentByte = 0;
        this.numBitsFilled = 0;
    }

    public void writeBits(int value, int numBits) throws IOException {
        for (int i = numBits - 1; i >= 0; i--) {
            int bit = (value >>> i) & 1;
            currentByte = (currentByte << 1) | bit;
            numBitsFilled++;
            if (numBitsFilled == 8) {
                out.write(currentByte);
                numBitsFilled = 0;
                currentByte = 0;
            }
        }
    }

    public void flush() throws IOException {
        if (numBitsFilled > 0) {
            currentByte <<= (8 - numBitsFilled);
            out.write(currentByte);
        }
        currentByte = 0;
        numBitsFilled = 0;
        out.flush();
    }
}
