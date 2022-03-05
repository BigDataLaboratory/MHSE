package it.bigdatalab.applications;

import it.bigdatalab.model.GraphGtMeasure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.GsonHelper;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.algo.GeometricCentralities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class WebGraphCentralities {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.WebGraphCentralities");

    private final Parameter mParam;
    private final GraphManager mGraph;
    private GeometricCentralities GeoCentrality;

    public WebGraphCentralities(GraphManager g, Parameter param) {
        this.mGraph = g;
        this.mParam = param;
    }


    public void computeGeometric() throws InterruptedException {
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

    public static void main(String[] args) throws IOException, InterruptedException {
        String inputFilePath = PropertiesManager.getProperty("centrality.inputFilePath");
        String outputFolderPath = PropertiesManager.getProperty("centrality.outputFolderPath");
        int threadNumber = Integer.parseInt(PropertiesManager.getProperty("centrality.threadNumber"));
        boolean inMemory = Boolean.parseBoolean(PropertiesManager.getProperty("centrality.inMemory"));


        Parameter param = new Parameter.Builder()
                .setInputFilePathGraph(inputFilePath)
                .setOutputFolderPath(outputFolderPath)
                .setNumThreads(threadNumber)
                .setInMemory(inMemory)
                .setTranspose(false)
                .setWebG(true)
                .setCompG(false)
                .setECompG(false)
                .setDirection("out")
                .build();
        //     public GraphManager(boolean WG, boolean CG,String inputFilePath, boolean transpose,boolean inM,boolean isoV,String direction) throws IOException {
        GraphManager g = new GraphManager(param.getWebGraph(), param.getCompGraph(),param.getInputFilePathGraph(), param.isTranspose(), param.isInMemory(),param.keepIsolatedVertices(),param.getDirection(),param.getCompEGraph());

        WebGraphCentralities webGCentralities = new WebGraphCentralities(g, param);
        webGCentralities.computeGeometric();

    }




}
