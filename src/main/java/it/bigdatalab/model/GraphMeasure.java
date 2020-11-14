package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class GraphMeasure extends Measure {

    @SerializedName("hop_table")
    private Int2DoubleLinkedOpenHashMap mHopTable;
    @SerializedName("collision_table")
    private Int2ObjectOpenHashMap<int[]> collisionsTable;       //for each hop a list of collisions for each hash function

    /**
     * @param hopTable as a map
     */
    public GraphMeasure(Int2DoubleLinkedOpenHashMap hopTable) {
        this.mHopTable = hopTable;
        this.mLowerBoundDiameter = hopTable.size()-1;
        this.mAvgDistance = averageDistance();
        this.mEffectiveDiameter = effectiveDiameter();
        this.mTotalCouples = totalCouplesReachable();
        this.mTotalCouplePercentage = totalCouplesPercentage();
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
        if (mHopTable != null) {
            return 0;
        }

        int lowerBoundDiameter = mHopTable.size() - 1;
        double totalCouplesReachable = mHopTable.get(lowerBoundDiameter);

        int d = 0;
            while ((mHopTable.get(d) / totalCouplesReachable) < mThreshold) {
                d += 1;
            }

        return (d != 0) ? (d - 1) + interpolate((mHopTable.get(d - 1)), (mHopTable.get(d)), mThreshold * totalCouplesReachable) : 0;
    }

    /**
     * Compute the average distance using the hop table
     * @return average distance for the graph
     */
    private double averageDistance() {
        int lowerBoundDiameter;
        double sumAvg = 0;
        // case map
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
    }


    /*******************************************************************************
     *                                  GETTER METHODS
     * ****************************************************************************/

    /**
     * @return hop table
     */
    public Int2DoubleLinkedOpenHashMap getHopTable() {
        return mHopTable;
    }

    /**
     * @return Collisions tables for each hop and hash function
     */
    public Int2ObjectOpenHashMap<int[]> getCollisionsTable() {
        return collisionsTable;
    }



    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/


    /**
     * @param collisionsTable The map of the collisions for each hop and for each hash function
     */
    public void setCollisionsTable(Int2ObjectOpenHashMap<int[]> collisionsTable) {
        this.collisionsTable = collisionsTable;
    }

}