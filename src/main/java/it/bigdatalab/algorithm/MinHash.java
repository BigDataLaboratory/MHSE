package it.bigdatalab.algorithm;

import it.bigdatalab.model.Measure;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public abstract class MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHash");

    protected int mNumSeeds;
    protected double mThreshold;

    protected IntArrayList mSeeds;
    protected ImmutableGraph mGraph;
    protected int[] mMinHashNodeIDs;

    public MinHash() {
    }

    public MinHash(final ImmutableGraph g, int numSeeds, double threshold, int[] nodes) {
        if (numSeeds != (nodes != null ? nodes.length : 0))
            throw new SeedsException("Specified different number of seeds in properties. \"minhash.numSeeds\" is " + mNumSeeds + " and length of seeds list is " + nodes.length);
        this.mNumSeeds = numSeeds;
        this.mThreshold = threshold;
        this.mGraph = g;
        this.mMinHashNodeIDs = nodes;
    }

    public MinHash(final ImmutableGraph g, int numSeeds, double threshold, IntArrayList seeds) {
        if (numSeeds != (seeds != null ? seeds.size() : 0))
            throw new SeedsException("Specified different number of seeds in properties. \"minhash.numSeeds\" is " + mNumSeeds + " and length of seeds list is " + seeds.size());
        this.mNumSeeds = numSeeds;
        this.mThreshold = threshold;
        this.mGraph = g;
        this.mSeeds = seeds;
        this.mMinHashNodeIDs = new int[mNumSeeds];
    }

    public MinHash(final ImmutableGraph g, int numSeeds, double threshold) {
        this.mNumSeeds = numSeeds;
        this.mThreshold = threshold;
        this.mGraph = g;
        this.mMinHashNodeIDs = new int[mNumSeeds];
    }

    public abstract Measure runAlgorithm() throws IOException;

    public int[] getNodes() {
        return mMinHashNodeIDs;
    }

    public static class SeedsException extends IllegalArgumentException {
        SeedsException(String message) {
            super(message);
        }
    }

    //Classes for computing the farness
    public double[] inverseFarnessArray(long[][] hopMatrix){
        int i,j;
        double [] inversefarness = new double[mGraph.numNodes()];
        Arrays.fill(inversefarness,0);
        for (i = 0; i < mGraph.numNodes(); i++){
            for (j = 0; j < this.mNumSeeds; j++){
                if (hopMatrix[i][j] > 0) {
                    inversefarness[i] += 1.0/hopMatrix[i][j];
                }
            }
        }
        return inversefarness;
    }
    public double[] farnessArray(long[][] hopMatrix ){
        int i,j;
        double [] farness = new double[mGraph.numNodes()];
        Arrays.fill(farness,0);
        for (i = 0; i < mGraph.numNodes(); i++){
            for (j = 0; j < this.mNumSeeds; j++){
                farness[i] += hopMatrix[i][j];
            }
        }
        return farness;

    }



}
