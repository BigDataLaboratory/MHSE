package it.bigdatalab.algorithm;

import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BMinHashOpt extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.BMinHashOpt");

    public BMinHashOpt(final ImmutableGraph g, int numSeeds, double threshold) {
        super(g, numSeeds, threshold);
    }

    public int lengthBitsArray(int numberOfNodes) {
        return (int) Math.ceil(numberOfNodes / (double) Integer.SIZE);
    }

    public abstract void normalizeCollisionsTable(int[][] collisionsMatrix, int lowerBound, int[] last);

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
            couples = (double) (sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hoptable[hop] = couples;
            logger.info("hop " + hop + " total collisions " + sumCollisions + " couples: " + couples);
        }
        return hoptable;
    }
}
