package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class SeedNode {

    @SerializedName("seeds")
    private IntArrayList mSeeds;
    @SerializedName("nodes")
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
