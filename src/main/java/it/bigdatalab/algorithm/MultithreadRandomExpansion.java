package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.utils.Constants;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultithreadRandomExpansion extends BMinHashOpt {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadExpansion");

    private final int mNumberOfThreads;
    private final double[] mSeedTime;
    private final boolean mDoCentrality;
    private final boolean mUnnormalized;
    private final float rndT;
    private double[][] mHarmonic;

    /**
     * Creates a new MultithreadExpansion instance with default values
     */
    public MultithreadRandomExpansion(final ImmutableGraph g, int numSeeds, float t, int[] nodes, int threads, boolean normalized) {
        super(g, numSeeds, nodes);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mSeedTime = new double[mNumSeeds];
        this.mUnnormalized = normalized;
        this.rndT = t;
    }

    /**
     * Creates a new MultithreadExpansion instance with default values
     */
    public MultithreadRandomExpansion(final ImmutableGraph g, int numSeeds, float t, int threads, boolean normalized) {
        super(g, numSeeds);
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mSeedTime = new double[mNumSeeds];
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
        this.mUnnormalized = normalized;
        this.rndT = t;
    }

    /**
     * Number of max threads to use for the computation
     *
     * @param suggestedNumberOfThreads if not equal to zero return the number of threads
     *                                 passed as parameter, else the number of max threads available
     * @return number of threads to use for the computation
     */
    private static int getNumberOfMaxThreads(int suggestedNumberOfThreads) {
        if (suggestedNumberOfThreads > 0) return suggestedNumberOfThreads;
        return Runtime.getRuntime().availableProcessors();
    }

    public Measure runAlgorithm() throws IOException {
        long startTime = System.currentTimeMillis();
        long totalTime;

        int[] vs_active = new int[mNumSeeds];
        for (int i = 0; i < mNumSeeds; i++) {
            vs_active[i] = i;
        }
        int d = mNumSeeds / mNumberOfThreads;
        int r = mNumSeeds % mNumberOfThreads;
        int ntasks = (d == 0) ? r : mNumberOfThreads;
        logger.debug("Number of threads to be used {}", ntasks);


        int task_size = (int) Math.ceil((double) mNumSeeds / ntasks);

        if (mDoCentrality) {

            mHarmonic = new double[ntasks][mGraph.numNodes()];

        }
        ExecutorService executor = Executors.newFixedThreadPool(ntasks); //creating a pool of threads
        List<MultithreadRandomExpansion.IterationThread> todo = new ArrayList<>(ntasks);
        for (int t = 0; t < ntasks; t++) {
            int start = t * task_size;
            int end = Math.min((t + 1) * task_size, mNumSeeds);
            //logger.debug("Thread {}/{} start {} end {} ",t,ntasks,start,end);
            todo.add(new MultithreadRandomExpansion.IterationThread(mGraph.copy(),start,end,t));
        }

        try {
            executor.invokeAll(todo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Reduction phase

        double[] harmonic = new double[0];
        double[] unnorm_harmonic = new double[0];

        harmonic = new double[mGraph.numNodes()];
        unnorm_harmonic = new double[mGraph.numNodes()];


        for (int i = 0; i < mGraph.numNodes(); i++) {
            for (int j = 0; j < ntasks; j++) {
                harmonic[i] += mHarmonic[j][i];
            }

            if (mUnnormalized) {
                unnorm_harmonic[i] = harmonic[i] * mGraph.numNodes() / mNumSeeds / rndT;
            }
            harmonic[i] = harmonic[i] * mGraph.numNodes() / (mGraph.numNodes() - 1) / mNumSeeds / rndT;

        }

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumSeeds(mNumSeeds);

        graphMeasure.setHarmonicCentrality(harmonic);
        if (mUnnormalized) {
            graphMeasure.setHarmonicCentralityUnnorm(unnorm_harmonic);
        }

        }
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setSeedsTime(mSeedTime);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);

        return graphMeasure;


    }

    class IterationThread implements Callable<Integer> {
        private final ImmutableGraph g;
        private final int start;
        private final int end;
        private final int index;

        public IterationThread(ImmutableGraph g, int start, int end, int index) {
            this.g = g;
            this.start = start;
            this.end = end;
            this.index = index;
        }

        @Override
        public Integer call() {


            int[] p_prev = new int[1];
            int[] p_next = new int[1];
            int[] expanded = new int[1];
            int[] visited = new int[g.numNodes()];
            int randomNode = -1;
            int task_id = 0;
            int remainderPositionNeigh;
            int quotientNeigh;

            int remainderPositionNode;
            int quotientNode;
            int node;
            int bit;
            int h;
            int h_max;
            double r;
            boolean signatureIsChanged;

            for (int s = start; s < end; s++) {
                // Initializing variables
                p_prev = new int[lengthBitsArray(g.numNodes())];
                p_next = new int[lengthBitsArray(g.numNodes())];
                expanded = new int[lengthBitsArray(g.numNodes())];
                r = Math.random();
                h_max = (int) Math.floor(rndT / r);

                Arrays.fill(visited, 0);
                randomNode = mMinHashNodeIDs[s];
                visited[randomNode] = 1;

                h = 0;
                signatureIsChanged = true;


                while (signatureIsChanged) {
                    //first hop - initialization
                    if (h == 0) {

                        // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                        // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                        // This is equal to logical and operation.
                        // remaremainderPositionRandomNode contains the bit index of the node
                        int remainderPositionRandomNode = (randomNode << Constants.REMAINDER) >>> Constants.REMAINDER;
                        // quotient is randomNode >>> MASK and give us the position of the node in the array
                        // i.e if the actual node is 16 and we use an array of int (32 bit lenght for each cell) then
                        // the node is at index 0 of the array of the first int from 0 to 31
                        p_next[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
                    } else { // next hops
                        signatureIsChanged = false;

                        // copy all the actual nodes hash in a new structure
                        System.arraycopy(p_next, 0, p_prev, 0, p_next.length);

                        for (int index = 0; index < p_prev.length; index++) {
                            if (p_prev[index] != 0) { // trick
                                for (int bitPosition = 0; bitPosition < Integer.SIZE; bitPosition++) {
                                    bit = (p_prev[index] >> bitPosition) & Constants.BIT; // todo maybe there's another solution?
                                    int bitExpanded = (expanded[index] >> bitPosition) & 1;

                                    if (bit == 1 && bitExpanded != 1) {
                                        node = (index * Integer.SIZE) + bitPosition;

                                        quotientNode = node >>> Constants.MASK; // position into array
                                        remainderPositionNode = (node << Constants.REMAINDER) >>> Constants.REMAINDER;
                                        expanded[quotientNode] |= (Constants.BIT) << remainderPositionNode;

                                        final int d = g.outdegree(node);
                                        final int[] successors = g.successorArray(node);
                                        for (int l = 0; l < d; l++) {
                                            final int neighbour = successors[l];
                                            quotientNeigh = neighbour >>> Constants.MASK; // position into array
                                            remainderPositionNeigh = (neighbour << Constants.REMAINDER) >>> Constants.REMAINDER;
                                            p_next[quotientNeigh] |= (Constants.BIT) << remainderPositionNeigh;
                                            if ((p_next[quotientNeigh] ^ p_prev[quotientNeigh]) != 0) {

                                                if (visited[neighbour] != 1) {
                                                    visited[neighbour] = 1;
                                                    mHarmonic[this.index][neighbour] += 1.0;

                                                }

                                                }
                                                signatureIsChanged = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // We don't need to count the collision, this algorithm is specific for the centrality.
                    if (h > h_max) {
                        signatureIsChanged = false;
                    }
                }
                task_id += 1;

            }

            return 0;

        }


    }
}
