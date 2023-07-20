package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.GraphUtils;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class MultithreadExpansionTest extends AlgoTest {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadExpansionTest");

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("cycleProvider")
    void testAlgorithm_DiCycle(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_directed/32-cycle.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(false)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("pathProvider")
    void testAlgorithm_DiPath(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_directed/32-path.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setComputeCentrality(false)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("tPathProvider")
    void testAlgorithm_DiTPath(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_directed/32t-path.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(true)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(false)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("inStarProvider")
    void testAlgorithm_DiInStar(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_directed/32in-star.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("outStarProvider")
    void testAlgorithm_DiOutStar(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_directed/32out-star.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(false)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("unCycleProvider")
    void testAlgorithm_UnCycle(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_undirected/32-cycle.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(false)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("unWheelProvider")
    void testAlgorithm_UnWheel(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_undirected/32-wheel.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(false)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("completeProvider")
    void testAlgorithm_Complete(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_undirected/32-complete.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(false)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("unCycleProvider")
    void testAlgorithm_UnCycle_checkSizeCollisionHopTable(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_undirected/32-cycle.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(false)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());

        GraphMeasureOpt measure = (GraphMeasureOpt) algo.runAlgorithm();

        // check hop table size (equals to lower bound + 1)
        // check collisions table # rows (equals to lower bound + 1)
        // check collisions table # cols (equals to # seed)
        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(measure.getLastHops()).as("Last hops size").hasSize(seeds.length);
        assertions.assertThat(measure.getHopTable()).as("HopTable size").hasSize(measure.getLowerBoundDiameter() + 1);
        assertions.assertThat(measure.getCollisionsMatrix()).as("CollisionsTable # rows # cols").hasDimensions(seeds.length, measure.getLowerBoundDiameter() + 1);
        assertions.assertAll();
    }

    @Test
    void testNormalizeCollisionsTable() {
        int[][] collisionMatrix = new int[][]{{1, 4, 32, 55, 98, 101, 201}, {1, 4}, {1}, {1, 32}};
        int nrows = 4;
        int lowerBoundDiameter = 6;
        MultithreadExpansion algo = new MultithreadExpansion(null, 4, 0.9, new int[]{0, 1, 2, 3}, 1, false);
        algo.normalizeCollisionsTable(collisionMatrix, lowerBoundDiameter);
        assertThat(collisionMatrix).as("CollisionsTable # rows # cols").hasDimensions(nrows, lowerBoundDiameter + 1);
    }
}
