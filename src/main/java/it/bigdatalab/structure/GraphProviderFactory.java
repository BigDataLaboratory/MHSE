package it.bigdatalab.structure;

import it.bigdatalab.structure.GraphProviders.EliasFanoGraphProvider;
import it.bigdatalab.structure.GraphProviders.P4D256GraphProvider;
import it.bigdatalab.structure.GraphProviders.P4DGraphProvider;
import it.bigdatalab.structure.GraphProviders.VarIntGBGraphProvider;

import java.io.IOException;

public class GraphProviderFactory {
    public static AbstractGraphProvider createGraphProvider(String inputPath, String format) throws IOException {

        if (format == null) {
            throw new IllegalArgumentException("Format must not be null");
        }

        switch (format.toLowerCase()) {
            case "varintgb":
                return new VarIntGBGraphProvider(inputPath, false);
            case "varintgb-diff":
                return new VarIntGBGraphProvider(inputPath, true);
            case "eliasfano":
                return new EliasFanoGraphProvider(inputPath);
            case "p4d":
                return new P4DGraphProvider(inputPath);
            case "p4d256":
                P4D256GraphProvider g = new P4D256GraphProvider();
                g.loadCompressedGraph(inputPath);
                return g;
            default:
                throw new IllegalArgumentException("Unsupported graph format: " + format);
        }
    }

}
