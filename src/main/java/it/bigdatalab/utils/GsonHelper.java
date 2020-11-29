package it.bigdatalab.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class GsonHelper {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.utils.GsonHelper");

    private static Gson sGson = new GsonBuilder().create();

    private GsonHelper() {
    }

    /**
     * @param <T>              the 1st type of the desired object
     * @param modelClassObject an object of model class
     */
    public static <T> void toJson(T modelClassObject, String path) throws IOException {

        Type type = new TypeToken<T>() {
        }.getType();

        try (FileWriter fileWriter = new FileWriter(path)) {
            sGson.toJson(modelClassObject, type, fileWriter);
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
            sGson.toJson(listOfModelClassObjects, type, fileWriter);
        }
    }

    @SafeVarargs
    public static <T> void toJson(List<T> listOfModelClassObjects, String path, Class<T> supertype, Class<T>... subtypes) throws IOException {
        if (supertype == null) return;

        RuntimeTypeAdapterFactory<T> typeAdapterFactory = RuntimeTypeAdapterFactory.of(supertype, "type");
        for (Class<T> subtype : subtypes) typeAdapterFactory.registerSubtype(subtype, subtype.getName());

        Type type = new TypeToken<List<T>>() {
        }.getType();
        sGson = new GsonBuilder().registerTypeAdapterFactory(typeAdapterFactory).create();

        try (FileWriter fileWriter = new FileWriter(path)) {
            sGson.toJson(listOfModelClassObjects, type, fileWriter);
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
        return sGson.fromJson(new FileReader(inputFilePath), desiredType);
    }
}
