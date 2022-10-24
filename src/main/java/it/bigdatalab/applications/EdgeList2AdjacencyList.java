package it.bigdatalab.applications;

import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;

public class EdgeList2AdjacencyList {
    private String ofp;
    private String inputFilePath;
    private boolean fromJanusGraph;
    private boolean degreeDistributionLabeling;
    //private Long2ObjectLinkedOpenHashMap<LongArrayList> normalizedEdgeList;
    private Long2ObjectLinkedOpenHashMap<LongArrayList> normalizedAdjList;
    //private HashMap<String, String> properties = new HashMap<String, String>();
    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.EdgeList2AdjacencyList");

    public EdgeList2AdjacencyList(boolean transpose){
        initialize(transpose);

    }

    public void initialize(boolean transpose){
       /*
        edgeList2AdjacencyList.inputEdgelistFilePath = /path/to/input/edgelist
        edgeList2AdjacencyList.outputFolderPath = /path/to/output/folder/adjList
        edgeList2AdjacencyList.fromJanusGraph = False

        */
        this.inputFilePath = PropertiesManager.getProperty("edgeList2AdjacencyList.inputEdgelistFilePath");
        this.ofp = PropertiesManager.getProperty("edgeList2AdjacencyList.outputFolderPath");
        this.fromJanusGraph = Boolean.parseBoolean(PropertiesManager.getProperty("edgeList2AdjacencyList.fromJanusGraph"));
        this.degreeDistributionLabeling = Boolean.parseBoolean(PropertiesManager.getProperty("edgeList2AdjacencyList.degreeDistributionLabeling"));

        try {
            //create normalized edgelist for webgraph from edgelist file

            createNormalizedAdjlist(transpose);
            //write normalized edgelist to disk
            writeNormalizedAdjList(transpose);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void createNormalizedAdjlist(boolean transpose) throws IOException {
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
                String[] sSplit = sCurrentLine.split("\t");
                long sourceID = -1;
                long targetID = -1;
                if(transpose){
                     sourceID = Long.parseLong(sSplit[1]);
                     targetID = Long.parseLong(sSplit[0]);
                }else {
                     sourceID = Long.parseLong(sSplit[0]);
                     targetID = Long.parseLong(sSplit[1]);
                }
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

                    //System.out.println("SOURCE = "+normalizedSourceID +" TARGET = "+normalizedTargetID);
                    //System.out.println(normalizedAdjList.size());
                    //System.out.println("AUHAHUAHUAHU");
                    //System.exit(1);
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



    private void writeNormalizedAdjList(boolean transposed) throws IOException {
        String inputDir = new File(inputFilePath).getParent();
        String graphName = new File(inputFilePath).getName();
        String normalizedFileName = "";
        if(transposed) {
             normalizedFileName = graphName.split(".edgelist")[0] + "_transposed.adjlist";
        }else{
             normalizedFileName = graphName.split(".edgelist")[0] + ".adjlist";

        }
        String adjListOutputFilePath = inputDir + File.separator + normalizedFileName;

        try (Writer writer = new FileWriter(adjListOutputFilePath)) {
            String eol = System.getProperty("line.separator");
            logger.info("Sorting source IDs...");
            long[] keys = normalizedAdjList.keySet().toLongArray();
            LongArrays.quickSort(keys);
            logger.info("Source IDs sorted");
            if (degreeDistributionLabeling) {
                Long2ObjectLinkedOpenHashMap<Long> degreeDistribution = new  Long2ObjectLinkedOpenHashMap<Long>();
                for (int i = 0; i < keys.length; i++) {
                    Long sourceID = keys[i];
                    int listSize = normalizedAdjList.get(sourceID).size();
                    degreeDistribution.put(listSize,sourceID);
                }
                long[] keys_degree_distribution =  degreeDistribution.keySet().toLongArray();
                LongArrays.quickSort(keys_degree_distribution);
                logger.info("Sorting nodes by degree distribution");
                Integer k= 0;
                Long2ObjectLinkedOpenHashMap<Integer> newLables = new  Long2ObjectLinkedOpenHashMap<Integer>();
                for(int i = 0; i < keys_degree_distribution.length; i++) {
                    newLables.put(keys_degree_distribution[i],k);
                    k++;
                }
                long[] relabeled_keys = new long[keys.length];
                for(int j = 0;j<keys.length;k++){
                    relabeled_keys[j] = newLables.get(keys[j]);
                }
                LongArrays.quickSort(relabeled_keys);
                for (int i = 0; i < relabeled_keys.length; i++) {
                    long sourceID = relabeled_keys[i];
                    LongArrayList adjacencyList = normalizedAdjList.get(sourceID);
                    LongArrayList relabeledAdjacencyList = new LongArrayList();
                    for(int j =0 ;j<adjacencyList.size();j++){
                        relabeledAdjacencyList.add(newLables.get(adjacencyList.get(j)));
                    }
                    Collections.sort(relabeledAdjacencyList);
                    writer.append(Long.toString(sourceID)).append("\t");
                    for (int j = 0; j < adjacencyList.size(); j++) {
                        long targetID = adjacencyList.getLong(j);
                        writer.append(Long.toString(newLables.get(targetID))).append("\t");

                    }
                    writer.append(eol);
                }
                logger.info("Graph relabeled");



            } else {



                for (int i = 0; i < keys.length; i++) {
                    long sourceID = keys[i];
                    LongArrayList adjacencyList = normalizedAdjList.get(sourceID);
                    //if(adjacencyList.size() != 0){


                    Collections.sort(adjacencyList);
                    writer.append(Long.toString(sourceID)).append("\t");

                    for (int j = 0; j < adjacencyList.size(); j++) {
                        long targetID = adjacencyList.getLong(j);
                        writer.append(Long.toString(targetID)).append("\t");

                    }
                    writer.append(eol);
                    //}
                }
            }
            logger.info("Normalized adj file saved in {}", adjListOutputFilePath);

            this.inputFilePath = inputDir + File.separator + normalizedFileName;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public static void main(String args[]) {
        EdgeList2AdjacencyList t = new EdgeList2AdjacencyList(false);
        //EdgeList2AdjacencyList t_trans = new EdgeList2AdjacencyList(true);

    }
}