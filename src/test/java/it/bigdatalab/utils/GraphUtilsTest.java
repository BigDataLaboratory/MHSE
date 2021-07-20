package it.bigdatalab.utils;

import com.google.common.graph.Graph;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GraphUtilsTest {

    @Test
    void testLoadGraph_InDirection() throws IOException {
        String inputFilePath = new File("src/test/data/g_directed/32-path.graph").getAbsolutePath();
        inputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        boolean transpose = false;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        boolean webGraph = true;
        boolean compressedGraph = false;
        String direction = "in";

        int[] expected = new int[]{3};
        GraphManager g =new GraphManager(webGraph,compressedGraph,inputFilePath, transpose, inMemory, isolatedVertices, direction);
        assertArrayEquals(expected, g.successorArray(2));
    }

    @Test
    void testLoadGraph_OutDirection() throws IOException {
        String inputFilePath = new File("src/test/data/g_directed/32-path.graph").getAbsolutePath();
        inputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        boolean transpose = false;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        boolean webGraph = true;
        boolean compressedGraph = false;
        String direction = "out";

        int[] expected = new int[]{2};
        GraphManager g =new GraphManager(webGraph,compressedGraph,inputFilePath, transpose, inMemory, isolatedVertices, direction);
        assertArrayEquals(expected, g.successorArray(3));
    }

    @Test
    void testLoadGraph_TransposedGraphInDirection() throws IOException {
        String inputFilePath = new File("src/test/data/g_directed/32t-path.graph").getAbsolutePath();
        inputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        boolean transpose = true;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        boolean webGraph = true;
        boolean compressedGraph = false;
        String direction = "in";

        int[] expected = new int[]{6};
        GraphManager g =new GraphManager(webGraph,compressedGraph,inputFilePath, transpose, inMemory, isolatedVertices, direction);
        assertArrayEquals(expected, g.successorArray(5));
    }

    @Test
    void testLoadGraph_TransposedGraphOutDirection() throws IOException {
        String inputFilePath = new File("src/test/data/g_directed/32t-path.graph").getAbsolutePath();
        inputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        boolean transpose = true;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        boolean webGraph = true;
        boolean compressedGraph = false;
        String direction = "out";

        int[] expected = new int[]{4};
        GraphManager g =new GraphManager(webGraph,compressedGraph,inputFilePath, transpose, inMemory, isolatedVertices, direction);
        assertArrayEquals(expected, g.successorArray(5));
    }

    @Test
    void testLoadGraph_ThrowsException() {
        String inputFilePath = "/nonexistent/graph/path/file";
        boolean transpose = false;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        boolean webGraph = true;
        boolean compressedGraph = false;
        String direction = "out";

        Assertions.assertThrows(IOException.class, () -> new GraphManager(webGraph,compressedGraph,inputFilePath, transpose, inMemory, isolatedVertices, direction));

    }


}