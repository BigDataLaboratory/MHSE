package it.uniroma2.algorithm;

import java.io.IOException;

public class MinHashFactory {

    public MinHashFactory(){}

    public MinHash getAlgorithm (AlgorithmEnum type) throws MinHash.DirectionNotSetException, MinHash.SeedsException, IOException {
        MinHash minHashAlgorithm = null;
        switch (type){
            case MHSE:
                minHashAlgorithm = new MHSE();
                break;
            case SEMHSE:
                minHashAlgorithm = new SEMHSE();
                break;
            case BooleanClassicalMinHash:
                minHashAlgorithm = new BooleanClassicalMinHash();
                break;
            case BooleanMinHash:
                minHashAlgorithm = new BooleanMinHash();
                break;
            case OriginalBooleanMinHash:
                minHashAlgorithm = new OriginalBooleanMinHash();
                break;
            case SlowBooleanMinHash:
                minHashAlgorithm = new SlowBooleanMinHash();
                break;
        }
        return minHashAlgorithm;
    }
}
