package it.bigdatalab.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MinHashFactory {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MinHashFactory");


    /**
     * Empty constructor
     */
    public MinHashFactory(){}

    /**
     * Choose one of the algorithm to be executed by the type passed as parameter
     * @param type algorithm type
     * @return algorithm to be executed
     * @throws MinHash.DirectionNotSetException
     * @throws MinHash.SeedsException
     * @throws IOException
     */
    public MinHash getAlgorithm (AlgorithmEnum type) throws MinHash.DirectionNotSetException, MinHash.SeedsException, IOException {
        MinHash minHashAlgorithm = null;
        boolean error = false;

        switch (type){
            case MHSE:
                minHashAlgorithm = new MHSE();
                break;
            case SEMHSE:
                minHashAlgorithm = new SEMHSE();
                break;
            case StandaloneBMinHash:
                minHashAlgorithm = new StandaloneBMinHash();
                break;
            case StandaloneBMinHashOptimized:
                minHashAlgorithm = new StandaloneBMinHashOptimized();
                break;
            case MultithreadBMinHash:
                minHashAlgorithm = new StandaloneBMinHash();
                break;
            default:
                error = true;
                logger.error("Algorithm name not recognized");
        }
        if(!error){
            logger.info("Selected " + type + " algorithm");
        }
        return minHashAlgorithm;
    }
}
