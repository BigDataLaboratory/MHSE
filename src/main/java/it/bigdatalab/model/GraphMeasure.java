package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.HashMap;

public class GraphMeasure {

    @SerializedName("memory")
    private long mMaxMemoryUsed;
    @SerializedName("lower_bound")
    private int mLowerBoundDiameter;
    @SerializedName("avg_distance")
    private double mAvgDistance;
    @SerializedName("effective_diameter")
    private double mEffectiveDiameter;
    @SerializedName("total_couples")
    private double mTotalCouples;
    @SerializedName("total_couples_perc")
    private double mTotalCouplePercentage;
    @SerializedName("time")
    private long mTime; //time elapsed in milliseconds
    @SerializedName("hop_table")
    private Int2DoubleSortedMap mHopTable;
    @SerializedName("hop_table")
    private double[] mHopTableArray;
    @SerializedName("collision_table")
    private Int2ObjectOpenHashMap<int[]> collisionsTable;       //for each hop a list of collisions for each hash function
    @SerializedName("collision_table")
    private int[][] mCollisionsMatrix;
    @SerializedName("algorithm")
    private String mAlgorithmName;
    @SerializedName("threshold")
    private double mThreshold;
    @SerializedName("minhash_node_ids")
    private String minHashNodeIDs;
    @SerializedName("seed_list")
    private String mSeedsList;
    @SerializedName("num_seed")
    private int numSeeds;
    @SerializedName("num_nodes")
    private int numNodes;
    @SerializedName("num_edges")
    private long numArcs;
    @SerializedName("direction")
    private String mDirection;
    @SerializedName("seeds_time")
    private HashMap<Integer, Double> seedsTime;
    @SerializedName("last_hops")
    private int[] lastHops;

    /**
     * @param hopTable as a map
     */
    public GraphMeasure(Int2DoubleSortedMap hopTable){
        this.mHopTable = hopTable;
        this.mMaxMemoryUsed = -1;
        this.mTime = -1;
        this.mAlgorithmName = "";
        this.numNodes = -1;
        this.numArcs = -1;
        this.numSeeds = -1;
        this.mSeedsList = "";
        this.minHashNodeIDs = "";
        this.seedsTime = new HashMap<>();
        this.mThreshold = Double.parseDouble(PropertiesManager.getProperty("minhash.threshold"));
        this.mLowerBoundDiameter = hopTable.size()-1;
        this.mAvgDistance = averageDistance();
        this.mEffectiveDiameter = effectiveDiameter();
        this.mTotalCouples = totalCouplesReachable();
        this.mTotalCouplePercentage = totalCouplesPercentage();
        this.mSeedsList = PropertiesManager.getProperty("minhash.seeds");
        this.numSeeds = mSeedsList.split(",").length;
        this.mDirection = PropertiesManager.getProperty("minhash.direction");
        this.lastHops = null;
        this.collisionsTable = null;
    }

    /**
     * @param hopTable           as an array
     * @param lowerBoundDiameter computed when algorithm completed
     */
    public GraphMeasure(double[] hopTable, int lowerBoundDiameter) {
        this.mHopTable = null;
        this.mHopTableArray = hopTable;
        this.mMaxMemoryUsed = -1;
        this.mTime = -1;
        this.mAlgorithmName = "";
        this.numNodes = -1;
        this.numArcs = -1;
        this.numSeeds = -1;
        this.mSeedsList = "";
        this.minHashNodeIDs = "";
        this.seedsTime = new HashMap<>();
        this.mThreshold = Double.parseDouble(PropertiesManager.getProperty("minhash.threshold"));
        this.mLowerBoundDiameter = lowerBoundDiameter;
        this.mAvgDistance = averageDistance();
        this.mEffectiveDiameter = effectiveDiameter();
        this.mTotalCouples = totalCouplesReachable();
        this.mTotalCouplePercentage = totalCouplesPercentage();
        this.mSeedsList = PropertiesManager.getProperty("minhash.seeds");
        this.numSeeds = mSeedsList.split(",").length;
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
        return (mHopTable != null) ? mHopTable.get(mLowerBoundDiameter) : mHopTableArray[mLowerBoundDiameter];
    }

    /**
     * @return percentage of number of reachable pairs (last hop)
     */
    private double totalCouplesPercentage() {
        if (mHopTable != null)
            return mHopTable.get(mLowerBoundDiameter) * mThreshold;
        else
            return mHopTableArray[mLowerBoundDiameter] * mThreshold;
    }

