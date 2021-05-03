package it.bigdatalab.applications;

import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Collections;

public class EdgeList2AdjacencyList {
    private String ofp;
    private String inputFilePath;
    private boolean fromJanusGraph;
    //private Long2ObjectLinkedOpenHashMap<LongArrayList> normalizedEdgeList;
    private Long2ObjectLinkedOpenHashMap<LongArrayList> normalizedAdjList;
    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.EdgeList2AdjacencyList");

    public EdgeList2AdjacencyList(){
        initialize();

    }

    public void initialize(){
       /*
        edgeList2AdiacencyList.inputEdgelistFilePath = /path/to/input/edgelist
        edgeList2AdiacencyList.outputFolderPath = /path/to/output/folder/adjList
        edgeList2AdiacencyList.fromJanusGraph = False

        */
        this.inputFilePath = PropertiesManager.getProperty("edgeList2AdiacencyList.inputEdgelistFilePath");
        this.ofp = PropertiesManager.getProperty("edgeList2AdiacencyList.outputFolderPath");
        this.fromJanusGraph = Boolean.parseBoolean(PropertiesManager.getProperty("edgeList2AdiacencyList.fromJanusGraph"));

        try {
            //create normalized edgelist for webgraph from edgelist file
            createNormalizedAdjlist();
            //write normalized edgelist to disk
            writeNormalizedAdjList();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createNormalizedAdjlist() throws IOException {
        // construct a normalized edgelist with a mapping between previous ID and normalized ID
        normalizedAdjList = new Long2ObjectLinkedOpenHashMap<LongArrayList>();
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
                if(normalizedAdjList.containsKey(normalizedSourceID)){
                    //normalizedSourceID existing
                    normalizedAdjList.get(normalizedSourceID).add(normalizedTargetID);
                } else {
                    //normalizedSourceID not existing
                    LongArrayList adjacencyList = new LongArrayList();
                    adjacencyList.add(normalizedTargetID);
                    normalizedAdjList.put(normalizedSourceID, adjacencyList);
                }

                //Add normalizedTargetID (if not present)
                if(!normalizedAdjList.containsKey(normalizedTargetID)){
                    LongArrayList adjacencyList = new LongArrayList();
                    normalizedAdjList.put(normalizedTargetID, adjacencyList);
                }
                currentLine++;
            }
        } finally {
            br.close();
        }
        logger.info("AdjList normalization process completed");
        logger.info("Number of graph vertices: " + normalizedAdjList.size());
        logger.info("Number of graph edges: " + currentLine);



    }



    private void writeNormalizedAdjList() throws IOException {
        String inputDir = new File(inputFilePath).getParent();
        String graphName = new File(inputFilePath).getName();
        String normalizedFileName = graphName + "Normalized";
        String adjListOutputFilePath = inputDir + File.separator + normalizedFileName;

        try (Writer writer = new FileWriter(adjListOutputFilePath)) {
            String eol = System.getProperty("line.separator");
            logger.info("Sorting source IDs...");
            long[] keys = normalizedAdjList.keySet().toLongArray();
            LongArrays.quickSort(keys);
            logger.info("Source IDs sorted");
            for(int i=0;i<keys.length;i++){
                long sourceID = keys[i];
                LongArrayList adjacencyList = normalizedAdjList.get(sourceID);
                Collections.sort(adjacencyList);
                writer.append(Long.toString(sourceID)).append("\t");
                for(int j = 0; j<adjacencyList.size();j++){
                    long targetID = adjacencyList.getLong(j);
                    writer.append(Long.toString(targetID));
                }
                writer.append(eol);
            }
            logger.info("Normalized adj file saved in {}", adjListOutputFilePath);

            this.inputFilePath = inputDir + File.separator + normalizedFileName;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public static void main(String args[]) {
        EdgeList2AdjacencyList t = new EdgeList2AdjacencyList();
    }
}