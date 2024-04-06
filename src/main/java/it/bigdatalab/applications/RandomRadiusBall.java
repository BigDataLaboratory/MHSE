package it.bigdatalab.applications;

import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.Constants;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

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
        long startTime = System.currentTimeMillis();
        long totalTime,logTime,hopStartTime;
        long lastLogTime = startTime;
        int i,n,tau,h;
        n = mGraph.numNodes();
        double[] dist = new double[n];
        float [] centrality = new float[mGraph.numNodes()];
        double r;


        for (i = 0; i <n; i++) {
            r = Math.random();
            tau = (int) Math.floor(1 /  (r / t));
            //BFS of depth tau from i
            Arrays.fill(dist, -1);
            Queue<Integer> ball = new LinkedList<>();
            ball.add(i);
            dist[i] = 0;
            h = 0;
            while (ball.size() != 0 && h < tau) {
                hopStartTime = System.currentTimeMillis();
                int w = ball.remove();
                final int d = mGraph.outdegree(w);
                final int[] successors = mGraph.successorArray(w);
                for (int l = 0; l < d; l++) {
                    if (dist[successors[l]] == -1) {
                        dist[successors[l]] = dist[w] + 1;
                        centrality[successors[l]] += 1;
                        ball.add(successors[l]);
                    }
                }

                logTime = System.currentTimeMillis();
                if (logTime - lastLogTime >= Constants.LOG_INTERVAL) {
                    logger.info("# nodes analyzed {} / {} for hop {} [elapsed {}, node/s {}]",
                            n, mGraph.numNodes(),
                            h,
                            (logTime - hopStartTime) / (double) 1000,
                            ((n + 1) / ((logTime - hopStartTime) / (double) 1000)));
                    lastLogTime = logTime;
                }
                h += 1;

            }
        }

        for (i = 0; i <n; i++) {
            centrality[i] = centrality[i] /t;
        }




        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        return graphMeasure;
    }
}
