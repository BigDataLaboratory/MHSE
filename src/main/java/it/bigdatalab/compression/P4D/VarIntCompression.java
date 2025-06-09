package it.bigdatalab.compression.P4D;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VarIntCompression {
    public static void writeVarInt(OutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    public static int readVarInt(InputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        int b;
        do {
            b = in.read();
            if (b == -1) throw new EOFException();
            result |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return result;
    }
}
