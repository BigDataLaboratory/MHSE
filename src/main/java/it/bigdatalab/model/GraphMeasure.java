package it.bigdatalab.model;

import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GraphMeasure {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.model.GraphMeasure");

    private long mMaxMemoryUsed;
    private int mLowerBoundDiameter;
    private double mAvgDistance;
    private double mEffectiveDiameter;
    private double mTotalCouples;
    private double mTotalCouplePercentage;
    private long mTime; //time elapsed in milliseconds
    private long[] mTimePerSeed; // Time elapsed in milliseconds for each seed
    private Int2DoubleLinkedOpenHashMap mHopTable;
    private Int2ObjectOpenHashMap<int[]> collisionsTable;       //for each hop a list of collisions for each hash function
    private String mAlgorithmName;
    private double mThreshold;
    private int[] minHashNodeIDs;
    private IntArrayList mSeedsList;
    private int numSeeds;
    private int numNodes;
    private long numArcs;
    private String mDirection;
    private HashMap<Integer, Double> seedsTime;
    private int[] lastHops;


    public GraphMeasure(Int2DoubleLinkedOpenHashMap hopTable) {
        this.mHopTable = hopTable;
        this.mMaxMemoryUsed = -1;
        this.mTime = -1;
        this.mAlgorithmName = "";
        this.numNodes = -1;
        this.numArcs = -1;
        this.numSeeds = -1;
        this.seedsTime = new HashMap<>();
        this.mThreshold = Double.parseDouble(PropertiesManager.getProperty("minhash.threshold"));
        this.mLowerBoundDiameter = hopTable.size()-1;
        this.mAvgDistance = averageDistance();
        this.mEffectiveDiameter = effectiveDiameter();
        this.mTotalCouples = totalCouplesReachable();
        this.mTotalCouplePercentage = totalCouplesPercentage();
        this.mSeedsList = null;
        this.mDirection = PropertiesManager.getProperty("minhash.direction");
        this.lastHops = null;
        this.collisionsTable = null;

    }

    // empty constructor
    public GraphMeasure() {

    }

    /**
     * @return total number of reachable pairs (last hop)
     */
    private double totalCouplesReachable() {
        return mHopTable.get(mLowerBoundDiameter);
    }

    /**
     * @return percentage of number of reachable pairs (last hop)
     */
    private double totalCouplesPercentage() {
        return mHopTable.get(mLowerBoundDiameter) * mThreshold;
    }

    /**
     * @return effective diameter of the graph (computed using hop table), defined as the 90th percentile distance between nodes,
     * that is the minimum distance that allows to connect the 90th percent of all reachable pairs
     */
    private double effectiveDiameter() {
        if(mHopTable.size() == 0) {
            return 0;
        }

        int lowerBoundDiameter = mHopTable.size()-1;
        double totalCouplesReachable = mHopTable.get(lowerBoundDiameter);

        int d = 0;
        while((mHopTable.get(d)/totalCouplesReachable) < mThreshold) {
            d += 1;
        }

        double result = 0;
        if(d != 0){
            result = (d-1) + interpolate(mHopTable.get(d-1), mHopTable.get(d), mThreshold * totalCouplesReachable);
        }

        return result ;
    }

    /**
     * Compute the average distance using the hop table
     * @return average distance for the graph
     */
    private double averageDistance() {
        if(mHopTable.size() == 0) {
            return 0;
        }
        int lowerBoundDiameter = mHopTable.size()-1;

        double sumAvg = 0;
        for (Map.Entry<Integer, Double> entry : mHopTable.entrySet()) {
            int key = entry.getKey();
            double value = entry.getValue();

            if(key != 0) {
                sumAvg+=(key*(value-mHopTable.get(key-1)));
            } else {
                sumAvg+=0;
            }
        }
        return (sumAvg/mHopTable.get(lowerBoundDiameter));
    }

    private double interpolate(double y0, double y1, double y) {
        // (y1 - y0) is the delta neighbourhood
        return (y - y0) / (y1 - y0);
    }


    /*******************************************************************************
     *                                  GETTER METHODS
     * ****************************************************************************/

    /**
     * @return time in ms for each seed
     */
    public HashMap<Integer, Double> getSeedsTime() {
        return seedsTime;
    }


    /**
     * @return graph's number of nodes
     */
    public int getNumNodes() {
        return numNodes;
    }

    /**
     * @return graph's number of edges
     */
    public long getNumArcs() {
        return numArcs;
    }

    /**
     * @return average distance
     */
    public double getAvgDistance() {
        return mAvgDistance;
    }

    /**
     * @return lower bound diamater of a graph
     */
    public int getLowerBoundDiameter() {
        return mLowerBoundDiameter;
    }

    /**
     * @return effective diameter of a graph, defined as 90% of the real diamaeter
     */
    public double getEffectiveDiameter() {
        return mEffectiveDiameter;
    }

    /**
     * @return total couples of reachable nodes
     */
    public double getTotalCouples() {
        return mTotalCouples;
    }

    /**
     * @return percentage of couples of reachable nodes
     */
    public double getTotalCouplePercentage() {
        return mTotalCouplePercentage;
    }

    /**
     * @return
     */
    public double getThreshold() {
        return mThreshold;
    }

    /**
     * @return execution time
     */
    public long getTime() {
        return mTime;
    }

    /**
     * @return execution time for each seed
     */
    public long [] getTimePerSeed() {
        return mTimePerSeed;
    }

    /**
     * @return algorithm's name
     */
    public String getAlgorithmName() {
        return mAlgorithmName;
    }

    /**
     * @return Comma separated IDs of minHash nodes
     */
    public int[] getMinHashNodeIDs() {
        return minHashNodeIDs;
    }

    /**
     * @param minHashNodeIDs Comma separated IDs of minHash nodes
     */
    public void setMinHashNodeIDs(int[] minHashNodeIDs) {
        this.minHashNodeIDs = minHashNodeIDs;
    }

    /**
     * @return numbers of seeds to use
     */
    public int getNumSeeds() {
        return numSeeds;
    }

    /**
     * @return type of direction to send messages to node's neighbours: out or in
     */
    public String getDirection() {
        return mDirection;
    }

    /**
     * @return list of seeds
     */
    public IntArrayList getSeedsList() {
        return mSeedsList;
    }

    /**
     * @return Collisions tables for each hop and hash function
     */
    public Int2ObjectOpenHashMap<int[]> getCollisionsTable() {
        return collisionsTable;
    }

    /**
     * @return max memory used by application
     */

    public long getMaxMemoryUsed() {
        return mMaxMemoryUsed;
    }

    /**
     * @return Array containing the last hop executed for each hash function
     */
    public int[] getLastHops() {
        return lastHops;
    }


    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/

    /**
     * @param maxMemoryUsed max memory used by application
     */
    public void setMaxMemoryUsed(long maxMemoryUsed) {
        this.mMaxMemoryUsed = maxMemoryUsed;
    }

    /**
     * @param seedsTime time for each seed
     */
    public void setSeedsTime(HashMap seedsTime) {
        this.seedsTime = seedsTime;
    }

    /**
     * @param numNodes graph's number of nodes
     */
    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    /**
     * @param numArcs graph's number of edges
     */
    public void setNumArcs(long numArcs) {
        this.numArcs = numArcs;
    }

    /**
     * @param numSeeds Number of seeds
     */
    public void setNumSeeds(int numSeeds) {
        this.numSeeds = numSeeds;
    }

    /**
     * @param time execution time
     */
    public void setTime(long time) {
        this.mTime = time;
    }

    /**
     * @param time execution time
     */
    public void setTime(long[] time) {
        this.mTimePerSeed = time;
    }

    /**
     * @param algorithmName algorithm's name
     */
    public void setAlgorithmName(String algorithmName) {
        this.mAlgorithmName = algorithmName;
    }

    /**
     * @param seedsList Comma separated seeds
     */
    public void setSeedsList(IntArrayList seedsList) {
        this.mSeedsList = seedsList;
    }

    /**
     * @return hop table
     */
    public Int2DoubleLinkedOpenHashMap getHopTable() {
        return mHopTable;
    }

    /**
     * @param collisionsTable The map of the collisions for each hop and for each hash function
     */
    public void setCollisionsTable(Int2ObjectOpenHashMap<int[]> collisionsTable) {
        this.collisionsTable = collisionsTable;
    }

    /**
     * @param lastHops Array containing the last hop for each hash function
     */
    public void setLastHops(int[] lastHops) {
        this.lastHops = lastHops;
    }

    public void setHopTable(Int2DoubleLinkedOpenHashMap hopTable) {
        this.mHopTable = hopTable;
    }
}