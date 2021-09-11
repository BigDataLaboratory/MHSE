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

            cG = new GraphManager(false,true,input+namesDirected[i],false,false,false,"in");
            int [][] uncG = G.getGraph();
            int [] edgeList;
            for(int k = 0;k<uncG.length;k++){
                edgeList = uncG[k];
                byte[] group_compression = compressor.listEncoding(edgeList);
                //System.out.println("LISTA ENCODED");
                int[] group_decompression = compressor.dec(group_compression);

                for(int u=0;u< group_decompression.length;u++){
                    if(group_decompression[u] != uncG[k][u]){
                        logger.error("ERRORE MISMATCH ");
                        System.exit(-1);
                    }
                }
            }
            logger.info("DECOMPRESSIONE VARINT TESTATA E FUNZIONANTE SULLE LISTE");
            // L'ERRORE PROBABILMENTE STA NELLA SCRITTURA DELLA MATRICE DI ADJ COMPRESSA E DELL'OFFSET
            logger.info("TESTING THE GET NEIGH FUNCTION");
            for(int o = 0;o< uncG.length;o++){
                edgeList = uncG[o];

                int [] decomPedgeList = cG.get_neighbours(uncG[o][0]);
                System.out.println("LUNG EL "+edgeList.length + " LUNG decEL "+decomPedgeList.length);
                for(int p = 0;p<decomPedgeList.length;p++){
                    if(edgeList[p+1]!= decomPedgeList[p]){
                        logger.error("ERRORE MISMATCH ");
                        System.exit(-1);
                    }
                }

            }
            logger.info("GET NEIGH TESTATA E FUNZIONANTE SULLE LISTE");



            logger.info("NUMBER OF NODES {}",uncG.length);
            CompressedGraph cGraph = cG.get_cGraph();
            int off[][] = cGraph.getOffset();
            logger.info("NUMBER OF NODES COMPRESSED INSTANCE {}",off.length);
            logger.info("TESTING THE COMPRESSION SCHEME ");
            byte [][]  compAdj = new byte[off.length][];
            byte [] comSerG = cGraph.getCompressed_graph();
            int by = 0;
            int g;
            for(int k = 0;k<off.length;k++){
                byte[] app ;
                g = 0;
                if(off[k][0] != off[off.length -1][0]) {
                    if (k != 0) {
                        app = new byte [off[k+1][1]-off[k][1]+1];

                        while (by < off[k+1][1]) {
                            System.out.println(by + "   "+comSerG.length + " "+g + " "+off[k+1][1]+ " "+off[k][1]);

                            app[g] = comSerG[by];
                            by += 1;
                            g += 1;
                        }

                    }else {
                        app = new byte[off[k][1]];

                        while (by < off[k][1]) {
                            app[g] = comSerG[by];
                            by += 1;
                            g += 1;
                        }
                    }
                }else{
                    app = new byte [comSerG.length-off[k][1]];
                    int dif = off[k][1];
                    while(by<comSerG.length){
                        System.out.println(by + "   "+comSerG.length + " "+g + " "+ dif);
                        app[g] = comSerG[by];
                        g+=1;
                        by+=1;
                    }
                }
                compAdj[k] = app;
            }
            compressor.decodeAdjList(compAdj);
            for(int k = 0;k< uncG.length;k++){
                for (int t = 0;t< uncG[k].length;t++){
                    System.out.println(uncG[k][t]);
                }
            }

            for (int j= 0; j<uncG.length;j++){
                int node = uncG[j][0];
                logger.info("NODE {}",node);
                int [] neigUnc = G.get_neighbours(node);
                logger.info("LUNG NEIGS UNC {}",neigUnc.length);

                int [] neigComp = cG.get_neighbours(node);

                for (int k=0;k<neigUnc.length;k++){
                    if(neigComp[k] != neigUnc[k]){
                        logger.error("DATASET: {}",namesDirected[i]);
                        logger.error("ERROR MISMATCH BETWEEN uncompressed and compressed");
                        logger.error("Uncompressed {}",neigUnc[k]);
                        logger.error("Compressed {}",neigComp[k]);
                    }
                }
            }
        }


    }


}
