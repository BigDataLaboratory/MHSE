package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class Measure {

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
    protected int[] mMinHashNodeIDs;
    @SerializedName("seed_list")
    protected IntArrayList mSeedsList;
    @SerializedName("num_seed")
    protected int mNumSeeds;
    @SerializedName("nodes")
    protected int mNumNodes;
    @SerializedName("edges")
    protected long mNumArcs;
    @SerializedName("direction")
    protected String mDirection;
    @SerializedName("seeds_time")
    protected double[] mSeedsTime;
    @SerializedName("last_hops")
    protected int[] mLastHops;
    @SerializedName("run")
    protected int mRun;

    public Measure(double threshold) {
        this.mThreshold = threshold;
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
     * @return graph's number of nodes
     */
    public int getNumNodes() {
        return mNumNodes;
    }

    /**
     * @param numNodes graph's number of nodes
     */
    public void setNumNodes(int numNodes) {
        this.mNumNodes = numNodes;
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
     * @return graph's number of edges
     */
    public long getNumArcs() {
        return mNumArcs;
    }

    /**
     * @return execution time
     */
    public long getTime() {
        return mTime;
    }

    /**
     * @return algorithm's name
     */
    public String getAlgorithmName() {
        return mAlgorithmName;
    }

    /**
     * @param numArcs graph's number of edges
     */
    public void setNumArcs(long numArcs) {
        this.mNumArcs = numArcs;
    }

    /**
     * @return list of seeds
     */
    public IntArrayList getSeedsList() {
        return mSeedsList;
    }

    /**
     * @return threshold
     */
    public double getThreshold() {
        return mThreshold;
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
     * @param threshold for diameter computing
     */
    public void setThreshold(double threshold) {
        this.mThreshold = threshold;
    }

    /**
     * @return Comma separated IDs of minHash nodes
     */
    public int[] getMinHashNodeIDs() {
        return mMinHashNodeIDs;
    }

    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/

    /**
     * @param minHashNodeIDs Comma separated IDs of minHash nodes
     */
    public void setMinHashNodeIDs(int[] minHashNodeIDs) {
        this.mMinHashNodeIDs = minHashNodeIDs;
    }

    /**
     * @return numbers of seeds to use
     */
    public int getNumSeeds() {
        return mNumSeeds;
    }

    /**
     * @param numSeeds Number of seeds
     */
    public void setNumSeeds(int numSeeds) {
        this.mNumSeeds = numSeeds;
    }

    /**
     * @return Array containing the last hop executed for each hash function
     */
    public int[] getLastHops() {
        return mLastHops;
    }

    /**
     * @return Array containing the last hop executed for each hash function
     */
    public void setLastHops(int[] lastHops) {
        this.mLastHops = lastHops;
    }

    /**
     * @return number of run
     */
    public int getRun() {
        return mRun;
    }

    /**
     * @param run number
     */
    public void setRun(int run) {
        this.mRun = run;
    }

    /**
     * @param seedsTime time for each seed
     */
    public void setSeedsTime(double[] seedsTime) {
        this.mSeedsTime = seedsTime;
    }

    /**
     * @param maxMemoryUsed max memory used by application
     */
    public void setMaxMemoryUsed(long maxMemoryUsed) {
        this.mMaxMemoryUsed = maxMemoryUsed;
    }

    /**
     * @param algorithmName algorithm's name
     */
    public void setAlgorithmName(String algorithmName) {
        this.mAlgorithmName = algorithmName;
    }

    /**
     * @param time execution time
     */
    public void setTime(long time) {
        this.mTime = time;
    }

    /**
     * @param seedsList Comma separated seeds
     */
    public void setSeedsList(IntArrayList seedsList) {
        this.mSeedsList = seedsList;
    }

    /**
     * @param direction of minhashing
     */
    public void setDirection(String direction) {
        this.mDirection = direction;
    }


}