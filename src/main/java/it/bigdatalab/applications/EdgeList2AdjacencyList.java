package it.bigdatalab.applications;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class EdgeList2AdjacencyList {
    private String ofp;
    private String inputFilePath;
    private boolean fromJanusGraph;
    private Long2ObjectLinkedOpenHashMap<LongArrayList> normalizedEdgeList;
    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.EdgeList2AdjacencyList");

    public EdgeList2AdjacencyList(){
        initialize();

    }

    public void initialize(){

    }

    public void createNormalizedEdgelist() throws IOException {
        // construct a normalized edgelist with a mapping between previous ID and normalized ID
        normalizedEdgeList = new Long2ObjectLinkedOpenHashMap<LongArrayList>();
        BufferedReader br = null;
        logger.info("Starting edgelist normalization process");
        int currentLine = 0;

        try {
            br = new BufferedReader(new FileReader(inputFilePath));
            String sCurrentLine;

            //creating edgelist normalized
            while ((sCurrentLine = br.readLine()) != null) {
                //skip commented lines
                if(sCurrentLine.startsWith("#")){
                    continue;
                }

                if(currentLine % 10000 == 0){
                    logger.info("First " + currentLine + " rows read");
                }
                String[] sSplit = sCurrentLine.split("\\s+");

                long sourceID = Long.parseLong(sSplit[0]);
                long targetID = Long.parseLong(sSplit[1]);

                long normalizedSourceID;
                long normalizedTargetID;

                if(fromJanusGraph) {
                    logger.debug("It is specified that the edgelist file comes from JanusGraph");
                    logger.debug("The edgelist will be normalized (i.e. normalizedID = ID/4 -1) and ordered");
                    //JanusGraph IDs are always multiple of 4 and starts the numeration with ID 4,
                    //so we have to divide JanusGraph ID by 4 and subtract 1
                    //to have a numeration starting from ID 0
                    normalizedSourceID = sourceID / 4 - 1;
                    normalizedTargetID = targetID / 4 - 1;
                } else {
                    //if it's not the case of Janus Graph IDs, we do not make any change
                    normalizedSourceID = sourceID;
                    normalizedTargetID = targetID;
                }
                //Add normalizedSourceID, and normalizedTargetID in his adjacency list
                if(normalizedEdgeList.containsKey(normalizedSourceID)){
                    //normalizedSourceID existing
                    normalizedEdgeList.get(normalizedSourceID).add(normalizedTargetID);
                } else {
                    //normalizedSourceID not existing
                    LongArrayList adjacencyList = new LongArrayList();
                    adjacencyList.add(normalizedTargetID);
                    normalizedEdgeList.put(normalizedSourceID, adjacencyList);
                }

                //Add normalizedTargetID (if not present)
                if(!normalizedEdgeList.containsKey(normalizedTargetID)){
                    LongArrayList adjacencyList = new LongArrayList();
                    normalizedEdgeList.put(normalizedTargetID, adjacencyList);
                }
                currentLine++;
            }
        } finally {
            br.close();
        }
        logger.info("Edgelist normalization process completed");
        logger.info("Number of graph vertices: " + normalizedEdgeList.size());
        logger.info("Number of graph edges: " + currentLine);



    }

}
