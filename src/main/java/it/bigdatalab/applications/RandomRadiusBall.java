package it.bigdatalab.applications;

import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomRadiusBall {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.RandomRadiusBall");
    private final Parameter mParam;
    private final ImmutableGraph mGraph;
    private final int t;
    private final float alpha;
    protected IntArrayList mSeeds;

    private boolean doCentrality;

    private boolean mNormalized;
    // Implementation of the algorithm Random-Radius Ball Method for Estimating Closeness Centrality
    public RandomRadiusBall(Parameter mParam, ImmutableGraph mGraph, int t, float alpha) {
        this.mParam = mParam;
        this.mGraph = mGraph;
        this.t = t;
        this.alpha = alpha;
    }


    public Measure runRBB(){
        float [] centrality = new float[mGraph.numNodes()];
        double r;
        int i,n;
        n = mGraph.numNodes();

        for (i = 0; i <n; i++){
            r = Math.random();

        }



        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        return graphMeasure;
    }
}
