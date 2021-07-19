package it.bigdatalab.algorithm;

import it.bigdatalab.applications.CreateSeeds;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of SE-MHSE (Space Efficient - MinHash Signature Estimation) algorithm
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class SEMHSE extends MinHash {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.SEMHSE");

    private Int2LongOpenHashMap hashes;
    private Int2LongOpenHashMap oldHashes;
    private final long[] graphSignature;

    /**
     * Creates a SE-MHSE instance with default values
     */
    public SEMHSE(GraphManager g, int numSeeds, double threshold, IntArrayList seeds) throws SeedsException {
        super(g, numSeeds, threshold, seeds);
        graphSignature = new long[mNumSeeds];
        //initialize graph signature with Long.MAX_VALUE
        Arrays.fill(graphSignature, Long.MAX_VALUE);
    }

    /**
     * Creates a SE-MHSE instance with default values
     */
    public SEMHSE(GraphManager g, int numSeeds, double threshold) throws SeedsException {
        super(g, numSeeds, threshold);
        this.mSeeds = CreateSeeds.genSeeds(mNumSeeds);
        graphSignature = new long[mNumSeeds];
        //initialize graph signature with Long.MAX_VALUE
        Arrays.fill(graphSignature, Long.MAX_VALUE);
    }

    /**
     * Execution of the SE-MHSE algorithm
     * @return Computed metrics of the algorithm
     */
    public Measure runAlgorithm() {
        long startTime = System.currentTimeMillis();
        long totalTime;
        long lastLogTime = startTime;
        long logTime;

        Int2ObjectOpenHashMap<int[]> collisionsTable = new Int2ObjectOpenHashMap<>();       //for each hop a list of collisions for each hash function
        Int2DoubleLinkedOpenHashMap hopTable;
        int[] lastHops = new int[mNumSeeds];                           //for each hash function, the last hop executed

        NodeIterator nodeIter;

        for (int s = 0; s < mNumSeeds; s++) {
            int h = 0;
            int collisions = 0;
            boolean signatureIsChanged = true;
            hashes = new Int2LongOpenHashMap(mGraph.numNodes());
            int [] nodes = mGraph.get_nodes();
            int i ;
            i= 0;
            while (signatureIsChanged) {
                int[] hopCollisions;
                if (collisionsTable.containsKey(h)) {
                    hopCollisions = collisionsTable.get(h);
                } else {
                    hopCollisions = new int[mNumSeeds];
                }

                //first h - initialization

                if (h == 0) {
                    initializeGraph(s);
                    //collisions computation

                    i = 0;
                    while (i<mGraph.numNodes()) {
                        int node = nodes[i];
                        if (hashes.get(node) == graphSignature[s]) {
                            collisions++;
                        }
                        i+=1;
                    }
                } else {   //next hops
                    signatureIsChanged = false;
                    // copy all the actual hashes in a new structure
                    oldHashes = new Int2LongOpenHashMap(mGraph.numNodes());
                    i = 0;
                    while (i<mGraph.numNodes()) {
                        int node = nodes[i];
                        oldHashes.put(node, hashes.get(node));
                        i+=1;
                    }

                    //collisions for this hash function, until this h
                    //s.e. number of nodes updated
                    collisions = 0;

                    //update of the hash values
                    i = 0;
                    while (i<mGraph.numNodes()) {
                        int node = nodes[i];
                        i+=1;
                        if (updateNodeHashValue(node)) {
                            signatureIsChanged = true;
                        }
                        //check if there is a collision between graph minhash and actual node hashValue
                        if (hashes.get(node) == graphSignature[s]) {
                            collisions++;
                        }

                        logTime = System.currentTimeMillis();
                        if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                            int maxHop = Arrays.stream(lastHops).summaryStatistics().getMax() + 1;
                            logger.info("(seed # {}) # nodes analyzed {} / {} for h {} / {} (upper bound), estimated time remaining {}",
                                    s + 1,
                                    node, mGraph.numNodes(),
                                    h + 1, maxHop,
                                    String.format("%d min, %d sec",
                                            TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - startTime)) / (s + 1)) - (logTime - startTime)),
                                            TimeUnit.MILLISECONDS.toSeconds(((mNumSeeds * (logTime - startTime)) / (s + 1)) - (logTime - startTime)) -
                                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(((mNumSeeds * (logTime - startTime)) / (s + 1)) - (logTime - startTime)))));
                            lastLogTime = logTime;
                        }
                    }
                }

                if (signatureIsChanged) {
                    hopCollisions[s] = collisions;
                    collisionsTable.put(h, hopCollisions);
                    lastHops[s] = h;
                    h++;
                }
            }
        }

        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        //normalize collisionsTable
        normalizeCollisionsTable(collisionsTable);

        hopTable = hopTable(collisionsTable);

        GraphMeasure graphMeasure = new GraphMeasure();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        graphMeasure.setCollisionsTable(collisionsTable);
        graphMeasure.setLastHops(lastHops);
        graphMeasure.setSeedsList(mSeeds);
        graphMeasure.setHopTable(hopTable);
        graphMeasure.setLowerBoundDiameter(hopTable.size() - 1);
        graphMeasure.setThreshold(mThreshold);
        graphMeasure.setNumSeeds(mNumSeeds);
        graphMeasure.setTime(totalTime);
        graphMeasure.setMinHashNodeIDs(mMinHashNodeIDs);
        graphMeasure.setAvgDistance(Stats.averageDistance(hopTable));
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(hopTable, mThreshold));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(hopTable));
        graphMeasure.setTotalCouplesPercentage(Stats.totalCouplesPercentage(hopTable, mThreshold));

        return graphMeasure;
    }



    /**
     * Initialization of the graph structures
     * Creates hash values for all graph nodes
     * and store minhash in graphSignature according to seedIndex
     * @param seedIndex
     */
    private void initializeGraph(int seedIndex){
        int [] nodes = mGraph.get_nodes();
        int j;
        j= 0;
        int seed = mSeeds.getInt(seedIndex);
        while(j<mGraph.numNodes()) {
            int node = nodes[j];
            long hashValue = CreateSeeds.hashFunction(node, seed);
            hashes.put(node,hashValue);
            if(hashValue < graphSignature[seedIndex]){
                graphSignature[seedIndex] = hashValue;
                mMinHashNodeIDs[seedIndex] = node;
            }
            j+=1;
        }
    }

    /**
     * Calculates the new signature for a node, based on the signature of the node's neighbours
     * @param node
     * @return true if the new signature is different from the previous one
     */
    public boolean updateNodeHashValue(int node) {
        boolean hashValueIsChanged = false;
        long newHashValue = hashes.get(node);         //new signature to be updated

        int [] neigh = mGraph.get_neighbours(node);
        int k;
        int d = neigh.length;
        long neighbourHashValue;
        int neighbour;
        k = 0;

        while(d-- != 0) {
            neighbour =neigh[k];
            k+=1;
            neighbourHashValue = oldHashes.get(neighbour);
            if(neighbourHashValue < newHashValue){
                newHashValue = neighbourHashValue;
                hashValueIsChanged = true;
            }
        }
        hashes.put(node,newHashValue);
        return hashValueIsChanged;
    }

    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */

    private Int2DoubleLinkedOpenHashMap hopTable(Int2ObjectOpenHashMap<int[]> collisionsTable) {
        Int2DoubleLinkedOpenHashMap hopTable = new Int2DoubleLinkedOpenHashMap();
        int lastHop = collisionsTable.size() - 1;
        long sumCollisions = 0;

        for(int hop = 0; hop <= lastHop; hop++){
            int[] collisions = collisionsTable.get(hop);
            sumCollisions = Arrays.stream(collisions).sum();
            double couples = (double) (sumCollisions * mGraph.numNodes()) / this.mNumSeeds;
            hopTable.put(hop, couples);
        }
        return hopTable;
    }


    /***
     * TODO Optimizable?
     * Normalization of the collisionsTable.
     * For each hop check if one of the hash functions reached the end of computation.
     * If so, we have to substitute the 0 value in the table with
     * the maximum value of the other hash functions of the same hop
     */
    private void normalizeCollisionsTable(Int2ObjectOpenHashMap<int[]> collisionsTable) {
        int lowerBoundDiameter = collisionsTable.size() - 1;

        //Start with hop 1
        //There is no check for hop 0 because at hop 0 there is always (at least) 1 collision, never 0.
        for (int i = 1; i <= lowerBoundDiameter; i++) {
            int[] previousHopCollisions = collisionsTable.get(i - 1);
            int[] hopCollisions = collisionsTable.get(i);
            //TODO first if is better for performance?
            if (Arrays.stream(hopCollisions).anyMatch(coll -> coll == 0)) {
                for (int j = 0; j < hopCollisions.length; j++) {
                    if(hopCollisions[j] == 0){
                        hopCollisions[j] = previousHopCollisions[j];
                    }
                }
            }
            collisionsTable.put(i, hopCollisions);
        }
    }

}


