package it.bigdatalab.algorithm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PropagateFactoryTest {

    @Test
    public void testGetAlgorithm_throwsException() {
        PropagateFactory propagateFactory = new PropagateFactory();
        Assertions.assertThrows(IllegalArgumentException.class, () -> propagateFactory.getAlgorithm(null, AlgorithmEnum.valueOf("NonExisting Algorithm"), 1, 1, 1, false));
    }
}