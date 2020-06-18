package it.bigdatalab.algorithm;

import it.bigdatalab.model.GraphMeasure;
import it.unimi.dsi.fastutil.ints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class StandaloneBMinHash extends MinHash {

    private Int2LongSortedMap mTotalCollisions;

    private static final int MASK = 6; // 2^6
    private static final int REMAINDER = 58;
    private static final long BIT = 1;

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.StandaloneBMinHash");


    /**
     * Creates a new BooleanMinHash instance with default values
     */
    public StandaloneBMinHash() throws DirectionNotSetException, SeedsException, IOException {
        super();
        mTotalCollisions = new Int2LongLinkedOpenHashMap();

        logger.info("# nodes {}, # edges {}", mGraph.numNodes(), mGraph.numArcs());
    }


    public GraphMeasure runAlgorithm() {
        int lowerBoundDiameter = 0;
        int previousLowerBoundDiameter;
        Int2LongSortedMap totalCollisionForHash = new Int2LongLinkedOpenHashMap();

        for (int i = 0; i < this.numSeeds; i++) {

            logger.info("Starting computation on seed {}", i);

            Int2LongSortedMap hopCollision;
            int counter;

            // Set false as signature of all graph nodes
            // used to computing the algorithm
            long[] mutable = new long[lengthBitsArray(mGraph.numNodes())];
            long[] immutable = new long[lengthBitsArray(mGraph.numNodes())];

            // Choose a random node is equivalent to compute the minhash
            int randomNode = ThreadLocalRandom.current().nextInt(0, mGraph.numNodes());
//            int randomNode = 0;

            // take a long number, if we divide it to power of 2, quotient is in the first 6 bit, remainder
            // in the last 58 bit. So, move the remainder to the left, and then to the right to delete the quotient.
            // This is equal to logical and operation.
            int remainderPositionRandomNode = ((randomNode << REMAINDER) >>> REMAINDER);
            // quotient is randomNode >>> MASK
            mutable[randomNode >>> MASK] |= (BIT) << remainderPositionRandomNode;

            int h = 0;

            // initialization of the collision counter for the hop
            // we use a dict because we want to iterate over the nodes until
            // the number of collisions in the actual hop
            // is different than the previous hop
            hopCollision = new Int2LongLinkedOpenHashMap();
            hopCollision.put(h, 1);

            while(hopCollision.getOrDefault(h, 0) != hopCollision.getOrDefault(h-1, 0)) {
                h += 1;
                logger.debug("(seed {}) Starting computation on hop {}", i, h);
                mTotalCollisions.put(h-1, (mTotalCollisions.getOrDefault(h-1, 0) + hopCollision.getOrDefault(h-1, 0)));
                logger.info("# seed {} # hop: {} \n total collisions table {}", i, (h-1), mTotalCollisions);

                // copy all the actual nodes hash in a new structure
                System.arraycopy(mutable, 0, immutable, 0, mutable.length);

                hopCollision.put(h, 0);

                long remainderPositionNode;
                int quotientNode;
                for (int n = 0; n < mGraph.numNodes(); n++) {

                    final int node = n;

                    final int d = mGraph.outdegree(node);
                    final int[] successors = mGraph.successorArray(node);

                    // update the node hash iterating over all its neighbors
                    // and computing the OR between the node signature and
                    // the neighbor signature.
                    // store the new signature as the current one
                    remainderPositionNode = (node << REMAINDER) >>> REMAINDER;
                    quotientNode = node >>> MASK;

                    long value = immutable[quotientNode];
                    long bitNeigh;
                    long nodeMask = (1L << remainderPositionNode);

                    if (((nodeMask & value) >>> remainderPositionNode) == 0) { // check if node bit is 0
                        for (int l = 0; l < d; l++) {
                            final int neighbour = successors[l];


                            int quotientNeigh = neighbour >>> MASK;
                            long remainderPositionNeigh = (neighbour << REMAINDER) >>> REMAINDER;

                            bitNeigh = (((1L << remainderPositionNeigh) & immutable[quotientNeigh]) >>> remainderPositionNeigh) << remainderPositionNode;
                            value = bitNeigh | nodeMask & immutable[quotientNode];
                            if ((value >>> remainderPositionNode) == 1) {
                                break;
                            }
                        }
                    }
                    mutable[quotientNode] = mutable[quotientNode] | value;
                }

                // count the collision between the node signature and the graph signature
                counter = 0;
                for (int c = 0; c < mutable.length; c++) {
                    counter += Long.bitCount(mutable[c]);
                }
                hopCollision.put(h, counter);
                logger.debug("(seed {}) Hop Collision {}", i, hopCollision);
            }

            // compute the collision table
            totalCollisionForHash.put(i, hopCollision.get(h));
            if((h-1) > lowerBoundDiameter) {
                previousLowerBoundDiameter = lowerBoundDiameter;
                lowerBoundDiameter = h-1;
                // new lower bound diameter founded, normalize all the total collision hash computed
                for(int j = 0; j<i; j++) {
                    // add the missing number of collisions from the previous lower bound diameter to the new founded
                    for(int k = previousLowerBoundDiameter+1; k < lowerBoundDiameter+1; k++) {
                        mTotalCollisions.put(k, totalCollisionForHash.getOrDefault(j, 0));
                    }
                }
            } else if((h-1) < lowerBoundDiameter) {
                for(int k = h; k < lowerBoundDiameter+1; k++) {
                    mTotalCollisions.put(k, hopCollision.getOrDefault(h, 0));
                }
            }
            logger.debug("(seed {}) Collision table {}", i, mTotalCollisions);
            logger.debug("Ended computation on seed {}", i);
        }
        hopTable = hopTable();
        GraphMeasure graphMeasure = new GraphMeasure(hopTable);
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setNumArcs(mGraph.numArcs());
        return graphMeasure;
    }

    private int lengthBitsArray(int numberOfNodes) {
        return (int) Math.ceil(numberOfNodes / (double) Long.SIZE);
    }


    /***
     * Compute the hop table for reachable pairs within h hops [(CountAllCum[h]*n) / s]
     * @return hop table
     */

    private Int2DoubleSortedMap hopTable() {
        Int2DoubleSortedMap hopTable = new Int2DoubleLinkedOpenHashMap();
        mTotalCollisions.forEach((key, value) -> {
            Double r = ((double) (value * mGraph.numNodes()) / this.numSeeds);
            hopTable.put(key, r);
        });
        return hopTable;
    }
}
