package it.bigdatalab.compression.P4D256;

import it.bigdatalab.compression.P4D256.utils.BitOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class P4D256Encoder {
    private static final int DEFAULT_OVERHEAD = 16;
    private static final int DEFAULT_BLOCK_SIZE = 256;
    private int degree;
    private int bestBitWidth;
    private int maxBitWidth;
    private int bestNExcept;
    private int bestCost;
    private ByteArrayOutputStream byteContainer;
    private int[][] dataToBePacked = new int[33][0];
    private int[] dataPointers = new int[33];
    private int adjListSize;
    private BitOutputStream packer;
    
    public P4D256Encoder() {

    }

    public void encodeBigList(int[] inputList, ByteArrayOutputStream out) throws IOException {
        this.degree = inputList.length;
        deltaEncode(inputList, 0, degree);

        for (int k = 1; k < dataToBePacked.length; k++) {
            dataToBePacked[k] = new int[degree / 32 * 4];
        }

        int numFullBlocks = degree / DEFAULT_BLOCK_SIZE;
        int remainder = degree % DEFAULT_BLOCK_SIZE;

        ByteArrayOutputStream listStream = new ByteArrayOutputStream();
        packer = new BitOutputStream(listStream);
        byteContainer = new ByteArrayOutputStream();

        for (int blockNum = 0; blockNum < numFullBlocks; blockNum++) {
            int startID = blockNum * DEFAULT_BLOCK_SIZE;
            encodeBlock(inputList, startID, DEFAULT_BLOCK_SIZE);
        }

        if (remainder > 0) {
            int startID = numFullBlocks * DEFAULT_BLOCK_SIZE;
            encodeBlock(inputList, startID, remainder);
        }

        packer.flush();

        int sizeBefore = out.size();

        out.write(-128);

        BitOutputStream packer2 = new BitOutputStream(out);

        packer2.writeBits(degree, 32);

        int metadata_offset = 1 + 4 + 4 + listStream.size();

        packer2.writeBits(metadata_offset, 32);
        packer2.flush();

        out.write(listStream.toByteArray());

        int metadata_size = byteContainer.size();
        packer2.writeBits(metadata_size, 32);
        packer2.flush();

        out.write(byteContainer.toByteArray());

        int bitmap = 0;
        for (int k = 1; k < dataToBePacked.length; k++) {
            if (dataPointers[k] > 0) {
                bitmap |= (1 << (k - 1));
            }
        }

        packer2.writeBits(bitmap, 32);
        packer2.flush();

        if (dataPointers[1] > 0) {
            packer2.writeBits(dataPointers[1], 32);
        }

        for (int k = 2; k < dataToBePacked.length; k++) {
            if (dataPointers[k] > 0) {
                packer2.writeBits(dataPointers[k], 32);
            }
        }

        for (int i = 2; i < dataToBePacked.length; i++) {
            if (dataPointers[i] > 0) {
                for (int j = 0; j < dataPointers[i]; j++) {
                    packer2.writeBits(dataToBePacked[i][j], i);
                }
            }
        }
        packer2.flush();

        adjListSize = out.size() - sizeBefore;
    }

    public void encodeSmallList(int[] inputList, ByteArrayOutputStream out) throws IOException {
        int sizeBefore = out.size();
        this.degree = inputList.length;

        deltaEncode(inputList, 0, degree);

        selectBestBitWidth(inputList, 0, degree);

        byte[] exceptionIndices = new byte[bestNExcept];
        int[] exceptionValues = new int[bestNExcept];
        int[] baseValues = new int[degree];
        int j = 0;

        for (int i = 0; i < degree; i++) {
            int value = inputList[i];

            if ((value >>> bestBitWidth) != 0) {
                baseValues[i] = value & ((1 << bestBitWidth ) - 1);
                exceptionIndices[j] = (byte) i;
                exceptionValues[j] = (value >>> bestBitWidth);
                j++;
            } else {
                baseValues[i] = value;
            }
        }

        BitOutputStream packer = new BitOutputStream(out);

        out.write(bestBitWidth);

        if (degree < 256) {
            out.write(degree);
        } else {
            out.write(0);
        }

        for (int i = 0; i < degree; i++) {
            packer.writeBits(baseValues[i], bestBitWidth);
        }
        packer.flush();

        out.write(bestNExcept);

        if (bestNExcept > 0) {
            out.write(maxBitWidth);

            for (int i = 0; i < bestNExcept; i++) {
                out.write(exceptionIndices[i]);
            }

            if (maxBitWidth - bestBitWidth > 1) {
                for (int i = 0; i < bestNExcept; i++) {
                    packer.writeBits(exceptionValues[i], maxBitWidth - bestBitWidth);
                }
            }
        }
        packer.flush();

        int sizeAfter = out.size();
        adjListSize = sizeAfter - sizeBefore;
    }

    public void encodeEmptyList(ByteArrayOutputStream out) {
        out.write(0);
        degree = 0;
        adjListSize = 1;
    }

    public void reset() {
        degree = 0;
        bestBitWidth = 32;
        maxBitWidth = 0;
        bestNExcept = 0;
        bestCost = 0;
        if (byteContainer != null) {
            byteContainer.reset();
        }
        Arrays.fill(dataPointers, 0);
        adjListSize = 0;
        packer = null;
    }

    public int getAdjListSize() {
        return adjListSize;
    }

    private void encodeBlock(int[] inputList, int start, int length) throws IOException {

        selectBestBitWidth(inputList, start, start + length);
        byteContainer.write((byte) bestBitWidth);
        byteContainer.write((byte) bestNExcept);

        if (bestNExcept > 0) {
            byteContainer.write((byte) maxBitWidth);

            int index = maxBitWidth - bestBitWidth;

            // Ensure exception array has enough space
            if (dataPointers[index] + bestNExcept >= dataToBePacked[index].length) {
                int newsize = 2 * (dataPointers[index] + bestNExcept);
                newsize = greatestMultiple(newsize + 31, 32); // align to 32
                dataToBePacked[index] = Arrays.copyOf(dataToBePacked[index], newsize);
            }

            for (int j = start; j < start + length; j++) {
                if (inputList[j] >>> bestBitWidth != 0) {
                    byteContainer.write((byte) j - start);
                    dataToBePacked[index][dataPointers[index]++] = inputList[j] >>> bestBitWidth;
                }
            }
        }

        for (int j = start; j < start + length; j++) {
            packer.writeBits(inputList[j], bestBitWidth);
        }
    }

    private static int greatestMultiple(int value, int factor) {
        return value - value % factor;
    }

    private void selectBestBitWidth(int[] inputList, int start, int end) {
        bestBitWidth = 32;
        bestNExcept = 0;
        maxBitWidth = 0;
        bestCost = 0;

        int[] bitHistogram = new int[33];
        int blockLength = end - start;

        for (int i = start; i < end; i++) {
            int bits = bitsNeeded(inputList[i]);
            bitHistogram[bits]++;
        }

        while (bitHistogram[bestBitWidth] == 0) {
            bestBitWidth = bestBitWidth - 1;
        }

        maxBitWidth = bestBitWidth;
        bestCost = bestBitWidth * blockLength;
        int countExcept = 0;

        for (int b = bestBitWidth - 1; b >= 0; b--) {
            countExcept += bitHistogram[b + 1];

            if (countExcept == blockLength) {
                break;
            }

            int currentCost = (((b * blockLength) + 7) / 8) * 8 + (countExcept * 8) + (((countExcept * (maxBitWidth - b) + 7) / 8) * 8) + 8;

            if (maxBitWidth - b == 1) {
                currentCost = currentCost - countExcept;
            }

            if (currentCost < bestCost) {
                bestCost = currentCost;
                bestBitWidth = b;
                bestNExcept = countExcept;
            }
        }

    }

    private int bitsNeeded(int value) {
        if (value == 0) {
            return 1;
        } else {
            return 32 - Integer.numberOfLeadingZeros(value);
        }
    }

    private void deltaEncode(int[] list, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            list[i] = list[i] - list[i - 1];
        }
    }

}
