package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public abstract class Measure {

    @SerializedName("memory_used")
    protected long mMaxMemoryUsed;
    @SerializedName("lower_bound")
    protected int mLowerBoundDiameter;
    @SerializedName("avg_distance")
    protected double mAvgDistance;
    @SerializedName("effective_diameter")
    protected double mEffectiveDiameter;
    @SerializedName("total_couples")
    protected double mTotalCouples;
    @SerializedName("total_couples_perc")
    protected double mTotalCouplePercentage;
    @SerializedName("time")
    protected long mTime; //time elapsed in milliseconds
    @SerializedName("algorithm")
    protected String mAlgorithmName;
    @SerializedName("threshold")
    protected double mThreshold;
    @SerializedName("node_ids")
    protected int[] minHashNodeIDs;
    @SerializedName("seed_list")
    protected IntArrayList mSeedsList;
    @SerializedName("num_seed")
    protected int numSeeds;
    @SerializedName("nodes")
    protected int numNodes;
    @SerializedName("edges")
    protected long numArcs;
    @SerializedName("direction")
    protected String mDirection;
    @SerializedName("seeds_time")
    protected double[] mSeedsTime;
    @SerializedName("last_hops")
    protected int[] lastHops;


    // empty constructor
    public Measure() {
        this.mMaxMemoryUsed = -1;
        this.mTime = -1;
        this.mAlgorithmName = "";
        this.numNodes = -1;
        this.numArcs = -1;
        this.numSeeds = -1;
        this.mThreshold = Double.parseDouble(PropertiesManager.getProperty("minhash.threshold"));
        this.mDirection = PropertiesManager.getProperty("minhash.direction");
        this.lastHops = null;
    }

    public double interpolate(double y0, double y1, double y) {
        // (y1 - y0) is the delta neighbourhood
        return (y - y0) / (y1 - y0);
    }


    /*******************************************************************************
     *                                  GETTER METHODS
     * ****************************************************************************/

    /**
     * @return time in ms for each seed
     */
    public double[] getSeedsTime() {
        return mSeedsTime;
    }

    /**
     * @param seedsTime time for each seed
     */
    public void setSeedsTime(double[] seedsTime) {
        this.mSeedsTime = seedsTime;
    }

    /**
     * @return graph's number of nodes
     */
    public int getNumNodes() {
        return numNodes;
    }

    /**
     * @param numNodes graph's number of nodes
     */
    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    /**
     * @return graph's number of edges
     */
    public long getNumArcs() {
        return numArcs;
    }

    /**
     * @param numArcs graph's number of edges
     */
    public void setNumArcs(long numArcs) {
        this.numArcs = numArcs;
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
     * @param time execution time
     */
    public void setTime(long time) {
        this.mTime = time;
    }

    /**
     * @return algorithm's name
     */
    public String getAlgorithmName() {
        return mAlgorithmName;
    }

    /**
     * @param algorithmName algorithm's name
     */
    public void setAlgorithmName(String algorithmName) {
        this.mAlgorithmName = algorithmName;
    }

    /**
     * @return Comma separated IDs of minHash nodes
     */
    public int[] getMinHashNodeIDs() {
        return minHashNodeIDs;
    }


    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/

    /**
     * @param minHashNodeIDs Comma separated IDs of minHash nodes
     */
    public void setMinHashNodeIDs(int[] minHashNodeIDs) {
        this.minHashNodeIDs = minHashNodeIDs;
    }

    /**
     * @return list of seeds
     */
    public IntArrayList getSeedsList() {
        return mSeedsList;
    }

    /**
     * @param seedsList Comma separated seeds
     */
    public void setSeedsList(IntArrayList seedsList) {
        this.mSeedsList = seedsList;
    }

    /**
     * @return numbers of seeds to use
     */
    public int getNumSeeds() {
        return numSeeds;
    }

    /**
     * @param numSeeds Number of seeds
     */
    public void setNumSeeds(int numSeeds) {
        this.numSeeds = numSeeds;
    }

    /**
     * @return type of direction to send messages to node's neighbours: out or in
     */
    public String getDirection() {
        return mDirection;
    }

    /**
     * @return max memory used by application
     */
    public long getMaxMemoryUsed() {
        return mMaxMemoryUsed;
    }

    /**
     * @param maxMemoryUsed max memory used by application
     */
    public void setMaxMemoryUsed(long maxMemoryUsed) {
        this.mMaxMemoryUsed = maxMemoryUsed;
    }

    /**
     * @return Array containing the last hop executed for each hash function
     */
    public int[] getLastHops() {
        return lastHops;
    }

    /**
     * @return Array containing the last hop executed for each hash function
     */
    public void setLastHops(int[] lastHops) {
        this.lastHops = lastHops;
    }


}