    /**
     * @return effective diameter of the graph (computed using hop table), defined as the 90th percentile distance between nodes,
     * that is the minimum distance that allows to connect the 90th percent of all reachable pairs
     */
    private double effectiveDiameter() {
        if (mHopTable != null ? mHopTable.size() == 0 : mHopTableArray.length == 0) {
            return 0;
        }

        int lowerBoundDiameter = mHopTable != null ? mHopTable.size() - 1 : mHopTableArray.length - 1;
        double totalCouplesReachable = mHopTable != null ? mHopTable.get(lowerBoundDiameter) : mHopTableArray[lowerBoundDiameter];

        int d = 0;
        if (mHopTable != null) {
            while ((mHopTable.get(d) / totalCouplesReachable) < mThreshold) {
                d += 1;
            }
        } else {
            while (mHopTableArray[d] / totalCouplesReachable < mThreshold) {
                d += 1;
            }
        }

        return (d != 0) ? (d - 1) + interpolate((mHopTable != null ? mHopTable.get(d - 1) : mHopTableArray[d - 1]), (mHopTable != null ? mHopTable.get(d) : mHopTableArray[d]), mThreshold * totalCouplesReachable) : 0;
    }

    /**
     * Compute the average distance using the hop table
     * @return average distance for the graph
     */
    private double averageDistance() {
        int lowerBoundDiameter;
        double sumAvg = 0;
        // case map
        if (mHopTable != null) {
            if (mHopTable.size() == 0) {
                return 0;
            }
            lowerBoundDiameter = mHopTable.size() - 1;

            for (Int2DoubleMap.Entry entry : mHopTable.int2DoubleEntrySet()) {
                int key = entry.getIntKey();
                double value = entry.getDoubleValue();

                if (key != 0) {
                    sumAvg += (key * (value - mHopTable.get(key - 1)));
                } else {
                    sumAvg += 0;
                }
            }
            return (sumAvg / mHopTable.get(lowerBoundDiameter));
        } else {
            // case array
            if (mHopTableArray.length == 0)
                return 0;

            lowerBoundDiameter = mHopTableArray.length - 1;
            for (int i = 0; i < mHopTableArray.length; i++) {
                if (i == 0) {
                    sumAvg += 0;
                } else {
                    sumAvg += (i * (mHopTableArray[i] - mHopTableArray[i - 1]));
                }
            }
            return (sumAvg / mHopTableArray[lowerBoundDiameter]);
        }
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
     * @return algorithm's name
     */
    public String getAlgorithmName() {
        return mAlgorithmName;
    }

    /**
     * @return Comma separated IDs of minHash nodes
     */
    public String getMinHashNodeIDs() {
        return minHashNodeIDs;
    }

    /**
     * @return list of seeds
     */
    public String getSeedsList() {
        return mSeedsList;
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
     * @return hop table
     */
    public Int2DoubleSortedMap getHopTable() {
        return mHopTable;
    }

    /**
     * @return hop table
     */
    public double[] getHopTableArray() {
        return mHopTableArray;
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

    /**
     * @return Collisions matrix for each hash function and hop
     */
    public int[][] getCollisionsMatrix() {
        return mCollisionsMatrix;
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
     * @param algorithmName algorithm's name
     */
    public void setAlgorithmName(String algorithmName) {
        this.mAlgorithmName = algorithmName;
    }

    /**
     * @param seedsList Comma separated seeds
     */
    public void setSeedsList(String seedsList) {
        this.mSeedsList = seedsList;
    }

    /**
     * @param minHashNodeIDs Comma separated IDs of minHash nodes
     */
    public void setMinHashNodeIDs(String minHashNodeIDs) {
        this.minHashNodeIDs = minHashNodeIDs;
    }

    /**
     * @param collisionsTable The map of the collisions for each hop and for each hash function
     */
    public void setCollisionsTable(Int2ObjectOpenHashMap<int[]> collisionsTable) {
        this.collisionsTable = collisionsTable;
    }

    /**
     * @param collisionsTable The map of the collisions for each hop and for each hash function
     */
    public void setCollisionsTable(int[][] collisionsTable) {
        this.mCollisionsMatrix = collisionsTable;
    }

    /**
     * @param hopTable
     */
    public void setHopTable(double[] hopTable) {
        this.mHopTableArray = hopTable;
    }


    /**
     * @param lastHops Array containing the last hop for each hash function
     */
    public void setLastHops(int[] lastHops) {
        this.lastHops = lastHops;
    }

    /**
     * @param collisionsMatrix Matrix of the collisions for each hash function and for each hop
     */
    public void setCollisionsMatrix(int[][] collisionsMatrix) {
        this.mCollisionsMatrix = collisionsMatrix;
    }
}