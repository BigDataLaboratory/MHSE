package it.uniroma2.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesManager {
    public static final Logger logger = LoggerFactory.getLogger("it.uniroma2.PropertiesManager");
    private static Properties prop = new Properties();

    static {
        try {
            initialise();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initialise() throws IOException {
        String propertiesFilePath = "etc/mhse.properties";
        String absolutePath = new File(propertiesFilePath).getAbsolutePath();
        if(!new File(absolutePath).exists()){
            //TODO Aggiungere altri path di default
            throw new IOException("Il file di configurazione " + absolutePath + " non esiste!");
        }

        try (InputStream input = new FileInputStream(propertiesFilePath)) {
            // load mhse properties file
            prop.load(input);
            logger.info("Properties loaded from file {}", absolutePath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String propertyName){
        return prop.getProperty(propertyName);
    }

    public static void setProperty(String propertyName, String propertyValue){
        prop.setProperty(propertyName, propertyValue);
    }
}
