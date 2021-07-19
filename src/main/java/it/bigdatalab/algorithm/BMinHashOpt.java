package it.bigdatalab.algorithm;

import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BMinHashOpt extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.BMinHashOpt");

    public BMinHashOpt(final GraphManager g, int numSeeds, double threshold, int[] nodes) {
        super(g, numSeeds, threshold, nodes);
    }

    public BMinHashOpt(final GraphManager g, int numSeeds, double threshold) {
        super(g, numSeeds, threshold);
    }

    public int lengthBitsArray(int numberOfNodes) {
        return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
    }

    /***
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    public void normalizeCollisionsTable(int[][] collisionsMatrix, int lowerBound) {

        for (int i = 0; i < collisionsMatrix.length; i++) { // check last hop of each seed
            // if last hop is not the lower bound
            // replace the 0 values from last hop + 1 until lower bound
            // with the value of the previous hop for the same seed
            if (collisionsMatrix[i].length - 1 < lowerBound) {
                int oldLen = collisionsMatrix[i].length;
                int[] copy = new int[lowerBound + 1];
                System.arraycopy(collisionsMatrix[i], 0, copy, 0, collisionsMatrix[i].length);
                collisionsMatrix[i] = copy;
                for (int j = oldLen; j <= lowerBound; j++) {
                    collisionsMatrix[i][j] = collisionsMatrix[i][j - 1];
                }
            }
        }
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */
    public double[] hopTable(int[][] collisionsMatrix, int lowerBound) {
        long sumCollisions;
        double couples;
        double[] hoptable = new double[lowerBound + 1];
        // lower bound is the max size of inner array
        for (int hop = 0; hop < lowerBound + 1; hop++) {
            sumCollisions = 0;
            for (int seed = 0; seed < collisionsMatrix.length; seed++) {
                sumCollisions += collisionsMatrix[seed][hop];
            }
            couples = ((double) sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hoptable[hop] = couples;
        }
        return hoptable;
    }
}
