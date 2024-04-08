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
    @SerializedName("closeness_centrality_apx")
    private double[] mClosenessCentrality;
    @SerializedName("harmonic_centrality_apx")
    private float[] mHarmonicCentrality;
    //@SerializedName("harmonic_centrality_apx_topk")
    //private Hasm[] mHarmonicCentralityTopK;
    @SerializedName("linn_centrality_apx")
    private double[] mLinnCentrality;
    @SerializedName("t_ball_size")
    private int TBall;
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

    public double[] getClosenessCentrality() {
        return mClosenessCentrality;
    }
    public float[] getHarmonicCentrality() {
        return mHarmonicCentrality;
    }
    public double[] getLinnCentrality() {
        return mLinnCentrality;
    }


    public void setTBall(int TBall) {this.TBall = TBall;}
    public int getTBall() {return this.TBall;}


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

    public void setClosenessCentrality(double[] closenessCentrality) {
        this.mClosenessCentrality = closenessCentrality;
    }

    public void setHarmonicCentrality(float[] HarmonicCentrality) {
        this.mHarmonicCentrality = HarmonicCentrality;
    }
   // public void setHarmonicCentralityTopK(float[] HarmonicCentralityTopK) {
      //  this.mHarmonicCentrality = HarmonicCentrality;
   // }

    public void setLinnCentrality(double[] LinnCentrality) {
        this.mLinnCentrality = LinnCentrality;
    }


}
