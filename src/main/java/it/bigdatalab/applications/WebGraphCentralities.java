package it.bigdatalab.applications;

import it.bigdatalab.model.Parameter;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.Constants;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.algo.GeometricCentralities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebGraphCentralities {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.WebGraphCentralities");

    private final String mMode;
    private final Parameter mParam;
    private final GraphManager mGraph;
    private GeometricCentralities GeoCentrality;

    public WebGraphCentralities(GraphManager g, Parameter param) {
        this.mGraph = g;
        this.mParam = param;
        this.mMode = Constants.DEFAULT_MODE;
    }


    public void computeCloseness() throws InterruptedException {
        ProgressLogger pl = new ProgressLogger();
        GeoCentrality = new GeometricCentralities(mGraph.get_mGraph(),mParam.getNumThreads(),pl);
        logger.info("Computing geometric centralities, using "+mParam.getNumThreads()+" threads");
        GeoCentrality.compute();
        logger.info("Geometric centralities computed.");
    }
    public double[] get_closeness(){
        return (GeoCentrality.closeness);
    }
    public double[] get_harmonic(){
        return (GeoCentrality.harmonic);
    }
    public double[] get_lin(){
        return (GeoCentrality.lin);
    }
    public long[] get_reachable_nodes(){
        return (GeoCentrality.reachable);
    }

}
