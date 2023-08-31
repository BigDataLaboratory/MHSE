package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphMeasureOpt extends Measure {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.model.GraphMeasureOpt");

    @SerializedName("hop_table")
    private double[] mHopTable;
    @SerializedName("collision_table")
    private int[][] mCollisionsMatrix;
    @SerializedName("hop_for_node")
    private short[][] mHopForNode;

    public GraphMeasureOpt() {

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
     * @param hopForNode Matrix of the hops when minhash encounters each node
     */
    public void setHopForNode(short[][] hopForNode) {
        this.mHopForNode = hopForNode;
    }

    /**
     * @return Matrix of the hops for each node
     */
    public short[][] getHopFornode() {
        return mHopForNode;
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
