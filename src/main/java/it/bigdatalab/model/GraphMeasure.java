package it.bigdatalab.model;

import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphMeasure extends Measure {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.model.GraphMeasure");

    @SerializedName("hop_table")
    private Int2DoubleLinkedOpenHashMap mHopTable;
    @SerializedName("collision_table")
    private Int2ObjectOpenHashMap<int[]> mCollisionsTable;       //for each hop a list of collisions for each hash function

    /**
     * @param hopTable as a map
     */
    public GraphMeasure(Int2DoubleLinkedOpenHashMap hopTable, double threshold) {
        super(threshold);
        this.mHopTable = hopTable;
        this.mLowerBoundDiameter = hopTable.size()-1;
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
        return mCollisionsTable;
    }



    /*******************************************************************************
     *                                  SETTER METHODS
     * ****************************************************************************/


    /**
     * @param collisionsTable The map of the collisions for each hop and for each hash function
     */
    public void setCollisionsTable(Int2ObjectOpenHashMap<int[]> collisionsTable) {
        this.mCollisionsTable = collisionsTable;
    }

}