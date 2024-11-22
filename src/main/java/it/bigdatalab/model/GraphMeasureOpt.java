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
    @SerializedName("harmonic_centrality")
    private double[] mHarmonicCentrality;
    @SerializedName("harmonic_centrality_unnorm")
    private double[] mHarmonicCentralityUnnorm;
    @SerializedName("linn_centrality_apx")
    private double[] mLinnCentrality;
    @SerializedName("t_ball_size")
    private float TBall;
    private double AvgBallSize;
    private double StdBallSize;
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
    public double[] getHarmonicCentrality() {
        return mHarmonicCentrality;
    }
    public double[] getLinnCentrality() {
        return mLinnCentrality;
    }


    public void setTBall(float TBall) {this.TBall = TBall;}
    public float getTBall() {return this.TBall;}

    public void setAvgBallSize(double avgSize){this.AvgBallSize = avgSize;}
    public double getAvgBallSize(){return this.AvgBallSize;}
    public void setStdBallSize(double stdSize){this.StdBallSize = stdSize;}
    public double getStdBallSize(){return this.StdBallSize;}

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

    public void setHarmonicCentrality(double[] HarmonicCentrality) {
        this.mHarmonicCentrality = HarmonicCentrality;
    }
    public void setHarmonicCentralityUnnorm(double [] HarmonicCentralityUnnorm){
        this.mHarmonicCentralityUnnorm = HarmonicCentralityUnnorm;
    }
   // public void setHarmonicCentralityTopK(float[] HarmonicCentralityTopK) {
      //  this.mHarmonicCentrality = HarmonicCentrality;
   // }

    public void setLinnCentrality(double[] LinnCentrality) {
        this.mLinnCentrality = LinnCentrality;
    }


}
