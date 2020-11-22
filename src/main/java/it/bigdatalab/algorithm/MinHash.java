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

    public MinHash(final ImmutableGraph g, int numSeeds, double threshold) {
        this.mNumSeeds = numSeeds;
        this.mThreshold = threshold;
        this.mGraph = g;
        mMinHashNodeIDs = new int[mNumSeeds];
    }


    public IntArrayList getSeeds() {
        return mSeeds;
    }

    public void setSeeds(IntArrayList seeds) throws SeedsException {
        if (mNumSeeds != seeds.size()) {
            String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + mNumSeeds + " and length of seeds list is " + seeds.size();
            throw new SeedsException(message);
        }

        this.mSeeds = seeds;
    }

    public int[] getNodes() {
        return mMinHashNodeIDs;
    }

    public void setNodes(int[] nodes) throws SeedsException {
        if (mNumSeeds != nodes.length) {
            String message = "Specified different number of seeds in properties. \"minhash.numSeeds\" is " + mNumSeeds + " and length of nodes list is " + mMinHashNodeIDs.length;
            throw new SeedsException(message);
        }

        this.mMinHashNodeIDs = nodes;
    }

    public void setNumSeeds(int numSeeds) {
        this.mNumSeeds = numSeeds;
    }

    public abstract Measure runAlgorithm() throws IOException;

    public static class SeedsException extends Throwable {
         SeedsException(String message) {
            super(message);
        }
    }

}
