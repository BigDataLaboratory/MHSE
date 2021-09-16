package it.bigdatalab.AlgorithmOnDGAP;

import it.bigdatalab.algorithm.MinHash;
import it.bigdatalab.algorithm.MultithreadBMinHash;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.structure.GraphManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MultithreadBMinHashTest {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.algorithm.MultithreadBMinHashTest");

    private Comparator<Integer> mLessThan;

    private static Stream<Arguments> cycleProvider() {
        return Stream.of(
                Arguments.of("in", new int[]{-372222666}, new int[]{1}, new Measure(1, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("in", new int[]{955522904, 1741204198}, new int[]{24, 28}, new Measure(2, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("in", new int[]{453057370, -456587907, 607574255, -2104320307}, new int[]{23, 4, 30, 28}, new Measure(4, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("in", new int[]{1970239154, 1757455415, 100373564, 363336826, 1406431479, 689901333, 1516449588, 1505011033}, new int[]{3, 15, 20, 29, 15, 19, 8, 24}, new Measure(8, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("in", new int[]{-664801267, 1258820520, -947422122, 1283387230, -1456301379, 899107882, 1353530495, 900655226, 535242463, 2115096797, 748669758, 725549636, -1886462093, -1116647959, -1743103279, 1134030635}, new int[]{31, 3, 2, 16, 31, 28, 12, 21, 6, 11, 0, 24, 18, 5, 22, 20}, new Measure(16, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("out", new int[]{-372222666}, new int[]{1}, new Measure(1, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("out", new int[]{955522904, 1741204198}, new int[]{24, 28}, new Measure(2, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("out", new int[]{453057370, -456587907, 607574255, -2104320307}, new int[]{23, 4, 30, 28}, new Measure(4, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("out", new int[]{1970239154, 1757455415, 100373564, 363336826, 1406431479, 689901333, 1516449588, 1505011033}, new int[]{3, 15, 20, 29, 15, 19, 8, 24}, new Measure(8, 31, 15.5, 27.8, 1024.0, 921.6)),
                Arguments.of("out", new int[]{-664801267, 1258820520, -947422122, 1283387230, -1456301379, 899107882, 1353530495, 900655226, 535242463, 2115096797, 748669758, 725549636, -1886462093, -1116647959, -1743103279, 1134030635}, new int[]{31, 3, 2, 16, 31, 28, 12, 21, 6, 11, 0, 24, 18, 5, 22, 20}, new Measure(16, 31, 15.5, 27.8, 1024.0, 921.6))
        );
    }

    private static Stream<Arguments> unCycleProvider() {
        return Stream.of(
                Arguments.of("in", new int[]{-372222666}, new int[]{1}, new Measure(1, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("in", new int[]{955522904, 1741204198}, new int[]{24, 28}, new Measure(2, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("in", new int[]{453057370, -456587907, 607574255, -2104320307}, new int[]{23, 4, 30, 28}, new Measure(4, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("in", new int[]{1970239154, 1757455415, 100373564, 363336826, 1406431479, 689901333, 1516449588, 1505011033}, new int[]{3, 15, 20, 29, 15, 19, 8, 24}, new Measure(8, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("in", new int[]{-664801267, 1258820520, -947422122, 1283387230, -1456301379, 899107882, 1353530495, 900655226, 535242463, 2115096797, 748669758, 725549636, -1886462093, -1116647959, -1743103279, 1134030635}, new int[]{31, 3, 2, 16, 31, 28, 12, 21, 6, 11, 0, 24, 18, 5, 22, 20}, new Measure(16, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("out", new int[]{-372222666}, new int[]{1}, new Measure(1, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("out", new int[]{955522904, 1741204198}, new int[]{24, 28}, new Measure(2, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("out", new int[]{453057370, -456587907, 607574255, -2104320307}, new int[]{23, 4, 30, 28}, new Measure(4, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("out", new int[]{1970239154, 1757455415, 100373564, 363336826, 1406431479, 689901333, 1516449588, 1505011033}, new int[]{3, 15, 20, 29, 15, 19, 8, 24}, new Measure(8, 16, 8.0, 13.9, 1024.0, 921.6)),
                Arguments.of("out", new int[]{-664801267, 1258820520, -947422122, 1283387230, -1456301379, 899107882, 1353530495, 900655226, 535242463, 2115096797, 748669758, 725549636, -1886462093, -1116647959, -1743103279, 1134030635}, new int[]{31, 3, 2, 16, 31, 28, 12, 21, 6, 11, 0, 24, 18, 5, 22, 20}, new Measure(16, 16, 8.0, 13.9, 1024.0, 921.6))
        );
    }

    private static Stream<Arguments> unWheelProvider() {
        return Stream.of(
                Arguments.of("in", new int[]{-372222666}, new int[]{1}, new Measure(1, 2, 1.84375, 1.8857142857142857, 1024.0, 921.6)),
                Arguments.of("in", new int[]{955522904, 1741204198}, new int[]{24, 28}, new Measure(2, 2, 1.84375, 1.8857142857142857, 1024.0, 921.6)),
                Arguments.of("in", new int[]{453057370, -456587907, 607574255, -2104320307}, new int[]{23, 4, 30, 28}, new Measure(4, 2, 1.84375, 1.8857142857142857, 1024.0, 921.6)),
                Arguments.of("in", new int[]{1970239154, 1757455415, 100373564, 363336826, 1406431479, 689901333, 1516449588, 1505011033}, new int[]{3, 15, 20, 29, 15, 19, 8, 24}, new Measure(8, 2, 1.84375, 1.8857142857142857, 1024.0, 921.6)),
                Arguments.of("in", new int[]{-664801267, 1258820520, -947422122, 1283387230, -1456301379, 899107882, 1353530495, 900655226, 535242463, 2115096797, 748669758, 725549636, -1886462093, -1116647959, -1743103279, 1134030635}, new int[]{31, 3, 2, 16, 31, 28, 12, 21, 6, 11, 0, 24, 18, 5, 22, 20}, new Measure(16, 2, 1.7890625, 1.8780952380952383, 1024.0, 921.6)),
                Arguments.of("out", new int[]{-372222666}, new int[]{1}, new Measure(1, 2, 1.84375, 1.8857142857142857, 1024.0, 921.6)),
                Arguments.of("out", new int[]{955522904, 1741204198}, new int[]{24, 28}, new Measure(2, 2, 1.84375, 1.8857142857142857, 1024.0, 921.6)),
                Arguments.of("out", new int[]{453057370, -456587907, 607574255, -2104320307}, new int[]{23, 4, 30, 28}, new Measure(4, 2, 1.84375, 1.8857142857142857, 1024.0, 921.6)),
                Arguments.of("out", new int[]{1970239154, 1757455415, 100373564, 363336826, 1406431479, 689901333, 1516449588, 1505011033}, new int[]{3, 15, 20, 29, 15, 19, 8, 24}, new Measure(8, 2, 1.84375, 1.8857142857142857, 1024.0, 921.6)),
                Arguments.of("out", new int[]{-664801267, 1258820520, -947422122, 1283387230, -1456301379, 899107882, 1353530495, 900655226, 535242463, 2115096797, 748669758, 725549636, -1886462093, -1116647959, -1743103279, 1134030635}, new int[]{31, 3, 2, 16, 31, 28, 12, 21, 6, 11, 0, 24, 18, 5, 22, 20}, new Measure(16, 2, 1.7890625, 1.8780952380952383, 1024.0, 921.6))
        );
    }

    private static Stream<Arguments> completeProvider() {
        return Stream.of(
                Arguments.of("in", new int[]{-372222666}, new int[]{1}, new Measure(1, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("in", new int[]{955522904, 1741204198}, new int[]{24, 28}, new Measure(2, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("in", new int[]{453057370, -456587907, 607574255, -2104320307}, new int[]{23, 4, 30, 28}, new Measure(4, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("in", new int[]{1970239154, 1757455415, 100373564, 363336826, 1406431479, 689901333, 1516449588, 1505011033}, new int[]{3, 15, 20, 29, 15, 19, 8, 24}, new Measure(8, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("in", new int[]{-664801267, 1258820520, -947422122, 1283387230, -1456301379, 899107882, 1353530495, 900655226, 535242463, 2115096797, 748669758, 725549636, -1886462093, -1116647959, -1743103279, 1134030635}, new int[]{31, 3, 2, 16, 31, 28, 12, 21, 6, 11, 0, 24, 18, 5, 22, 20}, new Measure(16, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("out", new int[]{-372222666}, new int[]{1}, new Measure(1, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("out", new int[]{955522904, 1741204198}, new int[]{24, 28}, new Measure(2, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("out", new int[]{453057370, -456587907, 607574255, -2104320307}, new int[]{23, 4, 30, 28}, new Measure(4, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("out", new int[]{1970239154, 1757455415, 100373564, 363336826, 1406431479, 689901333, 1516449588, 1505011033}, new int[]{3, 15, 20, 29, 15, 19, 8, 24}, new Measure(8, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6)),
                Arguments.of("out", new int[]{-664801267, 1258820520, -947422122, 1283387230, -1456301379, 899107882, 1353530495, 900655226, 535242463, 2115096797, 748669758, 725549636, -1886462093, -1116647959, -1743103279, 1134030635}, new int[]{31, 3, 2, 16, 31, 28, 12, 21, 6, 11, 0, 24, 18, 5, 22, 20}, new Measure(16, 1, 0.96875, 0.8967741935483872, 1024.0, 921.6))
        );
    }

    private static Stream<Arguments> pathProvider() {
        return Stream.of(
                Arguments.of("in", new int[]{-148195958}, new int[]{6}, new Measure(1, 18, 3.0, 5.3, 224.0, 201.6)),
                Arguments.of("in", new int[]{-1666501769, 377641651}, new int[]{4, 11}, new Measure(2, 13, 4.470588235294118, 9.3, 272.0, 244.8)),
                Arguments.of("in", new int[]{252785538, -136010130, -463078329, -1866071554}, new int[]{4, 20, 8, 2}, new Measure(4, 20, 6.815789473684211, 16.200000000000003, 304.0, 273.6)),
                Arguments.of("in", new int[]{1015761470, -954533682, -1809478130, 404567971, -2106778180, 1587577811, 1281329477, 136212347}, new int[]{9, 6, 29, 22, 12, 17, 19, 1}, new Measure(8, 29, 9.560975609756097, 19.35, 492.0, 442.8)),
                Arguments.of("in", new int[]{1511313872, -227918291, -1139497127, -372222666, -579314130, 673836020, 1270557561, -1872398185, -1541659072, -1732621646, 1190931453, 1560200934, 1138398279, -204183935, 2070631112, -1359976513}, new int[]{2, 28, 29, 1, 12, 16, 25, 5, 31, 26, 0, 20, 8, 10, 19, 23}, new Measure(16, 31, 11.118081180811808, 22.483333333333334, 542.0, 487.8)),
                Arguments.of("out", new int[]{-148195958}, new int[]{6}, new Measure(1, 25, 12.5, 22.400000000000002, 832.0, 748.8000000000001)),
                Arguments.of("out", new int[]{-1666501769, 377641651}, new int[]{4, 11}, new Measure(2, 27, 12.0, 22.1, 784.0, 705.6)),
                Arguments.of("out", new int[]{252785538, -136010130, -463078329, -1866071554}, new int[]{4, 20, 8, 2}, new Measure(4, 29, 12.287234042553191, 23.300000000000004, 752.0, 676.8000000000001)),
                Arguments.of("out", new int[]{1015761470, -954533682, -1809478130, 404567971, -2106778180, 1587577811, 1281329477, 136212347}, new int[]{9, 6, 29, 22, 12, 17, 19, 1}, new Measure(8, 30, 10.382978723404255, 20.96666666666667, 564.0, 507.6)),
                Arguments.of("out", new int[]{1511313872, -227918291, -1139497127, -372222666, -579314130, 673836020, 1270557561, -1872398185, -1541659072, -1732621646, 1190931453, 1560200934, 1138398279, -204183935, 2070631112, -1359976513}, new int[]{2, 28, 29, 1, 12, 16, 25, 5, 31, 26, 0, 20, 8, 10, 19, 23}, new Measure(16, 31, 10.852140077821012, 22.660000000000004, 514.0, 462.6))
        );
    }

    private static Stream<Arguments> tPathProvider() {
        return Stream.of(
                Arguments.of("in", new int[]{-148195958}, new int[]{6}, new Measure(1, 18, 3.0, 5.3, 224.0, 201.6)),
                Arguments.of("in", new int[]{-1666501769, 377641651}, new int[]{4, 11}, new Measure(2, 13, 4.470588235294118, 9.3, 272.0, 244.8)),
                Arguments.of("in", new int[]{252785538, -136010130, -463078329, -1866071554}, new int[]{4, 20, 8, 2}, new Measure(4, 20, 6.815789473684211, 16.200000000000003, 304.0, 273.6)),
                Arguments.of("in", new int[]{1015761470, -954533682, -1809478130, 404567971, -2106778180, 1587577811, 1281329477, 136212347}, new int[]{9, 6, 29, 22, 12, 17, 19, 1}, new Measure(8, 29, 9.560975609756097, 19.35, 492.0, 442.8)),
                Arguments.of("in", new int[]{1511313872, -227918291, -1139497127, -372222666, -579314130, 673836020, 1270557561, -1872398185, -1541659072, -1732621646, 1190931453, 1560200934, 1138398279, -204183935, 2070631112, -1359976513}, new int[]{2, 28, 29, 1, 12, 16, 25, 5, 31, 26, 0, 20, 8, 10, 19, 23}, new Measure(16, 31, 11.118081180811808, 22.483333333333334, 542.0, 487.8)),
                Arguments.of("out", new int[]{-148195958}, new int[]{6}, new Measure(1, 25, 12.5, 22.400000000000002, 832.0, 748.8000000000001)),
                Arguments.of("out", new int[]{-1666501769, 377641651}, new int[]{4, 11}, new Measure(2, 27, 12.0, 22.1, 784.0, 705.6)),
                Arguments.of("out", new int[]{252785538, -136010130, -463078329, -1866071554}, new int[]{4, 20, 8, 2}, new Measure(4, 29, 12.287234042553191, 23.300000000000004, 752.0, 676.8000000000001)),
                Arguments.of("out", new int[]{1015761470, -954533682, -1809478130, 404567971, -2106778180, 1587577811, 1281329477, 136212347}, new int[]{9, 6, 29, 22, 12, 17, 19, 1}, new Measure(8, 30, 10.382978723404255, 20.96666666666667, 564.0, 507.6)),
                Arguments.of("out", new int[]{1511313872, -227918291, -1139497127, -372222666, -579314130, 673836020, 1270557561, -1872398185, -1541659072, -1732621646, 1190931453, 1560200934, 1138398279, -204183935, 2070631112, -1359976513}, new int[]{2, 28, 29, 1, 12, 16, 25, 5, 31, 26, 0, 20, 8, 10, 19, 23}, new Measure(16, 31, 10.852140077821012, 22.660000000000004, 514.0, 462.6))
        );
    }

    private static Stream<Arguments> inStarProvider() {
        return Stream.of(
                Arguments.of("in", new int[]{-148195958}, new int[]{20}, new Measure(1, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("in", new int[]{-231125982}, new int[]{0}, new Measure(1, 1, 0.9696969696969697, 0.896875, 1089.0, 980.1)),
                Arguments.of("in", new int[]{1351495042, 600767559}, new int[]{31, 16}, new Measure(2, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("in", new int[]{1121441672, -355110414, -1081200504, 1212731417}, new int[]{20, 26, 7, 31}, new Measure(4, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("in", new int[]{571898839, -1432145860, -1133378059, 621916878, -1138635951, -1041970128, -1061396053, 332705999}, new int[]{14, 32, 27, 16, 8, 4, 15, 13}, new Measure(8, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("in", new int[]{-1661843915, -328937796, 571898839, -1999389973, 11310977, -1781139022, -1001108646, -2046205192, -444995994, -304731378, -724798370, 1784683099, 813722321, -1061396053, 621916878, -1041970128}, new int[]{12, 32, 14, 29, 13, 21, 25, 0, 1, 22, 18, 31, 27, 15, 16, 4}, new Measure(16, 1, 0.6666666666666666, 0.8500000000000001, 99.0, 89.10000000000001)),
                Arguments.of("out", new int[]{1828442608}, new int[]{20}, new Measure(1, 1, 0.5, 0.7999999999999999, 66.0, 59.4)),
                Arguments.of("out", new int[]{-231125982}, new int[]{0}, new Measure(1, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("out", new int[]{1351495042, 600767559}, new int[]{31, 16}, new Measure(2, 1, 0.5, 0.7999999999999999, 66.0, 59.4)),
                Arguments.of("out", new int[]{1121441672, -355110414, -1081200504, 1212731417}, new int[]{20, 26, 7, 31}, new Measure(4, 1, 0.5, 0.7999999999999999, 66.0, 59.4)),
                Arguments.of("out", new int[]{571898839, -1432145860, -1133378059, 621916878, -1138635951, -1041970128, -1061396053, 332705999}, new int[]{14, 32, 27, 16, 8, 4, 15, 13}, new Measure(8, 1, 0.5, 0.7999999999999999, 66.0, 59.4)),
                Arguments.of("out", new int[]{-1661843915, -328937796, 571898839, -1999389973, 11310977, -1781139022, -1001108646, -2046205192, -444995994, -304731378, -724798370, 1784683099, 813722321, -1061396053, 621916878, -1041970128}, new int[]{12, 32, 14, 29, 13, 21, 25, 0, 1, 22, 18, 31, 27, 15, 16, 4}, new Measure(16, 1, 0.4838709677419355, 0.7933333333333334, 63.9375, 57.54375))
        );
    }

    private static Stream<Arguments> outStarProvider() {
        return Stream.of(
                Arguments.of("in", new int[]{-148195958}, new int[]{20}, new Measure(1, 1, 0.5, 0.7999999999999999, 66.0, 59.4)),
                Arguments.of("in", new int[]{-231125982}, new int[]{0}, new Measure(1, 0, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("in", new int[]{1351495042, 600767559}, new int[]{31, 16}, new Measure(2, 1, 0.5, 0.7999999999999999, 66.0, 59.4)),
                Arguments.of("in", new int[]{1121441672, -355110414, -1081200504, 1212731417}, new int[]{20, 26, 7, 31}, new Measure(4, 1, 0.5, 0.7999999999999999, 66.0, 59.4)),
                Arguments.of("in", new int[]{571898839, -1432145860, -1133378059, 621916878, -1138635951, -1041970128, -1061396053, 332705999}, new int[]{14, 32, 27, 16, 8, 4, 15, 13}, new Measure(8, 1, 0.5, 0.7999999999999999, 66.0, 59.4)),
                Arguments.of("in", new int[]{-1661843915, -328937796, 571898839, -1999389973, 11310977, -1781139022, -1001108646, -2046205192, -444995994, -304731378, -724798370, 1784683099, 813722321, -1061396053, 621916878, -1041970128}, new int[]{12, 32, 14, 29, 13, 21, 25, 0, 1, 22, 18, 31, 27, 15, 16, 4}, new Measure(16, 1, 0.4838709677419355, 0.7933333333333334, 63.9375, 57.54375)),
                Arguments.of("out", new int[]{1828442608}, new int[]{20}, new Measure(1, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("out", new int[]{-231125982}, new int[]{0}, new Measure(1, 1, 0.9696969696969697, 0.896875, 1089.0, 980.1)),
                Arguments.of("out", new int[]{1351495042, 600767559}, new int[]{31, 16}, new Measure(2, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("out", new int[]{1121441672, -355110414, -1081200504, 1212731417}, new int[]{20, 26, 7, 31}, new Measure(4, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("out", new int[]{571898839, -1432145860, -1133378059, 621916878, -1138635951, -1041970128, -1061396053, 332705999}, new int[]{14, 32, 27, 16, 8, 4, 15, 13}, new Measure(8, 1, 0.0, 0.0, 33.0, 29.7)),
                Arguments.of("out", new int[]{-1661843915, -328937796, 571898839, -1999389973, 11310977, -1781139022, -1001108646, -2046205192, -444995994, -304731378, -724798370, 1784683099, 813722321, -1061396053, 621916878, -1041970128}, new int[]{12, 32, 14, 29, 13, 21, 25, 0, 1, 22, 18, 31, 27, 15, 16, 4}, new Measure(16, 1, 0.6666666666666666, 0.8500000000000001, 99.0, 89.10000000000001))
        );
    }

    @BeforeEach
    void setUp() {
        this.mLessThan = (x, y) -> x <= y ? 0 : 1;
    }

    @AfterEach
    void tearDown() {
        this.mLessThan = null;
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("cycleProvider")
    void testAlgorithm_DiCycle(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_directed_compressed_dgaps/32-cycle.adjlist").getAbsolutePath();


        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());
        Measure measure2 = algo2.runAlgorithm();

        assertThat(measure2)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);

    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("pathProvider")
    void testAlgorithm_DiPath(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_directed_compressed_dgaps/32-path.adjlist.txt").getAbsolutePath();

        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();


        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());
        Measure measure2 = algo2.runAlgorithm();

        assertThat(measure2)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("tPathProvider")
    void testAlgorithm_DiTPath(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_directed_compressed_dgaps/32t-path.adjlist.txt").getAbsolutePath();



        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(true)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();


        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());
        Measure measure2 = algo2.runAlgorithm();

        assertThat(measure2)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("inStarProvider")
    void testAlgorithm_DiInStar(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_directed_compressed_dgaps/32in-star.adjlist.txt").getAbsolutePath();

        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();


        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());
        Measure measure2 = algo2.runAlgorithm();

        assertThat(measure2)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("outStarProvider")
    void testAlgorithm_DiOutStar(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_directed_compressed_dgaps/32out-star.adjlist.txt").getAbsolutePath();


        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());
        Measure measure2 = algo2.runAlgorithm();

        assertThat(measure2)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }


    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("unCycleProvider")
    void testAlgorithm_UnCycle(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_undirected_compressed_dgaps/32-cycle.adjlist.txt").getAbsolutePath();
        path2 = path2.substring(0, path2.lastIndexOf('.'));

        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());
        Measure measure2 = algo2.runAlgorithm();

        assertThat(measure2)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("unWheelProvider")
    void testAlgorithm_UnWheel(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_undirected_compressed_dgaps/32-wheel.adjlist.txt").getAbsolutePath();


        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());
        Measure measure2 = algo2.runAlgorithm();

        assertThat(measure2)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("completeProvider")
    void testAlgorithm_Complete(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_undirected_compressed_dgaps/32-complete.adjlist.txt").getAbsolutePath();


        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());
        Measure measure2 = algo2.runAlgorithm();

        assertThat(measure2)
                .usingRecursiveComparison()
                .ignoringFields("mCollisionsTable", "mHopTable", "mThreshold", "mMaxMemoryUsed", "mTime", "mAlgorithmName", "mMinHashNodeIDs", "mSeedsList", "mNumNodes", "mNumArcs", "mSeedsTime", "mLastHops", "mRun")
                .withComparatorForFields(mLessThan, "mLowerBoundDiameter")
                .isEqualTo(expected);

    }

    @ParameterizedTest(name = "{index} => direction={0}, seeds={1}, nodes={2}, expected={3}")
    @MethodSource("cycleProvider")
    void testAlgorithm_DiCycle_checkSizeCollisionHopTable(String direction, int[] seeds, int[] nodes, Measure expected) throws IOException, MinHash.SeedsException {

        String path2 = new File("src/test/data/g_directed_compressed_dgaps/32-cycle.adjlist.txt").getAbsolutePath();


        Parameter param2 = new Parameter.Builder()
                .setInputFilePathGraph(path2)
                .setIsolatedVertices(true)
                .setInMemory(true)
                .setNumSeeds(seeds.length)
                .setDirection(direction)
                .setTranspose(false)
                .setSeedsRandom(false)
                .setWebG(false)
                .setCompG(true)
                .setDifferentialCompression(true)
                .setThreshold(0.9)
                .setNumThreads(4)
                .build();

        GraphManager g2 = new GraphManager(param2.getWebGraph(),param2.getCompGraph(),param2.getInputFilePathGraph(),param2.isTranspose(),param2.isInMemory(),param2.keepIsolatedVertices(), param2.getDirection());
        MultithreadBMinHash algo2 = new MultithreadBMinHash(g2, param2.getNumSeeds(), param2.getThreshold(), nodes, param2.getNumThreads());

        GraphMeasure measure2 =   (GraphMeasure) algo2.runAlgorithm();

        // check hop table size (equals to lower bound + 1)
        // check collisions table # rows (equals to lower bound + 1)
        // check collisions table # cols (equals to # seed)
        SoftAssertions assertions2 = new SoftAssertions();
        assertions2.assertThat(measure2.getLastHops()).as("Last hops size").hasSize(seeds.length);
        assertions2.assertThat(measure2.getHopTable()).as("HopTable size").hasSize(measure2.getLowerBoundDiameter() + 1);
        assertions2.assertThat(measure2.getCollisionsTable()).as("CollisionsTable # rows").hasSize(measure2.getLowerBoundDiameter() + 1);
        assertions2.assertThat(measure2.getCollisionsTable().values()).extracting(record -> record.length).as("CollisionsTable # cols").containsOnly(seeds.length);
        assertions2.assertAll();

    }


    @Test
    void testNormalizeCollisionsTable() {
        Int2ObjectOpenHashMap<int[]> collisionTable = new Int2ObjectOpenHashMap<>();
        collisionTable.put(0, new int[]{1, 0, 0, 0, 0, 0, 0});
        collisionTable.put(1, new int[]{1, 4, 0, 0, 0, 0, 0});
        collisionTable.put(2, new int[]{1, 32, 54, 0, 0, 0, 0});
        collisionTable.put(3, new int[]{1, 4, 32, 55, 98, 101, 201});

        int nseed = 7;
        int lowerBoundDiameter = 3;
        MultithreadBMinHash algo = new MultithreadBMinHash(null, 7, 0.9, new int[]{0, 1, 2, 3, 4, 5, 6}, 1);
        algo.normalizeCollisionsTable(collisionTable);

        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(collisionTable).as("CollisionsTable # rows").hasSize(lowerBoundDiameter + 1);
        assertions.assertThat(collisionTable.values()).extracting(record -> record.length).as("CollisionsTable # cols").containsOnly(nseed);
        assertions.assertAll();
    }
}