package it.bigdatalab.applications;

import it.bigdatalab.model.Parameter;
import it.bigdatalab.model.SeedNode;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateSeedsTest {

    @Test
    void testGenerate_throwsException() {
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph("/nonexistent/graph/path/file")
                .setOutputFolderPath("/test/output/folder/path")
                .setNumTests(2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .build();

        CreateSeeds c = new CreateSeeds(param);
        Assertions.assertThrows(IOException.class, c::generate);
    }

    @Test
    void testGenerate_correctNumberOfLists() throws IOException {
        String path = new File("src/test/data/32-complete.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setOutputFolderPath("/test/output/folder/path")
                .setNumTests(2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .build();

        CreateSeeds c = new CreateSeeds(param);
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