package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphMeasureOpt extends Measure {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.model.GraphMeasureOpt");

    @SerializedName("hop_table")
    private double[] mHopTable;
    @SerializedName("collision_table")
    private int[][] mCollisionsMatrix;

    /**
     * @param hopTable           as an array
     * @param lowerBoundDiameter computed when algorithm completed
     */
    public GraphMeasureOpt(double[] hopTable, int lowerBoundDiameter, double threshold) {
        super(threshold);
        this.mHopTable = hopTable;
        this.mLowerBoundDiameter = lowerBoundDiameter;
        this.mAvgDistance = averageDistance();
        this.mEffectiveDiameter = effectiveDiameter();
        this.mTotalCouples = totalCouplesReachable();
        this.mTotalCouplePercentage = totalCouplesPercentage();
    }

    /**
     * @return total number of reachable pairs (last hop)
     */
    private double totalCouplesReachable() {
        return mHopTable[mLowerBoundDiameter];
    }

    /**
     * @return percentage of number of reachable pairs (last hop)
     */
    private double totalCouplesPercentage() {
        return mHopTable[mLowerBoundDiameter] * mThreshold;
    }

    /**
     * @return effective diameter of the graph (computed using hop table), defined as the 90th percentile distance between nodes,
     * that is the minimum distance that allows to connect the 90th percent of all reachable pairs
     */
    private double effectiveDiameter() {
        if (mHopTable.length == 0) {
            return 0;
        }

        int lowerBoundDiameter = mHopTable.length - 1;
        double totalCouplesReachable = mHopTable[lowerBoundDiameter];

        int d = 0;

        while (mHopTable[d] / totalCouplesReachable < mThreshold) {
            d += 1;
        }

        return (d != 0) ? (d - 1) + interpolate((mHopTable[d - 1]), (mHopTable[d]), mThreshold * totalCouplesReachable) : 0;
    }

    /**
     * Compute the average distance using the hop table
     *
     * @return average distance for the graph
     */
    private double averageDistance() {
        int lowerBoundDiameter;
        double sumAvg = 0;

        if (mHopTable.length == 0)
            return 0;

        lowerBoundDiameter = mHopTable.length - 1;
        for (int i = 0; i < mHopTable.length; i++) {
            if (i == 0 || i == 1) {
                sumAvg += 0;
            } else {
                sumAvg += (i * (mHopTable[i] - mHopTable[i - 1]));
            }
        }
        return (sumAvg / mHopTable[lowerBoundDiameter]);
    }


    /*******************************************************************************
     *                                  GETTER METHODS
     * ****************************************************************************/


    /**
     * @return hop table
     */
    public double[] getHopTable() {
        return mHopTable;
    }

    /**
     * @param hopTable
     */
    public void setHopTable(double[] hopTable) {
        this.mHopTable = hopTable;
    }


    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/

    /**
     * @return Collisions matrix for each hash function and hop
     */
    public int[][] getCollisionsMatrix() {
        return mCollisionsMatrix;
    }

    /**
     * @param collisionsMatrix Matrix of the collisions for each hash function and for each hop
     */
    public void setCollisionsMatrix(int[][] collisionsMatrix) {
        this.mCollisionsMatrix = collisionsMatrix;
    }

    /**
     * @param collisionsTable The map of the collisions for each hop and for each hash function
     */
    public void setCollisionsTable(int[][] collisionsTable) {
        this.mCollisionsMatrix = collisionsTable;
    }

}
