package it.bigdatalab.algorithm;

import it.bigdatalab.model.Measure;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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

}
