package it.bigdatalab.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class SeedNode {

    private IntArrayList mSeeds;
    private int[] mNodes;

    public SeedNode(IntArrayList seeds, int[] nodes) {
        this.mSeeds = seeds;
        this.mNodes = nodes;
    }

    public IntArrayList getSeeds() {
        return mSeeds;
    }

    public void setSeeds(IntArrayList seeds) {
        this.mSeeds = seeds;
    }

    public int[] getNodes() {
        return mNodes;
    }

    public void setNodes(int[] nodes) {
        this.mNodes = nodes;
    }
}
