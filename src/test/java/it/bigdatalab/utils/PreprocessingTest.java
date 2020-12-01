package it.bigdatalab.utils;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreprocessingTest {

    private ImmutableGraph g;

    @BeforeEach
    void setUp() throws IOException {
        String path = new File("src/test/data/12-chain.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        g = ImmutableGraph.load(path);
        int prevN = g.numNodes();

        // generate a new graph with a single isolated node
        // remove edges with node id 5 within
        Transform.ArcFilter f = (x, y) -> (x != 4 || y != 5) && (x != 5 || y != 6) && (x != 5 || y != 4) && (x != 6 || y != 5);
        g = Transform.filterArcs(g, f);
        assertEquals(prevN, g.numNodes());
    }

    @AfterEach
    void tearDown() {
        g = null;
    }

    @Test
    void removeIsolatedNodes() {
        int numNodesExpected = 11;
        g = Preprocessing.removeIsolatedNodes(g);
        assertEquals(numNodesExpected, g.numNodes(), "Doesn't remove every isolated node in the graph");
    }
}