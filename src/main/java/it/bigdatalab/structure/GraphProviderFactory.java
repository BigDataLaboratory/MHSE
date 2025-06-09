package it.bigdatalab.structure;

import java.io.IOException;

public class GraphProviderFactory {
    public static AbstractGraphProvider createGraphProvider(String inputPath, String format) throws IOException {

        if (format == null) {
            throw new IllegalArgumentException("Format must not be null");
        }

        /*
        return switch (format.toLowerCase()) {
            case "varintgb" -> new VarIntGBGraphProvider(inputPath, false);
            case "varintgb-diff" -> new VarIntGBGraphProvider(inputPath, true);
            case "eliasfano" -> new EliasFanoGraphProvider(inputPath);
            case "p4d" -> new P4DGraphProvider(inputPath);
            case "p4ds" -> new P4DSGraphProvider(inputPath);
            default -> throw new IllegalArgumentException("Unsupported graph format: " + format);
        };
         */

        return null;
    }
}
