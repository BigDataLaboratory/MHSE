package it.bigdatalab.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MinHashFactory {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHashFactory");


    /**
     * Choose one of the algorithm to be executed by the type passed as parameter
     * @param type algorithm type
     * @return algorithm to be executed
     * @throws MinHash.DirectionNotSetException
     * @throws MinHash.SeedsException
     * @throws IOException
     */
    public MinHash getAlgorithm(AlgorithmEnum type,
                                String inputFilePath, boolean isSeedsRandom, boolean isolatedVertices, String direction, int numSeeds, double threshold) throws MinHash.DirectionNotSetException, MinHash.SeedsException, IOException {
        MinHash minHashAlgorithm = null;
        boolean error = false;

        switch (type){
            case MHSE:
                minHashAlgorithm = new MHSE(inputFilePath, isSeedsRandom, isolatedVertices, direction, numSeeds, threshold);
                break;
            case SEMHSE:
                minHashAlgorithm = new SEMHSE(inputFilePath, isSeedsRandom, isolatedVertices, direction, numSeeds, threshold);
                break;
            case StandaloneBMinHash:
                minHashAlgorithm = new StandaloneBMinHash(inputFilePath, isSeedsRandom, isolatedVertices, direction, numSeeds, threshold);
                break;
            case StandaloneBMinHashOptimized:
                minHashAlgorithm = new StandaloneBMinHashOptimized(inputFilePath, isSeedsRandom, isolatedVertices, direction, numSeeds, threshold);
                break;
            case MultithreadBMinHash:
                minHashAlgorithm = new MultithreadBMinHash(inputFilePath, isSeedsRandom, isolatedVertices, direction, numSeeds, threshold);
                break;
            case MultithreadBMinHashOptimized:
                minHashAlgorithm = new MultithreadBMinHashOptimized(inputFilePath, isSeedsRandom, isolatedVertices, direction, numSeeds, threshold);
                break;
            default:
                error = true;
                logger.error("Algorithm name not recognized");
                break;
        }
        if(!error){
            logger.info("Selected " + type + " algorithm");
        }
        return minHashAlgorithm;
    }
}
