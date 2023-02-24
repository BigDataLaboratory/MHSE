package it.bigdatalab.applications;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.bigdatalab.model.GraphMeasure;
import it.bigdatalab.model.GraphMeasureOpt;
import it.bigdatalab.model.Measure;
import it.bigdatalab.model.Parameter;
import it.bigdatalab.utils.Constants;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.Main");

    protected final Parameter mParam;

    /**
     * Set parameters for one of the available algorithm
     */
    public Main(Parameter param) {
        this.mParam = param;
    }

    /**
     * Write the statistics computed on the input graph in a JSON file
     *
     * @param measures input graph statistics
     * @param path     output file path of the JSON file
     */
    private static void writeOnFile(List<Measure> measures, String path) throws IOException {
        path += Constants.JSON_EXTENSION;

        RuntimeTypeAdapterFactory<Measure> typeAdapterFactory = RuntimeTypeAdapterFactory
                .of(Measure.class, "type")
                .registerSubtype(GraphMeasure.class, GraphMeasure.class.getName())
                .registerSubtype(GraphMeasureOpt.class, GraphMeasureOpt.class.getName());

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeAdapterFactory)
                .create();

        Type gmListType = new TypeToken<List<Measure>>() {
        }.getType();

        boolean exist = new File(path).isFile();
        List<Measure> graphMeasures = new ArrayList<>();

        if (exist) {
            FileReader fr = new FileReader(path);
            graphMeasures = gson.fromJson(fr, gmListType);
            fr.close();
            // If graph measures list is empty
            if (null == graphMeasures) {
                graphMeasures = new ArrayList<>();
            }
        }

        // Add new graphMeasure to the list
        graphMeasures.addAll(measures);
        // No append replace the whole file
        FileWriter fw = new FileWriter(path);
        gson.toJson(graphMeasures, gmListType, fw);
        fw.close();

        logger.info("Graph measure wrote in " + path);
    }

    /**
     * @return list of seeds' list read from external json file
     * @throws FileNotFoundException
     */
    private List<IntArrayList> readSeedsFromJson(String inputFilePath) throws FileNotFoundException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<IntArrayList>>() {
        }.getType();
        return gson.fromJson(new FileReader(inputFilePath), listType);
    }

    /**
     * @return list of nodes' list read from external json file
     * @throws FileNotFoundException
     */
    private List<int[]> readNodesFromJson(String inputFilePath) throws FileNotFoundException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<int[]>>() {
        }.getType();
        return gson.fromJson(new FileReader(inputFilePath), listType);
    }

    protected int[] computeNodesFromRange(int start, int end) {
        return IntStream.rangeClosed(start, end).toArray();
    }

    protected static int[] rangeNodes(@NotNull String range) {
        return Arrays.stream(range.split(",")).mapToInt(Integer::parseInt).toArray();
    }
}
