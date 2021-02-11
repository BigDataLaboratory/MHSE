package it.bigdatalab.algorithm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MinHashFactoryTest {

    @Test
    public void testGetAlgorithm_throwsException() {
        MinHashFactory minHashFactory = new MinHashFactory();
        Assertions.assertThrows(IllegalArgumentException.class, () -> minHashFactory.getAlgorithm(null, AlgorithmEnum.valueOf("NonExisting Algorithm"), 1, 1, 1, false));
    }
}