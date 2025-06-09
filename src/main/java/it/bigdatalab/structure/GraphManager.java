package it.bigdatalab.structure;

import java.io.IOException;

public class GraphManager {
    private AbstractGraphProvider graphProvider;

    public GraphManager(String format, String inputPath) throws IOException {
        this.graphProvider = GraphProviderFactory.createGraphProvider(inputPath, format);
    }

    // Methods
    public int[] getNeighbors(int node) throws IOException {
        return graphProvider.getNeighbors(node);
    }

    public int[] getNodes() {
        return graphProvider.getNodes();
    }

    public int numNodes() {
        return graphProvider.numNodes();
    }

    public long numArcs() throws IOException {
        return graphProvider.numArcs();
    }

}
