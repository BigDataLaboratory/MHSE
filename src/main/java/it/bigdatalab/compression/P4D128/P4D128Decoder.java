package it.bigdatalab.compression.P4D128;

import it.bigdatalab.compression.P4D256.utils.BitInputStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class P4D128Decoder {
    private static final int DEFAULT_BLOCK_SIZE = 128;

    public static int[] decodeBigList(ByteArrayInputStream encodedList) throws IOException {
        DataInputStream dataIn = new DataInputStream(encodedList);

        int check = dataIn.read();
        int degree = dataIn.readInt();

        int numFullBlocks = degree / DEFAULT_BLOCK_SIZE;
        int remainder = degree % DEFAULT_BLOCK_SIZE;
        int numBlocks = (remainder > 0) ? numFullBlocks + 1 : numFullBlocks;

        int[] bitWidths = new int[numBlocks];
        for (int blockNum = 0; blockNum < numBlocks; blockNum++) {
            bitWidths[blockNum] = dataIn.readUnsignedByte();
        }

        BitInputStream unpacker = new BitInputStream(encodedList);
        int[] decoded = new int[degree];
        int currentIndex = 0;

        for (int blockNum = 0; blockNum < numFullBlocks; blockNum++) {
            for (int i = 0; i < DEFAULT_BLOCK_SIZE; i++) {
                decoded[currentIndex++] = unpacker.readBits(bitWidths[blockNum]);
            }
        }

        if (remainder > 0) {
            for (int i = 0; i < remainder; i++) {
                decoded[currentIndex++] = unpacker.readBits(bitWidths[numFullBlocks]);
            }
        }

        unpacker.flush();

        int[][] exceptionPositions = new int[numBlocks][];
        int[] maxBitWidths = new int[numBlocks];
        int[] numExceptions = new int[numBlocks];

        for (int blockNum = 0; blockNum < numBlocks; blockNum++) {
            numExceptions[blockNum] = dataIn.readUnsignedByte();

            if (numExceptions[blockNum] > 0) {
                maxBitWidths[blockNum] = dataIn.readUnsignedByte();

                exceptionPositions[blockNum] = new int[numExceptions[blockNum]];
                for (int i = 0; i < numExceptions[blockNum]; i++) {
                    exceptionPositions[blockNum][i] = dataIn.readUnsignedByte();
                }
            }
        }

        for (int blockNum = 0; blockNum < numBlocks; blockNum++) {
            if (numExceptions[blockNum] > 0) {
                int blockStart = blockNum * DEFAULT_BLOCK_SIZE;
                int highBitsWidth = maxBitWidths[blockNum] - bitWidths[blockNum];

                for (int i = 0; i < numExceptions[blockNum]; i++) {
                    int exceptionPos = exceptionPositions[blockNum][i];
                    int absolutePos = blockStart + exceptionPos;

                    if (highBitsWidth > 1) {
                        int highBits = unpacker.readBits(highBitsWidth);
                        decoded[absolutePos] |= (highBits << bitWidths[blockNum]);
                    } else {
                        decoded[absolutePos] |= (1 << bitWidths[blockNum]);
                    }
                }
            }
        }
        unpacker.flush();

        deltaDecode(decoded);

        return decoded;
    }

    public static int[] decodeSmallList(ByteArrayInputStream in) throws IOException {
        int bitWidth = in.read();
        if (bitWidth < 1 || bitWidth > 32) throw new IOException("Invalid bit width: " + bitWidth);

        int degree = in.read();

        if (degree == 0) {
            degree = 256;
        }

        BitInputStream unpacker = new BitInputStream(in);

        int[] smallList = new int[degree];
        for (int i = 0; i < degree; i++) {
            smallList[i] = unpacker.readBits(bitWidth);
        }
        unpacker.flush();

        int numExcept = in.read();

        if (numExcept > 0) {
            int[] exceptPos = new int[numExcept];
            int maxBitWidth = in.read();

            for (int i = 0; i < numExcept; i++) {
                exceptPos[i] = in.read();
            }

            if (maxBitWidth - bitWidth > 1) {
                for (int i = 0; i < numExcept; i++) {
                    int highBits = unpacker.readBits(maxBitWidth - bitWidth);
                    smallList[exceptPos[i]] |= highBits << bitWidth;
                }
            } else {
                for (int i = 0; i < numExcept; i++) {
                    smallList[exceptPos[i]] |= 1 << bitWidth;
                }
            }
        }

        unpacker.flush();

        deltaDecode(smallList);

        return smallList;
    }

    public static int[] decodeOffsetList(ByteArrayInputStream in, int size) throws IOException {
        int bitWidth = in.read();
        if (bitWidth < 1 || bitWidth > 32) throw new IOException("Invalid bit width: " + bitWidth);

        BitInputStream unpacker = new BitInputStream(in);

        int[] smallList = new int[size];
        for (int i = 0; i < size; i++) {
            smallList[i] = unpacker.readBits(bitWidth);
        }
        unpacker.flush();

        int numExcept = in.read();

        if (numExcept > 0) {
            int[] exceptPos = new int[numExcept];
            int maxBitWidth = in.read();

            for (int i = 0; i < numExcept; i++) {
                exceptPos[i] = in.read();
            }

            if (maxBitWidth - bitWidth > 1) {
                for (int i = 0; i < numExcept; i++) {
                    int highBits = unpacker.readBits(maxBitWidth - bitWidth);
                    smallList[exceptPos[i]] |= highBits << bitWidth;
                }
            } else {
                for (int i = 0; i < numExcept; i++) {
                    smallList[exceptPos[i]] |= 1 << bitWidth;
                }
            }
        }

        unpacker.flush();

        deltaDecode(smallList);

        return smallList;
    }

    private static void deltaDecode(int[] list) {
        for (int i = 1; i < list.length; i++) {
            list[i] = list[i] + list[i-1];
        }
    }

    private static void deltaDecodeRange(int[] list, int start, int end) {
        for (int i = start + 1; i < end; i++) {
            list[i] = list[i] + list[i-1];
        }
    }
}
