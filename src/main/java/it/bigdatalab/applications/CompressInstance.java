package it.bigdatalab.applications;
import it.bigdatalab.compression.GroupVarInt;
import it.bigdatalab.compression.DifferentialCompression;
import java.io.IOException;

public class CompressInstance {
    public CompressInstance() {
        GroupVarInt test = new GroupVarInt();
        DifferentialCompression diff = new DifferentialCompression();

        int[] array = new int[]{15, 34, 16777216, 16};
        byte [] compressi = test.EncodeGroup(array);

        for (int i=0;i<compressi.length; i++){

            System.out.println(compressi[i] );
        }
        System.out.println("____________");
        int[] lista = new int[]{1,2,3,4,5};
        byte [] listaCompressa = test.sequenceEncoding(lista);
        System.out.println("LISTA COMPRESSA");
        System.out.println("LUNGHEZZA "+ listaCompressa.length);
        for (int i=0;i<listaCompressa.length; i++){

            System.out.println(listaCompressa[i] );
        }

        int []enc = diff.encodeSortedSequence(lista);
        int []dec = diff.decodeSequence(enc);
        System.out.println("COMPRESSSA");
        for (int i = 0; i< enc.length;i++){
            System.out.println(enc[i]);
        }
        System.out.println("DECOMPRESSA");
        for (int i = 0; i< enc.length;i++){
            System.out.println(dec[i]);
        }

    }


    public static void main(String[] args) throws IOException {
        CompressInstance prova = new CompressInstance();


    }
}