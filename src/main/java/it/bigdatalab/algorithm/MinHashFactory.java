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
     * @throws MinHash.SeedsException
     * @throws IllegalArgumentException
     */
    public MinHash getAlgorithm(ImmutableGraph g,
                                AlgorithmEnum type,
                                int numSeeds,
                                double threshold,
                                IntArrayList seeds,
                                int[] nodes,
                                int threads) throws IllegalArgumentException, MinHash.SeedsException {

        MinHash minHashAlgorithm = null;

        switch (type) {
            case MHSE:
                minHashAlgorithm = new MHSE(g, numSeeds, threshold, seeds);
                break;
            case MHSEBSide:
                minHashAlgorithm = new MHSEBSide(g, numSeeds, threshold, nodes);
                break;
            case SEMHSE:
                minHashAlgorithm = new SEMHSE(g, numSeeds, threshold, seeds);
                break;
            case StandaloneBMinHash:
                minHashAlgorithm = new StandaloneBMinHash(g, numSeeds, threshold, nodes);
                break;
            case StandaloneBMinHashOptimized:
                minHashAlgorithm = new StandaloneBMinHashOptimized(g, numSeeds, threshold, nodes);
                break;
            case MultithreadBMinHash:
                minHashAlgorithm = new MultithreadBMinHash(g, numSeeds, threshold, nodes, threads);
                break;
            case MultithreadBMinHashOptimized:
                minHashAlgorithm = new MultithreadBMinHashOptimized(g, numSeeds, threshold, nodes, threads);
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
     * @throws MinHash.SeedsException
     * @throws IllegalArgumentException
     */
    public MinHash getAlgorithm(ImmutableGraph g,
                                AlgorithmEnum type,
                                int numSeeds,
                                double threshold,
                                int threads) throws IllegalArgumentException, MinHash.SeedsException {

        MinHash minHashAlgorithm = null;

        switch (type) {
            case MHSE:
                minHashAlgorithm = new MHSE(g, numSeeds, threshold);
                break;
            case MHSEBSide:
                minHashAlgorithm = new MHSEBSide(g, numSeeds, threshold);
                break;
            case SEMHSE:
                minHashAlgorithm = new SEMHSE(g, numSeeds, threshold);
                break;
            case StandaloneBMinHash:
                minHashAlgorithm = new StandaloneBMinHash(g, numSeeds, threshold);
                break;
            case StandaloneBMinHashOptimized:
                minHashAlgorithm = new StandaloneBMinHashOptimized(g, numSeeds, threshold);
                break;
            case MultithreadBMinHash:
                minHashAlgorithm = new MultithreadBMinHash(g, numSeeds, threshold, threads);
                break;
            case MultithreadBMinHashOptimized:
                minHashAlgorithm = new MultithreadBMinHashOptimized(g, numSeeds, threshold, threads);
                break;
            default:
                throw new IllegalArgumentException("Algorithm name " + type + " not recognized");
        }
        logger.info("Selected " + type + " algorithm");
        return minHashAlgorithm;
    }
}
