package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;

public class GraphGtMeasure {

    @SerializedName("avg_distance")
    private double mAvgDistance;
    @SerializedName("diameter")
    private double mDiameter;
    @SerializedName("effective_diameter")
    private double mEffectiveDiameter;
    @SerializedName("total_couples")
    private double mTotalCouples;
    @SerializedName("nodes")
    private int mNodes;
    @SerializedName("edges")
    private long mEdges;

    public GraphGtMeasure(int nodes, long edges, double avgDistance, double effectiveDiameter, double diameter, double totalCouples) {
        this.mEdges = edges;
        this.mNodes = nodes;
        this.mAvgDistance = avgDistance;
        this.mDiameter = diameter;
        this.mEffectiveDiameter = effectiveDiameter;
        this.mTotalCouples = totalCouples;
    }

    /*******************************************************************************
     *                                  GETTER METHODS
     * ****************************************************************************/

    /**
     * @return graph's number of nodes
     */
    public int getNumNodes() {
        return mNodes;
    }

    /**
     * @param numNodes graph's number of nodes
     */
    public void setNumNodes(int numNodes) {
        this.mNodes = numNodes;
    }

    /**
     * @return graph's number of edges
     */
    public long getNumArcs() {
        return mEdges;
    }

    /**
     * @param numArcs graph's number of edges
     */
    public void setNumArcs(long numArcs) {
        this.mEdges = numArcs;
    }

    /**
     * @return average distance
     */
    public double getAvgDistance() {
        return mAvgDistance;
    }

    /**
     * @param avgDistance average distance
     */
    public void setAvgDistance(double avgDistance) {
        this.mAvgDistance = avgDistance;
    }


    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/

    /**
     * @return ground truth diameter of a graph
     */
    public double getDiameter() {
        return mDiameter;
    }

    /**
     * @param diameter ground truth diameter of a graph
     */
    public void setDiameter(double diameter) {
        this.mDiameter = diameter;
    }

    /**
     * @return effective diameter of a graph, defined as 90% of the real diamaeter
     */
    public double getEffectiveDiameter() {
        return mEffectiveDiameter;
    }

    /**
     * @param effectiveDiameter effective diameter of a graph, defined as 90% of the real diamaeter
     */
    public void setEffectiveDiameter(double effectiveDiameter) {
        this.mEffectiveDiameter = effectiveDiameter;
    }

    /**
     * @return total couples of reachable nodes
     */
    public double getTotalCouples() {
        return mTotalCouples;
    }

    /**
     * @param totalCouples total couples of reachable nodes
     */
    public void setTotalCouples(double totalCouples) {
        this.mTotalCouples = totalCouples;
    }


}
