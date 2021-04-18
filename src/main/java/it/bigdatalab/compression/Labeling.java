package it.bigdatalab.compression;

import it.bigdatalab.structure.CompressedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Labeling {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.compression.Labeling");
    private int [][] LGraph;
    private CompressedGraph Graph;
    public Labeling(CompressedGraph graph){
        Graph = graph;
    }
    // Da finire, valuta se usare l'oggetto o meno.
    public void edgeDistributionLabeling(){
        /*
        int [][] outdegrees;
        int i,n;
        //n = Graph.length;
        outdegrees = new int[n][1];
        for (i = 0; i< n;i++){
            outdegrees[i][0] = Graph.[i][0];
            outdegrees[i][1] = Graph.outdegree()
        }

         */
    }
}
