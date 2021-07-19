package it.bigdatalab.algorithm;

import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public abstract class BMinHash extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.BMinHash");

    public BMinHash(final GraphManager g, int numSeeds, double threshold) {
        super(g, numSeeds, threshold);
    }

    public BMinHash(final GraphManager g, int numSeeds, double threshold, int[] nodes) {
        super(g, numSeeds, threshold, nodes);
    }

    public int lengthBitsArray(int numberOfNodes) {
        return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    public Int2DoubleLinkedOpenHashMap hopTable(Int2ObjectOpenHashMap<int[]> ct) {
        Int2DoubleLinkedOpenHashMap hopTable = new Int2DoubleLinkedOpenHashMap();
        int lastHop = ct.size() - 1;
        long sumCollisions = 0;

        for (int hop = 0; hop <= lastHop; hop++) {
            int[] collisions = ct.get(hop);
            sumCollisions = Arrays.stream(collisions).sum();
            double couples = ((double) sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hopTable.put(hop, couples);
        }
        return hopTable;
    }

    /***
     * TODO Optimizable?
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    public void normalizeCollisionsTable(Int2ObjectOpenHashMap<int[]> ct) {
        int lowerBoundDiameter = ct.size() - 1;

        //Start with hop 1
        //There is no check for hop 0 because at hop 0 there is always (at least) 1 collision, never 0.
        for (int i = 1; i <= lowerBoundDiameter; i++) {
            int[] previousHopCollisions = ct.get(i - 1);
            int[] hopCollisions = ct.get(i);
            if (Arrays.stream(hopCollisions).anyMatch(coll -> coll == 0)) {
                for (int j = 0; j < hopCollisions.length; j++) {
                    if (hopCollisions[j] == 0) {
                        hopCollisions[j] = previousHopCollisions[j];
                    }
                }
            }
            ct.put(i, hopCollisions);
        }
    }
}
