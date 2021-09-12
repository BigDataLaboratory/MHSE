package it.bigdatalab.applications;

import it.bigdatalab.compression.GroupVarInt;
import it.bigdatalab.structure.CompressedGraph;
import it.bigdatalab.structure.GraphManager;
import it.bigdatalab.structure.UncompressedGraph;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

public class CompressInstance {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.CompressInstance");

    @Test
    void test_compressed_instance() throws IOException {
        String input="/home/antoniocruciani/IdeaProjects/MHSE/src/test/data/g_directed_compressed/";

        String [] namesDirected = {"32-cycle.adjlist","32-cycle_transposed.adjlist","32-path.adjlist","32-path_transposed.adjlist",
                "32in-star.adjlist","32in-star_transposed.adjlist","32out-star.adjlist","32out-star_transposed.adjlist","32t-path.adjlist"
                ,"32t-path_transposed.adjlist",};

        UncompressedGraph G;
        GraphManager cG;
        GroupVarInt compressor = new GroupVarInt();

        for(int i = 0;i< namesDirected.length;i++){
            G = new UncompressedGraph();
            G.load_graph(input+namesDirected[i],"\t");
            System.out.println("TESTING "+namesDirected[i]);

            cG = new GraphManager(false,true,input+namesDirected[i]+".txt",false,false,false,"in");
            int [][] uncG = G.getGraph();
            int [] edgeList;
            for(int k = 0;k<uncG.length;k++){
                edgeList = uncG[k];
                byte[] group_compression = compressor.listEncoding(edgeList);
                //System.out.println("LISTA ENCODED");
                int[] group_decompression = compressor.dec(group_compression);
                //System.out.println("---------------------------------------------------");
                for(int u=0;u< group_decompression.length;u++){
                   // System.out.println("DECOMPRESSED = "+group_decompression[u] + " ORIGINAL = "+uncG[k][u]);
                    if(group_decompression[u] != uncG[k][u]){
                        logger.error("ERRORE MISMATCH ");
                        System.exit(-1);
                    }
                }
            }



            logger.info("DECOMPRESSIONE VARINT TESTATA E FUNZIONANTE SULLE LISTE");

            logger.info("DECOMPRESSIONE DA FILE E NON COMPRESSO TEST");
            compressor.encodeAdjListFlat(uncG,false);
            System.out.println("LUNGHEZZA GRAFO COMPRESSO "+compressor.get_compressedAdjListFlat().length );
            //compressor.saveEncoding("/home/antoniocruciani/Desktop/monnezza/" ,"pippo.txt");
            //cG.set_compressed_graph(compressor.get_compressedAdjListFlat());
            //cG.set_offset(compressor.getOffset());
            //System.exit(-1);
            int [][] offs = cG.get_cGraph().get_offset();
            for(int y =0;y<offs.length;y++){
                int [] dec_neig = cG.get_neighbours(offs[y][0]);
                int [] neig = G.get_neighbours(offs[y][0]);
                System.out.println(" LUNG COMP "+dec_neig.length+ "  LUNG UNC "+neig.length);
                for(int x=0;x<dec_neig.length;x++){
                    if(dec_neig[x]!= neig[x]){
                        logger.error("MISMATCH decoded {}  not encoded {}",dec_neig[y],neig[x]);
                        System.exit(-6);
                    }
                }


            }

            logger.info("DECOMPRESSIONE DA FILE E NON COMPRESSO PASSATA");

            logger.info("TEST FUNZIONE COMPRESSIONE ADJ LIST");
            compressor.encodeAdjListFlat(uncG,false);
            logger.info("TEST OFFSET ");
            int [][] compOFSS = compressor.getOffset();

            System.out.println("LEN OFF COMPUTED "+ compOFSS.length+ " LEN LOADED OFFS "+cG.get_cGraph().get_offset().length);
            for(int y =0;y<compOFSS.length;y++){
                if(compOFSS[y][0] != cG.get_cGraph().get_offset()[y][0]){
                    logger.error("ERRORE MISMATCH NODI");
                    System.exit(-1);
                }
                if(compOFSS[y][1] != cG.get_cGraph().get_offset()[y][1]){
                    logger.error("ERRORE MISMATCH BYTES");
                    logger.error("INDEX {} COMPUTED {} LOADED {}",y,compOFSS[y][1],cG.get_cGraph().get_offset()[y][1]);
                    System.exit(-1);
                }
            }
            logger.info("OFFSET TEST PASSED");
            logger.info("TEST COMPRESSED GRAPH");
            // ERRORE NEL LOADING DEL COMPRESSED GRAPH CARICHI I BYTE PIU' GRANDI DI COME LI HAI DEFINITI
            byte [] compCG = compressor.get_compressedAdjListFlat();
            System.out.println("LEN COMPRESSED GRAPH COMPUTED "+ compCG.length+ " LEN COMPRESSED GRAPH  LOADED "+cG.get_cGraph().getCompressed_graph().length);
            for(int y =0;y<compCG.length;y++) {
                if (compCG[y] != cG.get_cGraph().getCompressed_graph()[y]) {
                    logger.error("ERRORE MISMATCH COMPRESSIONE GRAFO");
                    logger.error("INDEX {} COMPUTED {} LOADED {}", y, compCG[y], cG.get_cGraph().getCompressed_graph()[y]);
                    System.exit(-1);
                }
            }
            logger.info("TEST COMPRESSED GRAPH PASSEDs");
            //cG.set_compressed_graph(compressor.get_compressedAdjListFlat());
            //cG.set_offset(compressor.getOffset());
            // L'ERRORE PROBABILMENTE STA NELLA SCRITTURA DELLA MATRICE DI ADJ COMPRESSA E DELL'OFFSET
            logger.info("TESTING THE GET NEIGH FUNCTION");
           // System.out.println("MAX OFFSET "+cG.get_cGraph().getOffset()[cG.get_cGraph().getOffset().length-1][1]);
            //System.out.println("Lunghezza codifica "+ cG.get_cGraph().getCompressed_graph().length);
            // CONTROLLA LA FUNZIONE NEIGH C'E' qualcosa che non funziona
            //System.out.println(cG.get_cGraph().get_offset());
            for(int o = 0;o< uncG.length;o++){
                edgeList = uncG[o];

                int [] decomPedgeList = cG.get_neighbours(uncG[o][0]);
//                System.out.println("LISTA DECOMPRESSA");
//                for(int y =0;y<decomPedgeList.length;y++){
//                    System.out.println(decomPedgeList[y]);
//                }
//                System.out.println("LISTA ORIGINALE");
//
//                for(int y =0;y<uncG[o].length;y++){
//                    System.out.println(uncG[o][y]);
//                }
                //System.out.println("------------------------");
                //System.out.println("LUNG EL "+edgeList.length + " LUNG decEL "+decomPedgeList.length);
                for(int p = 0;p<decomPedgeList.length;p++){
                    if(edgeList[p+1]!= decomPedgeList[p]){
                        logger.error("ERRORE MISMATCH ");
                        System.exit(-1);
                    }
                }

            }
            logger.info("GET NEIGH TESTATA E FUNZIONANTE SULLE LISTE");

//
//
//            logger.info("NUMBER OF NODES {}",uncG.length);
//            CompressedGraph cGraph = cG.get_cGraph();
//            int off[][] = cGraph.getOffset();
//            logger.info("NUMBER OF NODES COMPRESSED INSTANCE {}",off.length);
//            logger.info("TESTING THE COMPRESSION SCHEME ");
//            byte [][]  compAdj = new byte[off.length][];
//            byte [] comSerG = cGraph.getCompressed_graph();
//            int by = 0;
//            int g;
//            for(int k = 0;k<off.length;k++){
//                byte[] app ;
//                g = 0;
//                if(off[k][0] != off[off.length -1][0]) {
//                    if (k != 0) {
//                        app = new byte [off[k+1][1]-off[k][1]+1];
//
//                        while (by < off[k+1][1]) {
//                            System.out.println(by + "   "+comSerG.length + " "+g + " "+off[k+1][1]+ " "+off[k][1]);
//
//                            app[g] = comSerG[by];
//                            by += 1;
//                            g += 1;
//                        }
//
//                    }else {
//                        app = new byte[off[k][1]];
//
//                        while (by < off[k][1]) {
//                            app[g] = comSerG[by];
//                            by += 1;
//                            g += 1;
//                        }
//                    }
//                }else{
//                    app = new byte [comSerG.length-off[k][1]];
//                    int dif = off[k][1];
//                    while(by<comSerG.length){
//                        System.out.println(by + "   "+comSerG.length + " "+g + " "+ dif);
//                        app[g] = comSerG[by];
//                        g+=1;
//                        by+=1;
//                    }
//                }
//                compAdj[k] = app;
//            }
//            compressor.decodeAdjList(compAdj);
//            for(int k = 0;k< uncG.length;k++){
//                for (int t = 0;t< uncG[k].length;t++){
//                    System.out.println(uncG[k][t]);
//                }
//            }
//
//            for (int j= 0; j<uncG.length;j++){
//                int node = uncG[j][0];
//                logger.info("NODE {}",node);
//                int [] neigUnc = G.get_neighbours(node);
//                logger.info("LUNG NEIGS UNC {}",neigUnc.length);
//
//                int [] neigComp = cG.get_neighbours(node);
//
//                for (int k=0;k<neigUnc.length;k++){
//                    if(neigComp[k] != neigUnc[k]){
//                        logger.error("DATASET: {}",namesDirected[i]);
//                        logger.error("ERROR MISMATCH BETWEEN uncompressed and compressed");
//                        logger.error("Uncompressed {}",neigUnc[k]);
//                        logger.error("Compressed {}",neigComp[k]);
//                    }
//                }
//            }
        }


    }


}
