package it.bigdatalab.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesManagerTest {

    @Test
    void testGetProperty_existingProperty() {
        String inputFilePath = PropertiesManager.getProperty("minhash.inputFilePath");
        String expected = "/test/path/to/input/graph";
        assertEquals(expected, inputFilePath);
    }

    @Test
    void testGetProperty_throwsException() {
        Assertions.assertThrows(InvalidParameterException.class, () -> {
            PropertiesManager.getProperty("inputFilePath");
        });
    }

    @Test
    void testGetProperty_withDefaultValueAndNonExistentProperty() {
        String inputFilePath = PropertiesManager.getProperty("inputFilePath", "/test/path/to/input/graph");
        String expected = "/test/path/to/input/graph";
        assertEquals(expected, inputFilePath);
    }

    @Test
    void testGetProperty_withDefaultValueAndExistentProperty() {
        String inputFilePath = PropertiesManager.getProperty("minhash.inputFilePath", "/test/path/existent");
        String expected = "/test/path/to/input/graph";
        assertEquals(expected, inputFilePath);
    }

    @Test
    void testGetPropertyIfNotEmpty_throwsException_withNonExistentProperty() {
        Assertions.assertThrows(InvalidParameterException.class, () -> {
            PropertiesManager.getPropertyIfNotEmpty("inputFilePath");
        });
    }

    @Test
    void testGetPropertyIfNotEmptyThrowsException_withExistentEmptyProperty() {
        Assertions.assertThrows(InvalidParameterException.class, () -> {
            PropertiesManager.getPropertyIfNotEmpty("minhash.nodeIDRange");
        });
    }

    @Test
    void testGetPropertyIfNotEmptyThrowsException_withExistentProperty() {
        String inputFilePath = PropertiesManager.getProperty("inputFilePath", "/test/path/to/input/graph");
        String expected = "/test/path/to/input/graph";
        assertEquals(expected, inputFilePath);
    }
}