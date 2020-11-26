package it.bigdatalab.utils;

import it.unimi.dsi.webgraph.ImmutableGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GraphUtilsTest {

    @Test
    void testLoadGraph_InDirection() throws IOException {
        String inputFilePath = new File("src/test/data/32-path.graph").getAbsolutePath();
        inputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        boolean transpose = false;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        String direction = "in";

        int[] expected = new int[]{3};
        ImmutableGraph g = GraphUtils.loadGraph(inputFilePath, transpose, inMemory, isolatedVertices, direction);
        assertArrayEquals(expected, g.successorArray(2));
    }

    @Test
    void testLoadGraph_OutDirection() throws IOException {
        String inputFilePath = new File("src/test/data/32-path.graph").getAbsolutePath();
        inputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        boolean transpose = false;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        String direction = "out";

        int[] expected = new int[]{2};
        ImmutableGraph g = GraphUtils.loadGraph(inputFilePath, transpose, inMemory, isolatedVertices, direction);
        assertArrayEquals(expected, g.successorArray(3));
    }

    @Test
    void testLoadGraph_TransposedGraphInDirection() throws IOException {
        String inputFilePath = new File("src/test/data/32t-path.graph").getAbsolutePath();
        inputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        boolean transpose = true;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        String direction = "in";

        int[] expected = new int[]{6};
        ImmutableGraph g = GraphUtils.loadGraph(inputFilePath, transpose, inMemory, isolatedVertices, direction);
        assertArrayEquals(expected, g.successorArray(5));
    }

    @Test
    void testLoadGraph_TransposedGraphOutDirection() throws IOException {
        String inputFilePath = new File("src/test/data/32t-path.graph").getAbsolutePath();
        inputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        boolean transpose = true;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        String direction = "out";

        int[] expected = new int[]{4};
        ImmutableGraph g = GraphUtils.loadGraph(inputFilePath, transpose, inMemory, isolatedVertices, direction);
        assertArrayEquals(expected, g.successorArray(5));
    }

    @Test
    void testLoadGraph_ThrowsException() {
        String inputFilePath = "/nonexistent/graph/path/file";
        boolean transpose = false;
        boolean inMemory = true;
        boolean isolatedVertices = true;
        String direction = "out";

        Assertions.assertThrows(IOException.class, () -> GraphUtils.loadGraph(inputFilePath, transpose, inMemory, isolatedVertices, direction));
    }
}