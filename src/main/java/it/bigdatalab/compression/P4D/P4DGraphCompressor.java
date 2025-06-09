package it.bigdatalab.compression.P4D;

import java.io.*;
import java.util.Arrays;

public class P4DGraphCompressor {
    private int numNodes;
    private long numArcs;
    private int[][] offsetTable;
    private byte[] compressedAdjLists;

    public P4DGraphCompressor(String inputPath) throws IOException {
        compressGraph(inputPath);
    }

    public int getNumNodes() {
        return numNodes;
    }

    public long getNumArcs() {
        return numArcs;
    }

    public void writeCompressedGraphToFile(String outPath) throws IOException {
        writeOffsetTableToFile(outPath);
        writeCompressedAdjListsToFile(outPath);
    }

    private void compressGraph(String inputPath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputPath));

        String nodeLine = reader.readLine();
        while (nodeLine != null) {
            numNodes++;
            nodeLine = reader.readLine();
        }
        reader.close();

        offsetTable = new int[numNodes][2];

        reader = new BufferedReader(new FileReader(inputPath));
        ByteArrayOutputStream compressedAdjStream = new ByteArrayOutputStream();
        nodeLine = reader.readLine();
        P4DEncoder encoder = new P4DEncoder();

        int byteOffset = 0;
        numArcs = 0;

        for (int i = 0; i < numNodes; i++) {
            int[] tempList = stringToTempList(nodeLine);
            offsetTable[i][0] = tempList[0];
            int[] adjList = Arrays.copyOfRange(tempList, 1, tempList.length);

            if (adjList.length == 0) {
                byte[] emptyAdjList = encoder.encodeEmptyAdjList();
                compressedAdjStream.write(emptyAdjList);
            } else {
                byte[] singleAdjList = encoder.encodeAdjList(adjList);
                compressedAdjStream.write(singleAdjList);
            }

            offsetTable[i][1] = byteOffset;
            numArcs += encoder.getDegree();
            byteOffset += encoder.getAdjListSize();

            encoder.reset();
            nodeLine = reader.readLine();
        }
        reader.close();

        compressedAdjLists = compressedAdjStream.toByteArray();
        numArcs = numArcs/2;
    }

    private void writeOffsetTableToFile(String outPath) throws IOException {
        String filePath = outPath + "_p4d_offsets.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (int i = 0; i < numNodes; i++) {
                String nodeID = String.valueOf(offsetTable[i][0]);
                String offset = String.valueOf(offsetTable[i][1]);

                writer.write(nodeID + "\t" + offset);
                writer.newLine();
            }
        }
    }

    private void writeCompressedAdjListsToFile(String outPath) throws IOException {
        String filePath = outPath + "_p4d.bin";

        try (FileOutputStream stream = new FileOutputStream(filePath)) {
            stream.write(compressedAdjLists);
        }
    }

    private int[] stringToTempList(String adjList) {
        String[] temp = adjList.trim().split("\t");
        int n = temp.length;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = Integer.parseInt(temp[i]);
        }
        return out;
    }

}
