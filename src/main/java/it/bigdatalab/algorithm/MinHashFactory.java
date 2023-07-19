package it.bigdatalab.algorithm;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinHashFactory {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHashFactory");

    /**
     * Choose one of the algorithm to be executed by the type passed as parameter
     *
     * @param type algorithm type
     * @return algorithm to be executed
     */
    public MinHash getAlgorithm(ImmutableGraph g,
                                AlgorithmEnum type,
                                int numSeeds,
                                double threshold,
                                IntArrayList seeds,
                                int[] nodes,
                                int threads,
                                boolean centrality) throws IllegalArgumentException, MinHash.SeedsException {

        MinHash minHashAlgorithm = null;

        switch (type) {
            case MHSE:
                minHashAlgorithm = new MHSE(g, numSeeds, threshold, seeds);
                break;
            case BMHSE:
                minHashAlgorithm = new MHSEX(g, numSeeds, threshold, nodes, centrality);
                break;
            case SEMHSE:
                minHashAlgorithm = new SEMHSE(g, numSeeds, threshold, seeds);
                break;
            case SEBMHSE:
                minHashAlgorithm = new StandaloneBMinHash(g, numSeeds, threshold, nodes, centrality);
                break;
            case SEBMHSEMulti:
                minHashAlgorithm = new MultithreadBMinHash(g, numSeeds, threshold, nodes, threads, centrality);
                break;
            case BMHSEMulti:
                minHashAlgorithm = new MultithreadMHSEX(g, numSeeds, threshold, nodes, threads);
                break;
            case Expansion:
                minHashAlgorithm = new MultithreadExpansion(g, numSeeds, threshold, nodes, threads, centrality);
                break;
            default:
                throw new IllegalArgumentException("Algorithm name " + type + " not recognized");
        }
        logger.info("Selected " + type + " algorithm");
        return minHashAlgorithm;
    }

    /**
     * Choose one of the algorithm to be executed by the type passed as parameter
     *
     * @param type algorithm type
     * @return algorithm to be executed
     */
    public MinHash getAlgorithm(ImmutableGraph g,
                                AlgorithmEnum type,
                                int numSeeds,
                                double threshold,
                                int threads,
                                boolean centrality) throws IllegalArgumentException, MinHash.SeedsException {

        MinHash minHashAlgorithm = null;

        switch (type) {
            case MHSE:
                minHashAlgorithm = new MHSE(g, numSeeds, threshold);
                break;
            case BMHSE:
                minHashAlgorithm = new MHSEX(g, numSeeds, threshold, centrality);
                break;
            case SEMHSE:
                minHashAlgorithm = new SEMHSE(g, numSeeds, threshold);
                break;
            case SEBMHSE:
                minHashAlgorithm = new StandaloneBMinHash(g, numSeeds, threshold, centrality);
                break;
            case SEBMHSEMulti:
                minHashAlgorithm = new MultithreadBMinHash(g, numSeeds, threshold, threads, centrality);
                break;
            case BMHSEMulti:
                minHashAlgorithm = new MultithreadMHSEX(g, numSeeds, threshold, threads);
                break;
            case Expansion:
                minHashAlgorithm = new MultithreadExpansion(g, numSeeds, threshold, threads, centrality);
                break;
            default:
                throw new IllegalArgumentException("Algorithm name " + type + " not recognized");
        }
        logger.info("Selected " + type + " algorithm");
        return minHashAlgorithm;
    }
}
