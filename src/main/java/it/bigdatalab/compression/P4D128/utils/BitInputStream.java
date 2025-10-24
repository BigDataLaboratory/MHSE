package GraphManagerDemo.compression.P4D128.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BitInputStream {
    private final InputStream in;
    private int currentByte;
    private int numBitsRemaining;

    public BitInputStream(InputStream in) {
        this.in = in;
        this.numBitsRemaining = 0;
    }

    public int readBits(int numBits) throws IOException {
        int result = 0;
        for (int i = 0; i < numBits; i++) {
            if (numBitsRemaining == 0) {
                currentByte = in.read();
                if (currentByte == -1) throw new EOFException();
                numBitsRemaining = 8;
            }
            numBitsRemaining--;
            int bit = (currentByte >>> numBitsRemaining) & 1;
            result = (result << 1) | bit;
        }
        return result;
    }

    public void flush() {
        numBitsRemaining = 0;  // Discard any remaining bits in current byte
    }
}
