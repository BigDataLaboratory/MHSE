package it.bigdatalab.algorithm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MinHashFactoryTest {

    @ParameterizedTest
    @EnumSource(AlgorithmEnum.class)
    void testGetAlgorithm_byAlgorithmName(AlgorithmEnum name) {
    }

}