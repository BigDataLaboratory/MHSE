package it.bigdatalab.utils;

import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsTest {

    private double[] d;
    private int[] index;

    @BeforeEach
    void setUp() {
        d = new double[]{300647.0, 1503235.0, 1.27962879375E7, 9.75599515E7, 6.187878973125E8, 3.47841062825E9, 1.1904456192875E10, 2.31722735728125E10, 3.161235559425E10, 3.69083088705625E10, 4.02138098929375E10, 4.1734200562375E10, 4.2273185471625E10, 4.24537803664375E10, 4.251516872575E10, 4.2535650302625E10, 4.2541888727875E10, 4.25441247899375E10, 4.25447260839375E10, 4.25448012456875E10};
        index = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
    }

    @AfterEach
    void tearDown() {
        d = null;
        index = null;
    }

    @Test
    void testAverageDistanceArray() {
        double avgDistance = Stats.averageDistance(d);
        double expected = 7.54940501454615;
        assertEquals(expected, avgDistance);
    }

    @Test
    void testAverageDistanceList() {
        Int2DoubleLinkedOpenHashMap dataList = new Int2DoubleLinkedOpenHashMap(index, d);
        double avgDistance = Stats.averageDistance(dataList);
        double expected = 7.54940501454615;

        assertEquals(expected, avgDistance);
    }

    @Test
    void testEffectiveDiameterArray() {
        double threshold = 0.9;
        double expected = 9.418094637152246;
        assertEquals(expected, Stats.effectiveDiameter(d, threshold));
    }

    @Test
    void testEffectiveDiameterList() {
        Int2DoubleLinkedOpenHashMap dataList = new Int2DoubleLinkedOpenHashMap(index, d);
        double threshold = 0.9;
        double expected = 9.418094637152246;
        assertEquals(expected, Stats.effectiveDiameter(dataList, threshold));
    }

    @Test
    void testTotalCouplesReachableArray() {
        double expected = 42544801245.6875;
        assertEquals(expected, Stats.totalCouplesReachable(d), "Total couples reachable must be the lower bound - 1 (last) value of hop table");
    }

    @Test
    void totalCouplesPercentageList() {
        Int2DoubleLinkedOpenHashMap dataList = new Int2DoubleLinkedOpenHashMap(index, d);
        double threshold = 0.9;
        double expected = 38290321121.11875;

        assertEquals(expected, Stats.totalCouplesPercentage(dataList, threshold), "Total couples reachable in percentage must be the lower bound - 1 (last) value of hop table multiplied with threshold");
    }

    @Test
    void testTotalCouplesReachableList() {
        Int2DoubleLinkedOpenHashMap dataList = new Int2DoubleLinkedOpenHashMap(index, d);
        double expected = 42544801245.6875;

        assertEquals(expected, Stats.totalCouplesReachable(dataList), "Total couples reachable must be the lower bound - 1 (last) value of hop table");
    }

    @Test
    void testTotalCouplesPercentageArray() {
        double threshold = 0.9;
        double expected = 38290321121.11875;
        assertEquals(expected, Stats.totalCouplesPercentage(d, threshold), "Total couples reachable in percentage must be the lower bound - 1 (last) value of hop table multiplied with threshold");
    }

    @Test
    void interpolate() {
        double inter = Stats.interpolate(10.0, 7.0, 3.0);
        double expected = 2.3333333333333335;
        assertEquals(expected, inter);
    }

    @Test
    void distanceFunction() {
        double[] data = new double[]{4.2541888727875E10, 4.25441247899375E10, 4.25447260839375E10, 4.25448012456875E10};
        double[] expected = new double[]{4.2541888727875E10, 2236062.0625, 601294.0, 75161.75};

        assertArrayEquals(expected, Stats.distanceFunction(data));
    }
}