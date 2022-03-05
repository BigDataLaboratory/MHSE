package it.bigdatalab.applications;

import it.bigdatalab.model.Parameter;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HyperBallCentralities {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.HyperBallCentralities");

    private final String mMode;
    private final Parameter mParam;
    private final GraphManager mGraph;
    public HyperBallCentralities(GraphManager g, Parameter param) {
        this.mGraph = g;
        this.mParam = param;
        this.mMode = Constants.DEFAULT_MODE;
    }

}
