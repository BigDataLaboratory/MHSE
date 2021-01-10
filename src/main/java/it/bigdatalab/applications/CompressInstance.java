package it.bigdatalab.applications;
import it.bigdatalab.compression.GroupVarInt;

import java.io.IOException;

public class CompressInstance {
    public CompressInstance() {
        GroupVarInt test = new GroupVarInt();
        int[] array = new int[]{15, 34, 16777216, 16};
        byte [] compressi = test.EncodeGroup(array);

        for (int i=0;i<compressi.length; i++){

            System.out.println(compressi[i] );
        }
    }

    public static void main(String[] args) throws IOException {
        CompressInstance prova = new CompressInstance();

    }
}