package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of MultithreadBMinHash (MinHash Signature Estimation multithread boolean version) algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class MultithreadBMinHash extends BMinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHash");

    private final int mNumberOfThreads;
    private final double[] mSeedTime;
    private long startTime;

    /**
     * Creates a new MultithreadBMinHash instance with default values
     */
    public MultithreadBMinHash(final GraphManager g, int numSeeds, double threshold, int[] nodes, int threads) {
        super(g, numSeeds, threshold, nodes);
        mSeedTime = new double[mNumSeeds];
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
    }

    /**
     * Creates a new MultithreadBMinHash instance with default values
     */
    public MultithreadBMinHash(final GraphManager g, int numSeeds, double threshold, int threads) {
        super(g, numSeeds, threshold);
        mSeedTime = new double[mNumSeeds];
        this.mNumberOfThreads = getNumberOfMaxThreads(threads);
        this.mMinHashNodeIDs = CreateSeeds.genNodes(mNumSeeds, mGraph.numNodes());
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

    /**
     * Execution of the MultithreadBMinHash algorithm
     *
     * @return Computed metrics of the algorithm
     */

    public Measure runAlgorithm() throws CloneNotSupportedException {
        logger.info("Running {} algorithm", MultithreadBMinHash.class.getName());
        startTime = System.currentTimeMillis();
        long totalTime;

        Int2ObjectOpenHashMap<int[]> collisionsTable = new Int2ObjectOpenHashMap<>();
        int[] lastHops = new int[mNumSeeds];

        logger.debug("Number of threads to be used {}", mNumberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(mNumberOfThreads); //creating a pool of threads
        List<IterationThread> todo = new ArrayList<>(this.mNumSeeds);

        for (int i = 0; i < this.mNumSeeds; i++) {
            if(mGraph.isWebGraph()){
                todo.add(new IterationThread((GraphManager) mGraph.clone(), i));

            }else {
                todo.add(new IterationThread(mGraph, i));
            }
        }

        try {
            List<Future<Int2LongLinkedOpenHashMap>> futures = executor.invokeAll(todo);
            for (int s = 0; s < this.mNumSeeds; s++) {
                Future<Int2LongLinkedOpenHashMap> future = futures.get(s);
                if (!future.isCancelled()) {
                    try {
                        Int2LongLinkedOpenHashMap actualCollisionTable = future.get();
                        int[] hopCollision;

                        for (int h : actualCollisionTable.keySet()) {
                            if (!collisionsTable.containsKey(h)) {
                                hopCollision = new int[mNumSeeds];
                                hopCollision[s] = (int) actualCollisionTable.get(h);
                                collisionsTable.put(h, hopCollision);
                            } else {
                                hopCollision = collisionsTable.get(h);
                                hopCollision[s] = (int) actualCollisionTable.get(h);
                                collisionsTable.put(h, hopCollision);
                            }
                        }

                        lastHops[s] = actualCollisionTable.size() - 1;

                    } catch (ExecutionException e) {
                        logger.error("Failed to get result", e);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                } else {
                    //TODO Implement better error management
                    logger.error("Future is cancelled!");
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        normalizeCollisionsTable(collisionsTable);

        Int2DoubleLinkedOpenHashMap hopTable = hopTable(collisionsTable);
        logger.debug("Hop table derived from collision table: {}", hopTable);

        GraphMeasure graphMeasure = new GraphMeasure();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setHopTable(hopTable);
        graphMeasure.setLowerBoundDiameter(hopTable.size() - 1);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setCollisionsTable(collisionsTable);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setSeedsTime(mSeedTime);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTable));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTable, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTable));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTable, mThreshold));

        return graphMeasure;
    }

    class IterationThread implements Callable<Int2LongLinkedOpenHashMap> {

        private final GraphManager g;
        private final int s;

        public IterationThread(GraphManager g, int s) {
            this.g = g;
            this.s = s;
        }

        @Override
        public Int2LongLinkedOpenHashMap call() {
            long startSeedTime = System.currentTimeMillis();
            long lastLogTime = startSeedTime;
            long logTime;

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            Int2LongLinkedOpenHashMap hopCollision = new Int2LongLinkedOpenHashMap();
            int collisions;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            int[] mutable = new int[lengthBitsArray(g.numNodes())];
            int[] immutable = new int[lengthBitsArray(g.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            //It could be set in mhse.properties file with the "minhash.nodeIDs" property
            int randomNode = mMinHashNodeIDs[s];

            int h = 0;
            boolean signatureIsChanged = true;

            while (signatureIsChanged) {
                //first hop - initialization
                if (h == 0) {
                    // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
                    // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
                    // This is equal to logical and operation.
                    int remainderPositionRandomNode = ((randomNode << Constants.REMAINDER) >>> Constants.REMAINDER);
                    // quotient is randomNode >>> MASK
                    mutable[randomNode >>> Constants.MASK] |= (Constants.BIT) << remainderPositionRandomNode;
                    signatureIsChanged = true;
                } else {
                    signatureIsChanged = false;

                    // copy all the actual nodes hash in a new structure
                    System.arraycopy(mutable, 0, immutable, 0, mutable.length);

                    int remainderPositionNode;
                    int quotientNode;
                    for (int n = 0; n < g.numNodes(); n++) {

                        final int node = n;
                        final int [] successors = mGraph.get_neighbours(node);
                        final int d = successors.length;
                        // update the node hash iterating over all its neighbors
                        // and computing the OR between the node signature and
                        // the neighbor signature.
                        // store the new signature as the current one
                        remainderPositionNode = (node << Constants.REMAINDER) >>> Constants.REMAINDER;
                        quotientNode = node >>> Constants.MASK;

                        int value = immutable[quotientNode];
                        int bitNeigh;
                        int nodeMask = (1 << remainderPositionNode);

                        if (((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                            for (int l = 0; l < d; l++) {
                                final int neighbour = successors[l];
                                int quotientNeigh = neighbour >>> Constants.MASK;
                                int remainderPositionNeigh = (neighbour << Constants.REMAINDER) >>> Constants.REMAINDER;

                                bitNeigh = (((1 << remainderPositionNeigh) & immutable[quotientNeigh]) >>> remainderPositionNeigh) << remainderPositionNode;
                                value = bitNeigh | nodeMask & immutable[quotientNode];
                                if ((value >>> remainderPositionNode) == 1) {
                                    signatureIsChanged = true;
                                    break;
                                }
                            }
                        }
                        mutable[quotientNode] = mutable[quotientNode] | value;

                        logTime = System.currentTimeMillis();
                        if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                            logger.info("(seed # {}) # nodes analyzed {} / {} for hop {}, estimated time remaining {}",
                                    s,
                                    n, mGraph.numNodes(),
                                    h + 1,
                                    String.format("%d min, %d sec",
                                            TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)),
                                            TimeUnit.MILLISECONDS.toSeconds(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)) -
                                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - MultithreadBMinHash.this.startTime)) / (s + 1)) - (logTime - MultithreadBMinHash.this.startTime)))));
                            lastLogTime = logTime;
                        }
                    }
                }

                if (signatureIsChanged) {
                    // count the collision between the node signature and the graph signature
                    collisions = 0;
                    for (int aMutable : mutable) {
                        collisions += Integer.bitCount(aMutable);
                    }
                    hopCollision.put(h, collisions);
                    h += 1;
                }
            }

            MultithreadBMinHash.this.mSeedTime[s] = (System.currentTimeMillis() - startSeedTime);
            return hopCollision;
        }

    }


}


