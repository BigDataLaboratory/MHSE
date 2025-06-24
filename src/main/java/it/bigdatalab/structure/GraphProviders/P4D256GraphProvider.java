package it.bigdatalab.structure.GraphProviders;


import it.bigdatalab.compression.P4D256.P4D256Decoder;
import it.bigdatalab.structure.AbstractGraphProvider;

import java.io.*;

public class P4D256GraphProvider extends AbstractGraphProvider {
    private byte[] compressedAdjLists;
    private int[] offsets;

    public P4D256GraphProvider() {

    }

    @Override
    public int[] getNeighbors(int node) throws IOException {
        int byteOffset = offsets[node];
        int finalByte;

        if (node == nNodes - 1) {
            finalByte = compressedAdjLists.length;
        } else {
            finalByte = offsets[node + 1];
        }

        int length = finalByte - byteOffset;

        if (length == 1) {
            return new int[0];
        }

        ByteArrayInputStream encodedAdjList = new ByteArrayInputStream(compressedAdjLists, byteOffset, length);

        int firstByte = (byte) encodedAdjList.read();
        encodedAdjList.reset();

        if (firstByte > 0 && firstByte <= 32) {
            return P4D256Decoder.decodeSmallList(encodedAdjList);
        } else if (firstByte == -128) {
            return P4D256Decoder.decodeBigList(encodedAdjList);
        } else {
            System.out.println("ERROR ON LIST " + node);
            return new int[0];
        }
    }

    @Override
    public int[] getNodes() {
        return new int[0];
    }

    @Override
    public int numNodes() {
        return 0;
    }

    @Override
    public long numArcs() throws IOException {
        return 0;
    }

    public void loadCompressedGraph(String inputPath) throws IOException {
        loadOffsets(inputPath);
        loadCompressedAdjLists(inputPath);
    }

    public void loadCompressedAdjLists(String inputPath) throws IOException {
        String filePath = inputPath + "_adjlists.bin";
        File file = new File(filePath);
        int fileSize = (int) file.length();

        try (FileInputStream stream = new FileInputStream(filePath)) {
            compressedAdjLists = new byte[fileSize];
            int bytesRead = stream.read(compressedAdjLists);

            // Optional: check if we read everything
            if (bytesRead != fileSize) {
                throw new IOException("Didn't read full file");
            }
        }
    }

    public void loadOffsets(String inputPath) throws IOException {
        String offsetFilePath = inputPath + "_offsets.txt";
        try (DataInputStream reader = new DataInputStream(new FileInputStream(offsetFilePath))) {
            // Read metadata
            nArcs = reader.readLong();
            nNodes = reader.readInt();

            // Calculate and read batch start offsets
            int nBatches = nNodes / 256 + 1;
            long[] batchStartOffsets = new long[nBatches];
            for (int i = 0; i < nBatches; i++) {
                batchStartOffsets[i] = reader.readLong();
            }

            // Read all relative offsets
            offsets = new int[nNodes];
            int nodeIndex = 0;

            for (int batchNum = 0; batchNum < nBatches && nodeIndex < nNodes; batchNum++) {
                long batchStartOffset = batchStartOffsets[batchNum];
                int nodesInThisBatch = Math.min(256, nNodes - nodeIndex);

                for (int i = 0; i < nodesInThisBatch; i++) {
                    int relativeOffset = reader.readInt();
                    offsets[nodeIndex] = (int)(batchStartOffset + relativeOffset);
                    nodeIndex++;
                }
            }
        }
    }

    public void writeGraphToFile(String outPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outPath))) {
            for (int i = 0; i < nNodes; i++) {
                int[] adjList = getNeighbors(i);

                writer.write(Integer.toString(i));

                if (adjList.length == 0) {
                    writer.write("\t");
                }

                for (int neighbor : adjList) {
                    writer.write("\t");
                    writer.write(Integer.toString(neighbor));
                }
                writer.newLine();
            }
        }
    }

}
