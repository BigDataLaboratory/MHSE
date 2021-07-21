package it.bigdatalab.applications;

import it.bigdatalab.model.Parameter;
import it.bigdatalab.model.SeedNode;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.GraphUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateSeedsTest {

    @Test
    void testGenerate_correctNumberOfLists() throws IOException {
        String path = new File("src/test/data/g_undirected/32-complete.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setNumTests(2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setWebG(true)
                .setCompG(false)
                .setTranspose(false)
                .setDirection("out")
                .build();

        GraphManager g = new GraphManager(param.getWebGraph(), param.getCompGraph(),param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(),param.keepIsolatedVertices(),param.getDirection());


        CreateSeeds c = new CreateSeeds(g, param);
        List<SeedNode> seedNodes = c.generate();
        assertEquals(param.getNumTests(), seedNodes.size(), "it have to create a number of list of seeds/nodes as specified in properties file");
    }

    @Test
    void testGenSeeds_correctSize() {
        int numSeeds = 8;
        IntArrayList seeds = CreateSeeds.genSeeds(numSeeds);
        assertEquals(numSeeds, seeds.size());
    }

    @Test
    void testGenSeeds_differentValuesInList() {
        int numSeeds = 8;
        IntArrayList seeds = CreateSeeds.genSeeds(numSeeds);
        assertEquals(new HashSet<>(seeds).size(), seeds.size());
    }
}