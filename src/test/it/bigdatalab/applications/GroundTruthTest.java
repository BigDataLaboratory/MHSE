package it.bigdatalab.applications;

import it.bigdatalab.model.GraphGtMeasure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.GraphUtils;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroundTruthTest {

    /*******************************************************************************
     *                                 32 PATH
     * ****************************************************************************/

    @Test
    void testGt_webGraph_32Path() throws IOException {
        String path = new File("src/test/data/g_directed/32-path.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "WebGraph";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(31, graphMeasure.getDiameter());
    }

    @Test
    void testGt_bfs_32Path() throws IOException {
        String path = new File("src/test/data/g_directed/32-path.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "BFS";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(31, graphMeasure.getDiameter());
    }

    /*******************************************************************************
     *                                 32 COMPLETE GRAPH
     * ****************************************************************************/


    @Test
    void testGt_webGraph_32Complete() throws IOException {
        String path = new File("src/test/data/g_undirected/32-complete.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "WebGraph";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(1, graphMeasure.getDiameter());
    }

    @Test
    void testGt_bfs_32Complete() throws IOException {
        String path = new File("src/test/data/g_undirected/32-complete.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "BFS";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(1, graphMeasure.getDiameter());
    }

    /*******************************************************************************
     *                                 32 CYCLE
     * ****************************************************************************/

    @Test
    void testGt_webGraph_32Cycle() throws IOException {
        String path = new File("src/test/data/g_directed/32-cycle.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "WebGraph";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(31, graphMeasure.getDiameter());
    }

    @Test
    void testGt_bfs_32Cycle() throws IOException {
        String path = new File("src/test/data/g_directed/32-cycle.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "BFS";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(31, graphMeasure.getDiameter());
    }

    /*******************************************************************************
     *                                 32 STAR
     * ****************************************************************************/

    @Test
    void testGt_webGraph_32InStar() throws IOException {
        String path = new File("src/test/data/g_directed/32in-star.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "WebGraph";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(1, graphMeasure.getDiameter());
    }

    @Test
    void testGt_bfs_32InStar() throws IOException {
        String path = new File("src/test/data/g_directed/32in-star.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "BFS";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(1, graphMeasure.getDiameter());
    }

    /*******************************************************************************
     *                                 32 WHEEL
     * ****************************************************************************/

    @Test
    void testGt_webGraph_32Wheel() throws IOException {
        String path = new File("src/test/data/g_undirected/32-wheel.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "WebGraph";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(2, graphMeasure.getDiameter());
    }

    @Test
    void testGt_bfs_32Wheel() throws IOException {
        String path = new File("src/test/data/g_undirected/32-wheel.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "BFS";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        GraphGtMeasure graphMeasure = gt.computeGroundTruth();
        assertEquals(2, graphMeasure.getDiameter());
    }

    /*******************************************************************************
     *                                 GENERIC
     * ****************************************************************************/


    @Test
    void testGt_throwsException_notExistingMode() throws IOException {
        String path = new File("src/test/data/g_undirected/32-wheel.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        String mode = "NotExisting";
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isInMemory(), param.keepIsolatedVertices());

        GroundTruth gt = new GroundTruth(g, param, mode);
        Assertions.assertThrows(IllegalArgumentException.class, gt::computeGroundTruth);

    }
}