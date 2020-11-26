package it.bigdatalab.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.text.MessageFormat;
import java.util.Properties;

public class PropertiesManager {

    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.utils.PropertiesManager");

    private static final Properties sProp = new Properties();

    static {
        try {
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read properties file from etc folder
     *
     * @throws IOException if file doesn't exist in the path specified
     */
    private static void load() throws IOException {
        String absolutePath = new File(Constants.DEFAULT_PROPERTIES_PATH).getAbsolutePath();

        if (!new File(absolutePath).exists()) {
            throw new IOException("Configuration file " + absolutePath + " does not exist!");
        }

        InputStream input = new FileInputStream(absolutePath);
        // load mhse properties file
        sProp.load(input);
        logger.info("Properties loaded from file {}", absolutePath);
    }

    /**
     * Get property value by property name
     *
     * @param propertyName
     * @return property value
     */
    public static String getProperty(String propertyName) {
        if (sProp.getProperty(propertyName) == null) {
            throw new InvalidParameterException(MessageFormat.format("Missing value for key {0}!", propertyName));
        }
        return sProp.getProperty(propertyName);
    }

    /**
     * Get property value by property name, if it's empty or null return the default value passed as input
     *
     * @param propertyName
     * @param defaultValue
     * @return property value
     */
    public static String getProperty(String propertyName, final String defaultValue) {
        if (sProp.getProperty(propertyName) == null || sProp.getProperty(propertyName).isEmpty())
            return defaultValue;
        return sProp.getProperty(propertyName, defaultValue);
    }

    /**
     * Get property value by property name, if empty throw new exception
     *
     * @param propertyName
     * @return property value
     */
    public static String getPropertyIfNotEmpty(String propertyName) {
        if (sProp.getProperty(propertyName) == null || sProp.getProperty(propertyName).isEmpty()) {
            throw new InvalidParameterException(MessageFormat.format("Missing value for key {0}!", propertyName));
        }
        return sProp.getProperty(propertyName);
    }

}
