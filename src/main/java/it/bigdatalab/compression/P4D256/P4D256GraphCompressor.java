package it.bigdatalab.compression.P4D256;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class P4D256GraphCompressor {
    private int numNodes;
    private long numArcs;
    private P4D256Encoder[] encoderPool;
    private final int DEFAULT_NUMBER_OF_THREADS = 8; // !!! not tested with n of threads != 8

    public P4D256GraphCompressor() {
        encoderPool = new P4D256Encoder[DEFAULT_NUMBER_OF_THREADS];
        for (int i = 0; i < DEFAULT_NUMBER_OF_THREADS; i++) {
            encoderPool[i] = new P4D256Encoder();
        }
    }

    public void convertAdjlistToP4D256(String inputPath, String outputPath) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(inputPath));
        FileOutputStream writer = new FileOutputStream(outputPath + "_adjlists.bin");

        long currentOffset = 0;
        List<Long> batchStartOffsets = new ArrayList<>();
        List<int[]> batchRelativeOffsets = new ArrayList<int[]>();

        List<String> batch = new ArrayList<>();
        String line;

        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_NUMBER_OF_THREADS);

        while ((line = reader.readLine()) != null) {
            batch.add(line);

            if (batch.size() == 256) {
                BatchOutput batchOutput = processBatch(batch, executor);
                writer.write(batchOutput.batchLists);
                batchStartOffsets.add(currentOffset);
                batchRelativeOffsets.add(batchOutput.batchOffsets);

                numArcs += batchOutput.batchArcs;
                numNodes += 256;
                currentOffset += batchOutput.batchLists.length;
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            BatchOutput batchOutput = processBatch(batch, executor);
            writer.write(batchOutput.batchLists);
            batchStartOffsets.add(currentOffset);
            batchRelativeOffsets.add(batchOutput.batchOffsets);
            numArcs += batchOutput.batchArcs;
            numNodes += batch.size();
        }

        executor.close();
        writer.close();
        reader.close();

        writeOffsetsToFile(outputPath, batchStartOffsets, batchRelativeOffsets);
    }

    private BatchOutput processBatch(List<String> batch, ExecutorService executor) throws Exception {
        int numThreads = DEFAULT_NUMBER_OF_THREADS;
        int listsPerThread = batch.size() / numThreads;
        int remainder = batch.size() % numThreads;

        List<Callable<ThreadOutput>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            int startPos = i * listsPerThread;
            int endPos = (i + 1) * listsPerThread;

            if (i == numThreads - 1) {
                endPos += remainder;
            }

            if (startPos >= batch.size()) continue;

            List<String> threadBatch = batch.subList(startPos, endPos);
            tasks.add(new CompressionTask(threadBatch, encoderPool[i]));
        }

        List<Future<ThreadOutput>> futures = executor.invokeAll(tasks);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int[] batchOffsets = new int[listsPerThread*numThreads + remainder];
        long batchArcs = 0;
        int currentOffset = 0;
        int currentPos = 0;

        for (int i = 0; i < futures.size(); i++) {
            ThreadOutput result = futures.get(i).get();
            out.write(result.threadLists);
            for (int j = 0; j < result.threadOffsets.length; j++) {
                batchOffsets[currentPos+j] = result.threadOffsets[j] + currentOffset;
            }
            currentPos += result.threadOffsets.length;
            currentOffset += result.threadLists.length;
            batchArcs += result.threadArcs;
        }

        return new BatchOutput(out.toByteArray(), batchOffsets, batchArcs);
    }

    public void writeOffsetsToFile(String outputPath, List<Long> batchStartOffsets, List<int[]> batchRelativeOffsets) throws IOException {
        String offsetFilePath = outputPath + "_offsets.txt";

        try (DataOutputStream writer = new DataOutputStream(new FileOutputStream(offsetFilePath))) {
            // Write numArcs as long
            writer.writeLong(numArcs);

            // Write numNodes as int
            writer.writeInt(numNodes);

            // Write batch start offsets (numNodes / 256 + 1 entries)
            for (Long batchOffset : batchStartOffsets) {
                writer.writeLong(batchOffset);
            }

            // Write all relative offsets as ints
            for (int[] batchOffsets : batchRelativeOffsets) {
                for (int offset : batchOffsets) {
                    writer.writeInt(offset);
                }
            }
        }
    }

}

class CompressionTask implements Callable<ThreadOutput> {
    private List<String> adjLists;
    private P4D256Encoder encoder;

    public CompressionTask(List<String> adjLists, P4D256Encoder encoder) {
        this.adjLists = adjLists;
        this.encoder = encoder;
    }

    @Override
    public ThreadOutput call() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long threadArcs = 0;
        int currentOffset = 0;
        int[] threadOffsets = new int[adjLists.size()];

        for (int i = 0; i < adjLists.size(); i++) {
            int[] processedAdjList = stringToAdjList(adjLists.get(i));

            int degree = processedAdjList.length;

            if (degree == 0) {
                encoder.encodeEmptyList(out);
            } else if (degree <= 256) {
                encoder.encodeSmallList(processedAdjList, out);
            } else {
                encoder.encodeBigList(processedAdjList, out);
            }

            threadArcs += degree;

            threadOffsets[i] = currentOffset;
            currentOffset += encoder.getAdjListSize();

            encoder.reset();
        }

        return new ThreadOutput(out.toByteArray(), threadOffsets, threadArcs);
    }

    private int[] stringToAdjList(String adjList) {
        String[] temp = adjList.trim().split("\t");
        int n = temp.length;
        int[] out = new int[n-1];
        for (int i = 1; i < n; i++) {
            out[i-1] = Integer.parseInt(temp[i]);
        }
        return out;
    }

}

class ThreadOutput {
    public byte[] threadLists;
    public int[] threadOffsets;
    public long threadArcs;

    public ThreadOutput(byte[] compressedResults, int[] offsets, long totalArcs) {
        this.threadLists = compressedResults;
        this.threadOffsets = offsets;
        this.threadArcs = totalArcs;
    }

}

class BatchOutput {
    public byte[] batchLists;
    public int[] batchOffsets;
    public long batchArcs;

    public BatchOutput(byte[] data, int[] offsets, long totalArcs) {
        this.batchLists = data;
        this.batchOffsets = offsets;
        this.batchArcs = totalArcs;
    }

}