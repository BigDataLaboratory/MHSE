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

    public static final Logger logger = LoggerFactory.getLogger("it.misebigdatalab.PropertiesManager");
    private static Properties prop = new Properties();

    static {
        try {
            initialise();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read properties file from etc folder
     * @throws IOException if file doesn't exist in the path specified
     */
    private static void initialise() throws IOException {
        String propertiesFilePath = "etc/mhse.properties";
        String absolutePath = new File(propertiesFilePath).getAbsolutePath();
        if(!new File(absolutePath).exists()){
            //TODO Add others default pahts
            throw new IOException("Configuration file " + absolutePath + " does not exist!");
        }

        try (InputStream input = new FileInputStream(propertiesFilePath)) {
            // load mhse properties file
            prop.load(input);
            logger.info("Properties loaded from file {}", absolutePath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get property value by property name
     * @param propertyName
     * @return property value
     */
    public static String getProperty(String propertyName) {
        if (prop.getProperty(propertyName) == null) {
            throw new InvalidParameterException(MessageFormat.format("Missing value for key {0}!", propertyName));
        }
        return prop.getProperty(propertyName);
    }

    /**
     * Get property value by property name, if it's empty or null return the default value passed as input
     *
     * @param propertyName
     * @param defaultValue
     * @return property value
     */
    public static String getProperty(String propertyName, final String defaultValue) {
        if (prop.getProperty(propertyName).isEmpty())
            return defaultValue;
        return prop.getProperty(propertyName, defaultValue);
    }

    /**
     * Get property value by property name, if empty throw new exception
     *
     * @param propertyName
     * @return property value
     */
    public static String getPropertyIfNotEmpty(String propertyName) {
        if (prop.getProperty(propertyName) == null || prop.getProperty(propertyName).isEmpty()) {
            throw new InvalidParameterException(MessageFormat.format("Missing value for key {0}!", propertyName));
        }
        return prop.getProperty(propertyName);
    }

    /**
     * Set a new property name and a linked property value
     * @param propertyName
     * @param propertyValue
     */
    public static void setProperty(String propertyName, String propertyValue){
        prop.setProperty(propertyName, propertyValue);
    }
}
