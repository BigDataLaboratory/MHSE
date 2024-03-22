package it.bigdatalab.utils;

import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stats {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.utils.Stats");


    /**
     * Compute the average distance using the hop table
     *
     * @return average distance for the graph
     */
    public static double averageDistance(Int2DoubleLinkedOpenHashMap h) {
        int lowerBoundDiameter;
        double sumAvg = 0;
        // case map
        if (h.size() == 0) {
            return 0;
        }
        lowerBoundDiameter = h.size() - 1;

        for (Int2DoubleMap.Entry entry : h.int2DoubleEntrySet()) {
            int key = entry.getIntKey();
            double value = entry.getDoubleValue();
            if (key == 0) {
                sumAvg += 0;
            } else {
                sumAvg += (key * (value - h.get(key - 1)));
            }
        }
        return (sumAvg / h.get(lowerBoundDiameter));
    }

    /**
     * Compute the average distance using the hop table
     *
     * @return average distance for the graph
     */
    @NotNull
    public static double averageDistance(double[] h) {
        if (h.length == 0) return 0;
        double[] distance = distanceFunction(h);
        double m = 0.0D;
        int lowerBoundDiameter = distance.length - 1;

        for (int i = 1; i < distance.length; i++) {
            m += (distance[i] * (double) i);
        }
        return m / h[lowerBoundDiameter];
    }

    /**
     * @return effective diameter of the graph (computed using hop table), defined as the @{threshold}
     * percentile distance between nodes, hat is the minimum distance
     * that allows to connect the threshold-th percent of all reachable pairs
     */
    public static double effectiveDiameter(double @NotNull [] h, double threshold) {
        if (h.length == 0) return 0;

        double totalCouplesReachable = totalCouplesReachable(h);
        int d = 0;
        while (h[d] / totalCouplesReachable < threshold) {
            d += 1;
        }

        return (d != 0) ? (d - 1) + interpolate((h[d - 1]), (h[d]), threshold * totalCouplesReachable) : 0;
    }

    /**
     * @return effective diameter of the graph (computed using hop table), defined as the 90th percentile distance between nodes,
     * that is the minimum distance that allows to connect the 90th percent of all reachable pairs
     */
    public static double effectiveDiameter(Int2DoubleLinkedOpenHashMap h, double threshold) {
        if (h == null) return 0;

        double totalCouplesReachable = totalCouplesReachable(h);
        int d = 0;
        while ((h.get(d) / totalCouplesReachable) < threshold) {
            d += 1;
        }

        return (d != 0) ? (d - 1) + interpolate((h.get(d - 1)), (h.get(d)), threshold * totalCouplesReachable) : 0;
    }


    /**
     * @return total number of reachable pairs (last hop)
     */
    @NotNull
    public static double totalCouplesReachable(double[] h) {
        return h[h.length - 1]; // h.length - 1 is the lower bound diameter
    }

    /**
     * @return percentage of number of reachable pairs (last hop)
     */
    @NotNull
    public static double totalCouplesPercentage(double[] h, double threshold) {
        return totalCouplesReachable(h) * threshold;
    }

    /**
     * @return total number of reachable pairs (last hop)
     */
    public static double totalCouplesReachable(Int2DoubleLinkedOpenHashMap h) {
        return h.get(h.size() - 1);
    }

    /**
     * @return percentage of number of reachable pairs (last hop)
     */
    public static double totalCouplesPercentage(Int2DoubleLinkedOpenHashMap h, double threshold) {
        return totalCouplesReachable(h) * threshold;
    }


    @NotNull
    public static double interpolate(double y0, double y1, double y) {
        // (y1 - y0) is the delta neighbourhood
        return (y - y0) / (y1 - y0);
    }

    public static double @NotNull [] distanceFunction(double[] h) {
        double[] hopTable = h.clone();
        for (int i = hopTable.length; i-- != 1; hopTable[i] -= hopTable[i - 1]) {
        }

        return hopTable;
    }
    // We need to check these three functions
    public static double [] ClosenessCentrality(int n,int s, double [] fareness,boolean normalized){
       double[] closenessCentrality = new double[n];
       int i;
       for (i = 0; i < n; i++){
           closenessCentrality[i] += (s * (n-1)) / (n * fareness[i]);
            //To check how to normalize
           /*
           if (normalized){
               closenessCentrality[i] +=  s  / (n * fareness[i]);
           }else{
           }
            */
       }

       return closenessCentrality;
    }

    public static double [] HarmonicCentrality(int n,int s, double [] fareness){
        double[] harmonicCentrality = new double[n];
        int i;
        for (i = 0; i < n; i++){
            if (fareness[i] > 0){
                harmonicCentrality[i] += 1. / (((double) n /(s*(n-1)))*fareness[i]);
            } else{
                harmonicCentrality[i] += 0;
            }

        }
        /*
        for (i = 0; i < n; i++){
            harmonicCentrality[i] += harmonicCentrality[i]* ((double) n /(s*(n-1)));
        }
         */
        return harmonicCentrality;
    }
    // Consider normalization
    public static double [] LinnCentrality(int n,int s, double [] fareness, double[] R){
        double[] LinnCentrality = new double[n];
        int i;
        int l = R.length -1;
        for (i = 0; i < n; i++) {
            if (fareness[i] > 0){
                LinnCentrality[i] +=  ((double) (s/n) * (R[l] * R[l]) / fareness[i]);
            }else{
                LinnCentrality[i] += 0;
            }
        }
        /*
        for (i = 0; i < n; i++){
            LinnCentrality[i] += LinnCentrality[i]* ((double) n /(n-1));
        }
         */
        return LinnCentrality;
    }
}
