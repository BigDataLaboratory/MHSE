package it.bigdatalab.algorithm;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
//import org.jgrapht.alg.scoring;

import java.io.IOException;
import java.util.*;


public class Clustering {

    private long [] clustersLables;
    private long [] clustersSignatures;
    private long [] clustersCentroids;
    private Int2ObjectOpenHashMap<long [] > clusterAssignments;


    public Clustering() throws IOException, MinHash.DirectionNotSetException {
        super();
        // Working on a new version of the clustering algorithm

    }
}
