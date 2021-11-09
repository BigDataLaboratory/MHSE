package it.bigdatalab.applications;

import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MinHashMainTest {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHashMainTest");

    private static Stream<Arguments> runProvider() {
        return Stream.of(
                // execute MHSE with in direction, one run, 4 random seeds, input graph (with isolated nodes, if there) is not transposed but loaded in memory, threshold is 0.9, 1 thread (local computation)
                Arguments.of("MHSE", "src/test/data/g_directed/32-path.graph", "test/output/folder/path", 1, 4, false, true, true, "", true, null, 0.9, "in", 1),
                // execute SEHSE with out direction, five run, 8 random seeds, input graph (with isolated nodes, if there) is not transposed but loaded in memory, threshold is 0.9, 1 thread (local computation)
                Arguments.of("SEMHSE", "src/test/data/g_directed/32in-star.graph", "test/output/folder/path", 5, 8, false, true, true, "", true, null, 0.9, "out", 1),
                // execute StandaloneBMinHash with in direction, one run, # seed is 0 because a range is specified (so seeds are not random), input graph (without isolated nodes, if there) is not transposed but loaded in memory, threshold is 0.9, 1 thread (local computation)
                Arguments.of("SEBMHSE", "src/test/data/g_directed/32-cycle.graph", "test/output/folder/path", 1, 32, false, true, false, null, true, new int[]{0, 31}, 0.9, "in", 1),
                // execute MultithreadBMinhash with out direction, seven run, # (random) seed is 16, input graph (without isolated nodes, if there) is not transposed but loaded in memory, threshold is 0.9, 2 thread (parallel computation)
                Arguments.of("SEBMHSEMulti", "src/test/data/g_undirected/32-wheel.graph", "test/output/folder/path", 7, 16, false, true, true, "", false, null, 0.9, "out", 2)
        );
    }

    private static Stream<Arguments> runProviderForException() {
        return Stream.of(
                // execute StandaloneBMinHash with in direction, four run, # seed is 0 because a range is specified (so seeds are not random), input graph (without isolated nodes, if there) is not transposed but loaded in memory, threshold is 0.9, 1 thread (local computation)
                Arguments.of("SEBMHSE", "src/test/data/g_directed/32-cycle.graph", "test/output/folder/path", 4, 32, false, true, false, null, true, new int[]{0, 31}, 0.9, "in", 1)
        );
    }

    private static Stream<Arguments> runProviderForAlgorithmException() {
        return Stream.of(
                // execute NonExistingAlgorithm with out direction, seven run, # (random) seed is 16, input graph (without isolated nodes, if there) is not transposed but loaded in memory, threshold is 0.9, 2 thread (parallel computation)
                Arguments.of("NonExistingAlgorithm", "src/test/data/g_undirected/32-wheel.graph", "test/output/folder/path", 7, 16, false, true, true, "", false, null, 0.9, "out", 2)
        );
    }

    @ParameterizedTest(name = "{index} => algorithmName={0}, inputFilePath={1}, outputFolderPath={2}, numTests={3}," +
            " numSeeds={4}, transpose={5}, inMemory={6}, isSeedsRandom={7}, inputFilePathSeedNode={8}, isolatedVertices={9}, range={10}, threshold={11}, direction={12}, suggestedNumberOfThreads={13}")
    @MethodSource("runProvider")
    void testRunAlgorithm(String algorithmName, String inputFilePath, String outputFolderPath,
                          int numTests, int numSeeds, boolean transpose,
                          boolean inMemory, boolean isSeedsRandom, String inputFilePathSeedNode,
                          boolean isolatedVertices, int[] range, double threshold, String direction, int suggestedNumberOfThreads) throws IOException {
        String path = new File(inputFilePath).getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        Parameter param = new Parameter.Builder()
                .setAlgorithmName(algorithmName)
                .setInputFilePathGraph(path)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(numSeeds)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setSeedsRandom(isSeedsRandom)
                .setInputFilePathSeedNode(inputFilePathSeedNode)
                .setIsolatedVertices(isolatedVertices)
                .setRange(range)
                .setThreshold(threshold)
                .setDirection(direction)
                .setNumThreads(suggestedNumberOfThreads)
                .build();

        MinHashMain m = new MinHashMain(param);
        List<Measure> measures = m.run();
        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(measures.size()).isEqualTo(param.getNumTests());
        assertions.assertThat(measures).extracting(Measure::getAlgorithmName).containsOnly(param.getAlgorithmName());
        assertions.assertThat(measures).extracting(Measure::getDirection).containsOnly(param.getDirection());
        assertions.assertAll();
    }

    @ParameterizedTest(name = "{index} => algorithmName={0}, inputFilePath={1}, outputFolderPath={2}, numTests={3}," +
            " numSeeds={4}, transpose={5}, inMemory={6}, isSeedsRandom={7}, inputFilePathSeedNode={8}, isolatedVertices={9}, range={10}, threshold={11}, direction={12}, suggestedNumberOfThreads={13}")
    @MethodSource("runProviderForException")
    void testRunAlgorithm_throwsException_numTestGreaterThanNumberOfListSeedsNodes(String algorithmName, String inputFilePath, String outputFolderPath,
                                                                                   int numTests, int numSeeds, boolean transpose,
                                                                                   boolean inMemory, boolean isSeedsRandom, String inputFilePathSeedNode,
                                                                                   boolean isolatedVertices, int[] range, double threshold, String direction, int suggestedNumberOfThreads) throws IOException {
        String path = new File(inputFilePath).getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        Parameter param = new Parameter.Builder()
                .setAlgorithmName(algorithmName)
                .setInputFilePathGraph(path)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(numSeeds)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setSeedsRandom(isSeedsRandom)
                .setInputFilePathSeedNode(inputFilePathSeedNode)
                .setIsolatedVertices(isolatedVertices)
                .setRange(range)
                .setThreshold(threshold)
                .setDirection(direction)
                .setNumThreads(suggestedNumberOfThreads)
                .build();

        MinHashMain m = new MinHashMain(param);
        assertThrows(IllegalStateException.class, m::run);
    }

    @ParameterizedTest(name = "{index} => algorithmName={0}, inputFilePath={1}, outputFolderPath={2}, numTests={3}," +
            " numSeeds={4}, transpose={5}, inMemory={6}, isSeedsRandom={7}, inputFilePathSeedNode={8}, isolatedVertices={9}, range={10}, threshold={11}, direction={12}, suggestedNumberOfThreads={13}")
    @MethodSource("runProviderForAlgorithmException")
    void testRunAlgorithm_throwsException_NonExistingAlgorithm(String algorithmName, String inputFilePath, String outputFolderPath,
                                                               int numTests, int numSeeds, boolean transpose,
                                                               boolean inMemory, boolean isSeedsRandom, String inputFilePathSeedNode,
                                                               boolean isolatedVertices, int[] range, double threshold, String direction, int suggestedNumberOfThreads) throws IOException {
        String path = new File(inputFilePath).getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));

        Parameter param = new Parameter.Builder()
                .setAlgorithmName(algorithmName)
                .setInputFilePathGraph(path)
                .setOutputFolderPath(outputFolderPath)
                .setNumTests(numTests)
                .setNumSeeds(numSeeds)
                .setTranspose(transpose)
                .setInMemory(inMemory)
                .setSeedsRandom(isSeedsRandom)
                .setInputFilePathSeedNode(inputFilePathSeedNode)
                .setIsolatedVertices(isolatedVertices)
                .setRange(range)
                .setThreshold(threshold)
                .setDirection(direction)
                .setNumThreads(suggestedNumberOfThreads)
                .build();

        MinHashMain m = new MinHashMain(param);
        assertThrows(IllegalArgumentException.class, m::run);
    }
}