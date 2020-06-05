package it.misebigdatalab.algorithm;

import java.io.IOException;

public class MinHashFactory {

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
        switch (type){
            case MHSE:
                minHashAlgorithm = new MHSE();
                break;
            case SEMHSE:
                minHashAlgorithm = new SEMHSE();
                break;
        }
        return minHashAlgorithm;
    }
}
