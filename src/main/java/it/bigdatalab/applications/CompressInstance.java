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
        System.out.print("Lunghezza "+ compressi.length);
        System.out.println("\n ____________");
        int[] lista = new int[]{15,34,16777216,16,12,37,56,78,90,98,128,260,520,9999};
        //int[] lista = new int[]{520};
        byte [] listaCompressa = test.sequenceEncoding(lista);
        System.out.println("LISTA COMPRESSA Goup varint");
        System.out.println("LUNGHEZZA "+ listaCompressa.length);
        for (int i=0;i<listaCompressa.length; i++){

            System.out.println(listaCompressa[i] );
        }
        System.out.println("DECOMPRESSIONE Group varint");
        int [] decom = test.decode(listaCompressa);
        for (int i= 0; i<decom.length;i++){
            System.out.println(decom[i]);
        }
        //int []enc = diff.encodeSortedSequence(lista);
        int []enc = diff.encodeSequence(lista);

        int []dec = diff.decodeSequence(enc);
        System.out.println("--------------------------");

        System.out.println("DIFFERENTIAL COMPRESSION");

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