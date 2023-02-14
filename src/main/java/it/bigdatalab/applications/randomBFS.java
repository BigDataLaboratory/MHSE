package it.bigdatalab.applications;

import it.bigdatalab.algorithm.MultithreadBMinHash;
import it.bigdatalab.model.GraphGtMeasure;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.GraphUtils;
import it.bigdatalab.utils.Stats;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.algo.ParallelBreadthFirstVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class randomBFS {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.RandomBFS");
    //private final Parameter mParam;
    private final ImmutableGraph mGraph;
    private final int nSeed;

    public randomBFS(ImmutableGraph g, int seedNumber) {
        this.mGraph = g;
        //this.mParam = param;
        this.nSeed = seedNumber;
    }

    public static void main(String[] args) throws IOException {

        ImmutableGraph g = GraphUtils.loadGraph("/media/antonio/Crucial1TB/PhD/DATASETS/FUB/done/hollywood-2009/hollywood-2009", false, true, true, "out");
        randomBFS rbf = new randomBFS(g,256);
        rbf.run();
    }

    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
    private Measure run(){
        int n = mGraph.numNodes();
        long startTime = System.currentTimeMillis();
        long totalTime;
        ProgressLogger pl = new ProgressLogger();
        double avgDistance = 0.0;
        double dd[] = new double[n];
        double dist[] = new double[n];
        double lower_bound = 0;
        Arrays.fill(dd, 0);
        int seed = 0;
        for (int i = 0; i< nSeed; i++){
            seed = getRandomNumber(0,n);
            Arrays.fill(dist, -1);
            Queue<Integer> ball = new LinkedList<>();
            ball.add(seed);
            dist[seed] = 0;
            while(ball.size() != 0){
                int w = ball.remove();
                final int d = mGraph.outdegree(w);
                final int[] successors = mGraph.successorArray(w);
                for (int l = 0;l<d; l++){
                    if (dist[successors[l]] == -1){
                        dist[successors[l]] = dist[w] +1;
                        dd[(int) dist[successors[l]]] +=1;
                        if (lower_bound < dist[successors[l]]){
                            lower_bound = dist[successors[l]];
                        }
                        ball.add(successors[l]);
                    }
                }
            }
        }
        int h = 0;
        double R[] = new double[(int) lower_bound];
        Arrays.fill(R, 0);
        double accum = 0;

        for (h = 0; h< lower_bound;h++){
            accum += dd[h];
            if (h == 0) {
                R[h] = n*dd[h]/nSeed;
            }else{
                R[h] = n*accum / nSeed;
            }
        }
        //rechablePairs = R[(int) lower_bound];

        //effDiam = Stats.effectiveDiameter(R, 0.9);
        totalTime = System.currentTimeMillis() - startTime;
        logger.info("Algorithm successfully completed. Time elapsed (in milliseconds) {}", totalTime);

        GraphMeasureOpt graphMeasure = new GraphMeasureOpt();
        graphMeasure.setNumNodes(mGraph.numNodes());
        graphMeasure.setLowerBoundDiameter((int) lower_bound);
        graphMeasure.setThreshold(0.9);
        graphMeasure.setAvgDistance(Stats.averageDistance(R));
        graphMeasure.setTime(totalTime);
        graphMeasure.setEffectiveDiameter(Stats.effectiveDiameter(R, 0.9));
        graphMeasure.setTotalCouples(Stats.totalCouplesReachable(R));
        System.out.println("AVG DIST "+graphMeasure.getAvgDistance()+"  DIAM "+graphMeasure.getLowerBoundDiameter() + "   EFF DIA "+graphMeasure.getEffectiveDiameter()+"  R PAIRS "+graphMeasure.getTotalCouples());
    return graphMeasure;
    }




}

