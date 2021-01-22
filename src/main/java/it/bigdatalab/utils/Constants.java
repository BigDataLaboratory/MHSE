package it.bigdatalab.utils;

import java.util.concurrent.TimeUnit;

public class Constants {

    /*******************************************************************************
     *                                PATH&FILE
     * ****************************************************************************/

    public static final String DEFAULT_PROPERTIES_PATH = "etc/mhse.properties";
    public static final String DEFAULT_MODE = "WebGraph";
    public static final String WEBGRAPH = DEFAULT_MODE;
    public static final String BFS = "BFS";

    public static final String NAMESEPARATOR = "_";
    public static final String WITHISOLATED = "with_iso";
    public static final String WITHOUTISOLATED = "without_iso";
    public static final String SEEDNODE = "seednode";
    public static final String GT = "gt_";


    public static final String JSON_EXTENSION = ".json";
    public static final String GRAPH_EXTENSION = ".graph";

    /*******************************************************************************
     *                                     LOG
     * ****************************************************************************/

    public static long LOG_INTERVAL = TimeUnit.MILLISECONDS.toMillis(2000L);

    /*******************************************************************************
     *                                     GENERIC
     * ****************************************************************************/

    public static final String TRUE = "True";
    public static final String FALSE = "False";
    public static final String NUM_THREAD_DEFAULT = "1";

    /*******************************************************************************
     *                                     DIRECTION
     * ****************************************************************************/

    public static final String IN_DIRECTION = "in";
    public static final String OUT_DIRECTION = "out";

    /*******************************************************************************
     *                        OPTIMIZED VERSIONS: ARRAY DIM
     * ****************************************************************************/

    public static final int N = 5;

    /*******************************************************************************
     *                            BINARY OPERATIONS
     * ****************************************************************************/

    public static final int MASK = 5; // 2^6
    public static final int REMAINDER = 27;

    // for 64 array lenght (long)
/*
    public static final int MASK64 = 6; // 2^6
    public static final int REMAINDER64 = 58;
    public static final int BIT64 = 1;
*/
    public static final int BIT = 1;

    /*******************************************************************************
     *                                  SEED
     * ****************************************************************************/

    public static final String NUM_TEST_DEFAULT = "10";
    public static final String NUM_SEEDS_DEFAULT = "256";
    public static final String NUM_RUN_DEFAULT = "1";
    public static final String LOG2M_DEFAULT = "8";


}
