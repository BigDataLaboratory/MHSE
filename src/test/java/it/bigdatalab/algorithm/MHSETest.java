package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.GraphUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class MHSETest extends AlgoTest {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MHSE");

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("cycleProvider")
    public void testAlgorithm_DiCycle(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("pathProvider")
    public void testAlgorithm_DiPath(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
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
                .setThreshold(0.9)
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));


        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));

        Measure measure = algo.runAlgorithm();

        assertThat(measure)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("inStarProvider")
    void testAlgorithm_DiInStar_checkSizeCollisionHopTable(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {
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
                .build();

        ImmutableGraph g = GraphUtils.loadGraph(param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(), param.keepIsolatedVertices(), param.getDirection());

        MHSE algo = new MHSE(g, param.getNumSeeds(), param.getThreshold(), new IntArrayList(seeds));

        GraphMeasure measure = (GraphMeasure) algo.runAlgorithm();

        // check hop table size (equals to lower bound + 1)
        SoftAssertions hopAndCollision = new SoftAssertions();
        hopAndCollision.assertThat(measure.getHopTable()).as("HopTable size").hasSize(measure.getLowerBoundDiameter() + 1);
        hopAndCollision.assertAll();
    }
}