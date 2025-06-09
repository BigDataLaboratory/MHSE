package it.bigdatalab.structure;

import java.io.IOException;

public abstract class AbstractGraphProvider {
    protected int[] nodes;
    protected int nNodes;
    protected long nArcs;

    public abstract int[] getNeighbors(int node) throws IOException;
    public abstract int[] getNodes();
    public abstract int numNodes();
    public abstract long numArcs() throws IOException;
}
