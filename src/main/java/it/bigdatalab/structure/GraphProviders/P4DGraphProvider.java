package it.bigdatalab.structure.GraphProviders;

import it.bigdatalab.compression.P4D.P4DDecoder;
import it.bigdatalab.structure.AbstractGraphProvider;

import java.io.*;
import java.util.Arrays;

/**
 * Alpha version of the P4D encoding technique.
 * Graph Provider for the P4DGraphCompressor class.
 * This version is not optimized, and it should NOT be used.
 * The newer version can be found in the P4D256 package.
 */

public class P4DGraphProvider extends AbstractGraphProvider {
    private byte[] compressedAdjLists;
    private int[][] offsetTable;

    public P4DGraphProvider(String inputPath) throws IOException {
        loadCompressedGraph(inputPath);
        nNodes = numNodes();
    }

    @Override
    public int[] getNeighbors(int nodeID) throws IOException {
        int nodeIndex = 0;
        for (int i = 0; i < nNodes; i++) {
            if (offsetTable[i][0] == nodeID) {
                nodeIndex = i;
                break;
            }
        }

        int byteOffset = offsetTable[nodeIndex][1];

        int finalByte;
        if (nodeID == offsetTable[nNodes-1][0]) {
            finalByte = compressedAdjLists.length;
        } else {
            finalByte = offsetTable[nodeIndex+1][1];
        }

        if (finalByte - byteOffset == 1) {
            return new int[0];
        } else {
            byte[] compressedAdjList = Arrays.copyOfRange(compressedAdjLists, byteOffset, finalByte);

            int[] adjList = P4DDecoder.decodeAdjList(compressedAdjList, 0 , compressedAdjList.length);

            return adjList;
        }
    }

    @Override
    public int[] getNodes() {
        return nodes;
    }

    @Override
    public int numNodes() {
        return nNodes;
    }

    @Override
    public long numArcs() {
        return nArcs;
    }

    public void loadCompressedGraph(String inPath) throws IOException {
        loadOffsetTable(inPath);
        loadCompressedAdjLists(inPath);
    }

    private void loadOffsetTable(String inPath) throws IOException {
        String filePath = inPath + "_p4d_offsets.txt";
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String offsetLine = reader.readLine();
        nNodes = 0;
        while (offsetLine != null) {
            nNodes++;
            offsetLine = reader.readLine();
        }
        reader.close();
        nodes = new int[nNodes];

        offsetTable = new int[nNodes][2];
        reader = new BufferedReader(new FileReader(filePath));
        offsetLine = reader.readLine();
        for (int i = 0; i < nNodes; i++) {
            String[] line = offsetLine.split("\t");
            offsetLine = reader.readLine();

            offsetTable[i][0] = Integer.parseInt(line[0]);
            offsetTable[i][1] = Integer.parseInt(line[1]);

            nodes[i] = offsetTable[i][0];
        }

        reader.close();
    }

    private void loadCompressedAdjLists(String inPath) throws IOException {
        String filePath = inPath + "_p4d.bin";
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

    public void writeGraphToFile(String outPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outPath))) {
            for (int i = 0; i < nNodes; i++) {
                int[] adjList = getNeighbors(i);
                writer.write(Integer.toString(i));

                for (int neighbor : adjList) {
                    writer.write("\t");
                    writer.write(Integer.toString(neighbor));
                }
                writer.newLine();
            }
        }
    }


}
