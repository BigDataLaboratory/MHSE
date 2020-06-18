package it.bigdatalab.model;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;
import it.bigdatalab.utils.PropertiesManager;

public class GraphMeasure {
    private int mLowerBoundDiameter;
    private double mAvgDistance;
    private double mEffectiveDiameter;
    private double mTotalCouples;
    private double mTotalCouplePercentage;
    private long mTime; //time elapsed in milliseconds
    private Int2DoubleSortedMap mHopTable;
    private String mAlgorithmName;
    private float mThreshold;
    private String minHashNodeIDs;
    private String mSeedsList;
    private int numSeeds;
    private int numNodes;
    private long numArcs;
    private String mDirection;


    public GraphMeasure(Int2DoubleSortedMap hopTable){
        this.mHopTable = hopTable;
        this.mTime = -1;
        this.mAlgorithmName = "";
        this.numNodes = -1;
        this.numArcs = -1;
        this.minHashNodeIDs = "";
        this.mThreshold = Float.parseFloat(PropertiesManager.getProperty("minhash.threshold"));
        this.mLowerBoundDiameter = hopTable.size()-1;
        this.mAvgDistance = averageDistance();
        this.mEffectiveDiameter = effectiveDiameter();
        this.mTotalCouples = totalCouplesReachable();
        this.mTotalCouplePercentage = totalCouplesPercentage();
        this.mSeedsList = PropertiesManager.getProperty("minhash.seeds");
        this.numSeeds = mSeedsList.split(",").length;
        this.mDirection = PropertiesManager.getProperty("minhash.direction");

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
     * that is the maximum distance that allows to connect the of all reachable pairs
     */
    private double effectiveDiameter() {
        if(mHopTable.size() == 0) {
            return 0;
        }

        int lowerBoundDiameter = mHopTable.size()-1;
        double numCollisions = mHopTable.get(lowerBoundDiameter);

        int d = 1;
        while((mHopTable.get(d)/numCollisions) < mThreshold) {
            d += 1;
        }

        return ((d-1) + interpolate(mHopTable.get(d-1), mHopTable.get(d), mThreshold * numCollisions));
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
        for(Int2DoubleMap.Entry entry : mHopTable.int2DoubleEntrySet()) {
            int key = entry.getIntKey();
            double value = entry.getDoubleValue();

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
    public float getThreshold() {
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

    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/

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
     * @param minHashNodeIDs Comma separated IDs of minHash nodes
     */
    public void setMinHashNodeIDs(String minHashNodeIDs) {
        this.minHashNodeIDs = minHashNodeIDs;
    }

}