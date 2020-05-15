package it.uniroma2.model;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;
import it.uniroma2.utils.PropertiesManager;

public class GraphMeasure {
    private int mLowerBoundDiameter;
    private double mAvgDistance;
    private double mEffectiveDiameter;
    private double mTotalCouples;
    private double mTotalCouplePercentage;
    private long mTime;             //time elapsed in milliseconds
    private Int2DoubleSortedMap mHopTable;
    private String mAlgorithmName;
    private float mThreshold;
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

    public GraphMeasure() {

    }

    private double totalCouplesReachable() {
        return mHopTable.get(mLowerBoundDiameter);
    }

    private double totalCouplesPercentage() {
        return mHopTable.get(mLowerBoundDiameter)*mThreshold;
    }

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


    /* GETTERS */
    public int getNumNodes() {
        return numNodes;
    }

    public long getNumArcs() {
        return numArcs;
    }

    public double getAvgDistance() {
        return mAvgDistance;
    }

    public int getLowerBoundDiameter() {
        return mLowerBoundDiameter;
    }

    public double getEffectiveDiameter() {
        return mEffectiveDiameter;
    }

    public double getTotalCouples() {
        return mTotalCouples;
    }

    public double getTotalCouplePercentage() {
        return mTotalCouplePercentage;
    }

    public float getThreshold() {
        return mThreshold;
    }

    public long getTime() {
        return mTime;
    }

    public String getAlgorithmName() {
        return mAlgorithmName;
    }

    public String getSeedsList() {
        return mSeedsList;
    }

    public int getNumSeeds() {
        return numSeeds;
    }

    public String getDirection() {
        return mDirection;
    }

    public Int2DoubleSortedMap getHopTable() {
        return mHopTable;
    }

    /* SETTERS */
    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    public void setNumArcs(long numArcs) {
        this.numArcs = numArcs;
    }

    public void setTime(long time) {
        this.mTime = time;
    }

    public void setAlgorithmName(String algorithmName) {
        this.mAlgorithmName = algorithmName;
    }

}