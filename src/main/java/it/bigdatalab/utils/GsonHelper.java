package it.bigdatalab.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GsonHelper {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.utils.GsonHelper");

    private static final GsonBuilder gsonBuilder = new GsonBuilder();

    private GsonHelper() {
    }

    public static Gson getGson() {
        return gsonBuilder.create();
    }

    public static Gson getGson(RuntimeTypeAdapterFactory<?> adapter) {
        return gsonBuilder.registerTypeAdapterFactory(adapter).create();
    }

    /**
     * @param <T>              the 1st type of the desired object
     * @param modelClassObject an object of model class
     */
    public static <T> void toJson(T modelClassObject, String path) throws IOException {

        Type type = new TypeToken<T>() {
        }.getType();

        try (FileWriter fileWriter = new FileWriter(path)) {
            getGson().toJson(modelClassObject, type, fileWriter);
        }
    }


    /**
     * @param <T>                     the 1st type of the desired object
     * @param listOfModelClassObjects a list of objects of model class
     */
    public static <T> void toJson(List<T> listOfModelClassObjects, String path) throws IOException {
        Type type = new TypeToken<List<T>>() {
        }.getType();

        try (FileWriter fileWriter = new FileWriter(path)) {
            getGson().toJson(listOfModelClassObjects, type, fileWriter);
        }
    }


    /**
     * @param <T>           the type of the desired object
     * @param inputFilePath path from which the object is to be deserialized
     * @param desiredType   the specific genericized type of source.
     * @return List<T> list of the desired objects
     * @throws FileNotFoundException
     */
    public static <T> List<T> fromJson(String inputFilePath, Type desiredType) throws FileNotFoundException {
        if (new File(inputFilePath).isFile())
            return getGson().fromJson(new FileReader(inputFilePath), desiredType);
        return new ArrayList<>();
    }

    /**
     * @param <T>                     the 1st type of the desired object
     * @param listOfModelClassObjects a list of objects of model class
     */
    public static <T> void toJson(List<T> listOfModelClassObjects, String path, Type desiredType, RuntimeTypeAdapterFactory<?> adapter) throws IOException {

        try (FileWriter fileWriter = new FileWriter(path)) {
            getGson(adapter).toJson(listOfModelClassObjects, desiredType, fileWriter);
        }
    }


    /**
     * @param <T>           the type of the desired object
     * @param inputFilePath path from which the object is to be deserialized
     * @param desiredType   the specific genericized type of source.
     * @return List<T> list of the desired objects
     * @throws FileNotFoundException
     */
    public static <T> List<T> fromJson(String inputFilePath, Type desiredType, RuntimeTypeAdapterFactory<?> adapter) throws FileNotFoundException {
        if (new File(inputFilePath).isFile())
            return getGson(adapter).fromJson(new FileReader(inputFilePath), desiredType);
        return new ArrayList<>();
    }
}
