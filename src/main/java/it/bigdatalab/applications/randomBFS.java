package it.bigdatalab.applications;

import it.bigdatalab.model.GraphGtMeasure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.Constants;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.algo.ParallelBreadthFirstVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class randomBFS {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.RandomBFS");
    private final Parameter mParam;
    private final ImmutableGraph mGraph;
    private final int nSeed;

    public randomBFS(ImmutableGraph g, Parameter param,int seedNumber) {
        this.mGraph = g;
        this.mParam = param;
        this.nSeed = seedNumber;
    }
    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
    public GraphGtMeasure run(){
        long startTime = System.currentTimeMillis();
        ProgressLogger pl = new ProgressLogger();
        long visitedNodes = 0;
        double max = 0;
        double avgDistance = 0;
        double effDiam = 0;
        double rechablePairs = 0;
        Int2ObjectOpenHashMap<int[]> collisionsTable = new Int2ObjectOpenHashMap<>();       //for each hop a list of collisions for each seed
        ParallelBreadthFirstVisit bfs = new ParallelBreadthFirstVisit(mGraph, mParam.getNumThreads(), false, pl);
        NodeIterator nodeIterator = mGraph.nodeIterator();
        int seed = 0;
        for (int i = 0; i< nSeed; i++){
            seed = getRandomNumber(0,mGraph.numNodes());
            bfs.clear();
            int nodeNumber = bfs.visit(seed);
            if (max < bfs.maxDistance()){
                max = bfs.maxDistance();
            }
            // DEVI CREARE LA HOP TABLE PER QUESTO METODO E SALVARE IL NUMERO DI NODI VISITATO AD OGNI LIVELLO
        }
    }
}
