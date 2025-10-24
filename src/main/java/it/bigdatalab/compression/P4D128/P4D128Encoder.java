package GraphManagerDemo.compression.P4D128;

import GraphManagerDemo.compression.P4D128.utils.BitOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class P4D128Encoder {
    private static final int DEFAULT_BLOCK_SIZE = 256;
    private int degree;
    private int optimalBitWidth;
    private int maxBitWidth;
    private int optimalNExcept;
    private int optimalCost;
    private ByteArrayOutputStream exceptMetadata;
    private int adjListSize;

    public P4D128Encoder() {

    }

    public void encodeBigList(int[] inputList, ByteArrayOutputStream out) throws IOException {
        this.degree = inputList.length;
        deltaEncode(inputList, 0, degree);

        int numFullBlocks = degree / DEFAULT_BLOCK_SIZE;
        int remainder = degree % DEFAULT_BLOCK_SIZE;
        int numBlocks = (remainder > 0) ? numFullBlocks + 1 : numFullBlocks;

        ByteArrayOutputStream listStream = new ByteArrayOutputStream();
        GraphManagerDemo.compression.P4D256.utils.BitOutputStream listPacker = new GraphManagerDemo.compression.P4D256.utils.BitOutputStream(listStream);

        ByteArrayOutputStream exceptStream = new ByteArrayOutputStream();
        GraphManagerDemo.compression.P4D256.utils.BitOutputStream exceptPacker = new GraphManagerDemo.compression.P4D256.utils.BitOutputStream(exceptStream);

        int[] bigListBitWidths = new int[numBlocks];
        int[] bigListNExcept = new int[numBlocks];
        int[] bigListMaxBitWidth = new int[numBlocks];
        int[][] bigListExceptPos = new int[numBlocks][];

        for (int blockNum = 0; blockNum < numBlocks; blockNum++) {
            int startID = blockNum * DEFAULT_BLOCK_SIZE;
            int endID;

            if (blockNum == numFullBlocks) {
                endID = startID + remainder;
            } else {
                endID = startID + DEFAULT_BLOCK_SIZE;
            }

            selectOptimalBitWidth(inputList, startID, endID);
            bigListBitWidths[blockNum] = optimalBitWidth;
            bigListNExcept[blockNum] = optimalNExcept;
            bigListMaxBitWidth[blockNum] = maxBitWidth;

            if (optimalNExcept > 0) {
                int exceptCounter = 0;
                bigListExceptPos[blockNum] = new int[optimalNExcept];

                for (int i = startID; i < endID; i++) {
                    listPacker.writeBits(inputList[i], optimalBitWidth);
                    if (inputList[i] >>> optimalBitWidth != 0) {
                        bigListExceptPos[blockNum][exceptCounter++] = i - startID;
                        if (maxBitWidth - optimalBitWidth > 1) {
                            exceptPacker.writeBits(inputList[i] >>> optimalBitWidth, maxBitWidth - optimalBitWidth);
                        }
                    }
                }

            } else {
                for (int i = startID; i < endID; i++) {
                    listPacker.writeBits(inputList[i], optimalBitWidth);
                }
            }
        }
        exceptPacker.flush();
        listPacker.flush();

        int sizeBefore = out.size();

        DataOutputStream writer = new DataOutputStream(out);
        writer.writeByte(-128);
        writer.writeInt(degree);

        for (int blockNum = 0; blockNum < numBlocks; blockNum++) {
            out.write(bigListBitWidths[blockNum]);
        }

        out.write(listStream.toByteArray());

        for (int blockNum = 0; blockNum < numBlocks; blockNum++) {
            writer.writeByte(bigListNExcept[blockNum]);

            if (bigListNExcept[blockNum] > 0){
                writer.writeByte(bigListMaxBitWidth[blockNum]);

                for (int pos : bigListExceptPos[blockNum]) {
                    writer.writeByte(pos);
                }
            }
        }

        out.write(exceptStream.toByteArray());

        adjListSize = out.size() - sizeBefore;
    }

    public void encodeSmallList(int[] inputList, ByteArrayOutputStream out) throws IOException {
        int sizeBefore = out.size();
        this.degree = inputList.length;

        deltaEncode(inputList, 0, degree);

        selectOptimalBitWidth(inputList, 0, degree);

        byte[] exceptionIndices = new byte[optimalNExcept];
        int[] exceptionValues = new int[optimalNExcept];
        int[] baseValues = new int[degree];
        int j = 0;

        for (int i = 0; i < degree; i++) {
            int value = inputList[i];

            if ((value >>> optimalBitWidth) != 0) {
                baseValues[i] = value & ((1 << optimalBitWidth) - 1);
                exceptionIndices[j] = (byte) i;
                exceptionValues[j] = (value >>> optimalBitWidth);
                j++;
            } else {
                baseValues[i] = value;
            }
        }

        GraphManagerDemo.compression.P4D256.utils.BitOutputStream packer = new GraphManagerDemo.compression.P4D256.utils.BitOutputStream(out);

        out.write(optimalBitWidth);

        if (degree < 256) {
            out.write(degree);
        } else {
            out.write(0);
        }

        for (int i = 0; i < degree; i++) {
            packer.writeBits(baseValues[i], optimalBitWidth);
        }
        packer.flush();

        out.write(optimalNExcept);

        if (optimalNExcept > 0) {
            out.write(maxBitWidth);

            for (int i = 0; i < optimalNExcept; i++) {
                out.write(exceptionIndices[i]);
            }

            if (maxBitWidth - optimalBitWidth > 1) {
                for (int i = 0; i < optimalNExcept; i++) {
                    packer.writeBits(exceptionValues[i], maxBitWidth - optimalBitWidth);
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

    public void encodeOffsetList(int[] inputList, ByteArrayOutputStream out, int size) throws IOException {
        deltaEncode(inputList, 0, size);

        selectOptimalBitWidth(inputList, 0, size);

        byte[] exceptionIndices = new byte[optimalNExcept];
        int[] exceptionValues = new int[optimalNExcept];
        int[] baseValues = new int[size];
        int j = 0;

        for (int i = 0; i < size; i++) {
            int value = inputList[i];

            if ((value >>> optimalBitWidth) != 0) {
                baseValues[i] = value & ((1 << optimalBitWidth) - 1);
                exceptionIndices[j] = (byte) i;
                exceptionValues[j] = (value >>> optimalBitWidth);
                j++;
            } else {
                baseValues[i] = value;
            }
        }

        BitOutputStream packer = new BitOutputStream(out);

        out.write(optimalBitWidth);

        for (int i = 0; i < size; i++) {
            packer.writeBits(baseValues[i], optimalBitWidth);
        }
        packer.flush();

        out.write(optimalNExcept);

        if (optimalNExcept > 0) {
            out.write(maxBitWidth);

            for (int i = 0; i < optimalNExcept; i++) {
                out.write(exceptionIndices[i]);
            }

            if (maxBitWidth - optimalBitWidth > 1) {
                for (int i = 0; i < optimalNExcept; i++) {
                    packer.writeBits(exceptionValues[i], maxBitWidth - optimalBitWidth);
                }
            }
        }
        packer.flush();
    }

    public void reset() {
        degree = 0;
        optimalBitWidth = 32;
        maxBitWidth = 0;
        optimalNExcept = 0;
        optimalCost = 0;
        if (exceptMetadata != null) {
            exceptMetadata.reset();
        }
        adjListSize = 0;
    }

    public int getAdjListSize() {
        return adjListSize;
    }

    private void selectOptimalBitWidth(int[] inputList, int start, int end) {
        optimalBitWidth = 32;
        optimalNExcept = 0;
        maxBitWidth = 0;
        optimalCost = 0;

        int[] bitHistogram = new int[33];
        int blockLength = end - start;

        for (int i = start; i < end; i++) {
            int bits = bitsNeeded(inputList[i]);
            bitHistogram[bits]++;
            if (bits > maxBitWidth) {
                maxBitWidth = bits;
            }
        }

        optimalBitWidth = maxBitWidth;
        optimalCost = alignToByte(optimalBitWidth * blockLength);
        int exceptCounter = 0;

        for (int b = optimalBitWidth - 1; b >= 0; b--) {
            exceptCounter += bitHistogram[b + 1];

            if (exceptCounter == blockLength) {
                break;
            }

            int currentCost = alignToByte(b * blockLength) + (exceptCounter * 8) + alignToByte((exceptCounter * (maxBitWidth - b)));

            if (maxBitWidth - b == 1) {
                currentCost = currentCost - exceptCounter;
            }

            if (currentCost < optimalCost) {
                optimalCost = currentCost;
                optimalBitWidth = b;
                optimalNExcept = exceptCounter;
            }
        }
    }

    private int alignToByte(int bits) {
        return ((bits + 7) / 8) * 8;
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
