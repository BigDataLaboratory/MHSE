package it.bigdatalab.compression.P4D256;

import it.bigdatalab.compression.P4D256.utils.BitInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class P4D256Decoder {

    public static int[] decodeBigList(ByteArrayInputStream encodedList) throws IOException {
        BitInputStream unpacker = new BitInputStream(encodedList);

        int bytesRead = 0;

        int bigListCheck = (byte) encodedList.read();

        int degree = unpacker.readBits(32);

        int metadata_offset = unpacker.readBits(32);

        encodedList.reset();
        encodedList.skip(metadata_offset);


        int metadata_size = unpacker.readBits(32);

        byte[] byteContainer = new byte[metadata_size];
        encodedList.read(byteContainer);

        int bitmap = unpacker.readBits(32);
        bytesRead += 4;

        int[][] exceptionArrays = new int[33][];

        for (int k = 1; k < 33; k++) {
            if ((bitmap & (1 << (k - 1))) != 0) {
                int size = unpacker.readBits(32);
                exceptionArrays[k] = new int[size];
            }
        }

        for (int k = 2; k < 33; k++) {
            if ((bitmap & (1 << (k - 1))) != 0) {
                for (int i = 0; i < exceptionArrays[k].length; i++) {
                    exceptionArrays[k][i] = unpacker.readBits(k);
                }
            }
        }

        encodedList.reset();
        encodedList.skip(1 + 4 + 4);

        BitInputStream baseUnpacker = new BitInputStream(encodedList);
        int[] decoded = new int[degree];
        int[] exceptionPointers = new int[33];
        int numBlocks = degree / 256;
        int remainder = degree % 256;
        int metadataOffset = 0;

        for (int blockNum = 0; blockNum < numBlocks; blockNum++) {
            int blockStart = blockNum*256;

            int bestBitWidth = byteContainer[metadataOffset++] & 0xFF;
            int bestNExcept = byteContainer[metadataOffset++] & 0xFF;

            int maxBitWidth = bestBitWidth;
            if (bestNExcept > 0) {
                maxBitWidth = byteContainer[metadataOffset++] & 0xFF;
            }

            for (int i = 0; i < 256; i++) {
                decoded[blockStart+i] = baseUnpacker.readBits(bestBitWidth);
            }

            if (bestNExcept > 0) {
                int bitDiff = maxBitWidth - bestBitWidth;

                for (int i = 0; i < bestNExcept; i++) {
                    int exceptionIndex = byteContainer[metadataOffset++] & 0xFF;

                    if (bitDiff == 1) {
                        decoded[blockStart + exceptionIndex] |= (1 << bestBitWidth);
                    } else {
                        int exceptionValue = exceptionArrays[bitDiff][exceptionPointers[bitDiff]++];
                        decoded[blockStart + exceptionIndex] |= (exceptionValue << bestBitWidth);
                    }
                }
            }
        }

        if (remainder > 0) {
            int blockStart = numBlocks * 256;

            int bestBitWidth = byteContainer[metadataOffset++] & 0xFF;
            int bestNExcept = byteContainer[metadataOffset++] & 0xFF;

            int maxBitWidth = bestBitWidth;
            if (bestNExcept > 0) {
                maxBitWidth = byteContainer[metadataOffset++] & 0xFF;
            }

            for (int i = 0; i < remainder; i++) {
                decoded[blockStart + i] = baseUnpacker.readBits(bestBitWidth);
            }

            if (bestNExcept > 0) {
                int bitDiff = maxBitWidth - bestBitWidth;

                for (int i = 0; i < bestNExcept; i++) {
                    int exceptionIndex = byteContainer[metadataOffset++] & 0xFF;

                    if (bitDiff == 1) {
                        decoded[blockStart + exceptionIndex] |= (1 << bestBitWidth);
                    } else {
                        int exceptionValue = exceptionArrays[bitDiff][exceptionPointers[bitDiff]++];
                        decoded[blockStart + exceptionIndex] |= (exceptionValue << bestBitWidth);
                    }
                }
            }
        }
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
