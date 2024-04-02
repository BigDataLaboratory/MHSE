package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.GraphUtils;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MHSEXTest extends AlgoTest {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MHSEBSideTest");

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
                .setComputeCentrality(false)
                .setThreshold(0.9)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());

        GraphMeasureOpt measure = (GraphMeasureOpt) algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHarmonicCentrality", "mLinnCentrality", "mClosenessCentrality", "mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHarmonicCentrality", "mLinnCentrality", "mClosenessCentrality", "mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .setComputeCentrality(false)
                .setThreshold(0.9)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHarmonicCentrality", "mLinnCentrality", "mClosenessCentrality", "mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .setComputeCentrality(false)
                .setThreshold(0.9)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHarmonicCentrality", "mLinnCentrality", "mClosenessCentrality", "mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .setComputeCentrality(false)
                .setThreshold(0.9)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHarmonicCentrality", "mLinnCentrality", "mClosenessCentrality", "mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .setComputeCentrality(false)
                .setThreshold(0.9)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHarmonicCentrality", "mLinnCentrality", "mClosenessCentrality", "mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .setComputeCentrality(false)
                .setThreshold(0.9)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHarmonicCentrality", "mLinnCentrality", "mClosenessCentrality", "mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .setComputeCentrality(false)
                .setThreshold(0.9)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mHarmonicCentrality", "mLinnCentrality", "mClosenessCentrality", "mHopForNode", "mCollisionsMatrix", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @Test
    void testLenghtBitsArray() {
        MHSEX algo = new MHSEX(null, 4, 0.9, new int[]{0, 1, 2, 3}, false);
        int expected = 1;
        int actual = algo.lengthBitsArray(20);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("unCyclefarnessProvider")
    void testfarness_UnCycle(String direction, int[] seeds, int[] nodes, int[] expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_undirected/32-cycle.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(false)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(true)
                .setNumThreads(1)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());
        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());
        Measure measure = algo.runAlgorithm();

        int[] farness = measure.getFarness();
        assertThat(expected).containsExactly(farness);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("cyclefarnessProvider")
    void testfarness_DiCycle(String direction, int[] seeds, int[] nodes, int[] expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_directed/32-cycle.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(false)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(true)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());
        MultithreadExpansion algo = new MultithreadExpansion(g, param.getNumSeeds(), param.getThreshold(), nodes, param.getNumThreads(), param.computeCentrality());
        Measure measure = algo.runAlgorithm();

        int[] farness = measure.getFarness();
        assertThat(expected).containsExactly(farness);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("unWheelfarnessProvider")
    void testfarness_UnWheel(String direction, int[] seeds, int[] nodes, int[] expected) throws IOException, MinHash.SeedsException {
        String path = new File("src/test/data/g_undirected/32-wheel.graph").getAbsolutePath();
        path = path.substring(0, path.lastIndexOf('.'));
        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(path)
                .setIsolatedVertices(false)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setThreshold(0.9)
                .setComputeCentrality(true)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());
        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());
        Measure measure = algo.runAlgorithm();

        int[] farness = measure.getFarness();
        assertThat(expected).containsExactly(farness);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("pathfarnessProvider")
    void testfarness_DiPath(String direction, int[] seeds, int[] nodes, int[] expected) throws IOException, MinHash.SeedsException {
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
                .setComputeCentrality(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());
        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());
        Measure measure = algo.runAlgorithm();

        int[] farness = measure.getFarness();
        assertThat(expected).containsExactly(farness);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("tPathfarnessProvider")
    void testAlgorithm_DiTPath(String direction, int[] seeds, int[] nodes, int[] expected) throws IOException, MinHash.SeedsException {
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
                .setComputeCentrality(true)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());
        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());
        Measure measure = algo.runAlgorithm();

        int[] farness = measure.getFarness();
        assertThat(expected).containsExactly(farness);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("inStarfarnessProvider")
    void testfarness_DiInStar(String direction, int[] seeds, int[] nodes, int[] expected) throws IOException, MinHash.SeedsException {
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
                .setComputeCentrality(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());
        logger.debug("nodes {}", g.numNodes());
        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());
        Measure measure = algo.runAlgorithm();

        int[] farness = measure.getFarness();
        assertThat(expected).containsExactly(farness);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("outStarfarnessProvider")
    void testfarness_DiOutStar(String direction, int[] seeds, int[] nodes, int[] expected) throws IOException, MinHash.SeedsException {
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
                .setComputeCentrality(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());
        MHSEX algo = new MHSEX(g, param.getNumSeeds(), param.getThreshold(), nodes, param.computeCentrality());
        Measure measure = algo.runAlgorithm();

        int[] farness = measure.getFarness();
        assertThat(expected).containsExactly(farness);
    }
}