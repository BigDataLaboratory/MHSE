package it.bigdatalab.applications;

import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;

public class EdgeList2WebGraph {

    private String ofp;
    private String inputFilePath;
    private boolean fromJanusGraph;
    private Long2ObjectLinkedOpenHashMap<LongArrayList> normalizedEdgeList;

    private static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.applications.EdgeList2WebGraph");

    public EdgeList2WebGraph() {
        //Transform an edgelist file to webgraph formatted file, saving it to disk
        initialize();
    }

    private void initialize() {
        this.inputFilePath = PropertiesManager.getProperty("edgeList2WebGraph.inputEdgelistFilePath");
        this.ofp = PropertiesManager.getProperty("edgeList2WebGraph.outputFolderPath");
        this.fromJanusGraph = Boolean.parseBoolean(PropertiesManager.getProperty("edgeList2WebGraph.fromJanusGraph"));

        try {
            //create normalized edgelist for webgraph from edgelist file
            createNormalizedEdgelist();
            //remove any missing ID from normalized edgelist file to have a list of consecutive IDs
            removeMissingIDsFromNormalizedEdgelist();
            //write normalized edgelist to disk
            writeNormalizedEdgelist();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createNormalizedEdgelist() throws IOException {
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

    private void removeMissingIDsFromNormalizedEdgelist() {
        logger.info("Removing missing IDs from normalized edgelist...");
        long[] keys = normalizedEdgeList.keySet().toLongArray();
        LongArrays.quickSort(keys);
        Long2LongLinkedOpenHashMap newMapping = new Long2LongLinkedOpenHashMap(normalizedEdgeList.size());

        for(int i=0;i<keys.length;i++){
            //new mapping from IDs, from 0 to n-1
            long previousID = keys[i];
            //TODO In this moment we cast a long ID to int, due to array index that is an int
            //TODO To be optimized to give another long
            long newID = i;
            newMapping.put(previousID, newID);
        }

        String eol = System.getProperty("line.separator");

        try (Writer writer = new FileWriter("/media/dati1/dataset/hashtag/map_oldid_newid.csv")) {
            for (Map.Entry<Long, Long> entry : newMapping.long2LongEntrySet()) {
                writer.append(Long.toString(entry.getKey()))
                        .append(',')
                        .append(Long.toString(entry.getValue()))
                        .append(eol);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }

        //update normalizedEdgeList with the new IDs
        Long2ObjectLinkedOpenHashMap<LongArrayList> newNormalizedEdgeList = new Long2ObjectLinkedOpenHashMap<LongArrayList>();
        for(int i=0;i<keys.length;i++){
            long oldSourceID = keys[i];
            long newSourceID = newMapping.get(oldSourceID);
            LongArrayList oldAdjacencyList = normalizedEdgeList.get(oldSourceID);
            int adjSize = oldAdjacencyList.size();

            LongListIterator neighbourIterator = oldAdjacencyList.listIterator();
            LongArrayList newAdjacencyList = new LongArrayList(adjSize);
            while(neighbourIterator.hasNext()){
                long oldTargetID = neighbourIterator.nextLong();
                long newTargetID = newMapping.get(oldTargetID);
                newAdjacencyList.add(newTargetID);
            }
            newNormalizedEdgeList.put(newSourceID, newAdjacencyList);
        }
        normalizedEdgeList = newNormalizedEdgeList;
        logger.info("Missing IDs removed from normalized edgelist");
    }

    private void writeNormalizedEdgelist(){
        String inputDir = new File(inputFilePath).getParent();
        String graphName = new File(inputFilePath).getName();
        String normalizedFileName = graphName + "Normalized";
        String edgelistOutputFilePath = inputDir + File.separator + normalizedFileName;

        try (Writer writer = new FileWriter(edgelistOutputFilePath)) {
            String eol = System.getProperty("line.separator");
            logger.info("Sorting source IDs...");
            long[] keys = normalizedEdgeList.keySet().toLongArray();
            LongArrays.quickSort(keys);
            logger.info("Source IDs sorted");

            for(int i=0;i<keys.length;i++){
                long sourceID = keys[i];
                LongArrayList adjacencyList = normalizedEdgeList.get(sourceID);
                LongListIterator neighbourIterator = adjacencyList.listIterator();
                while(neighbourIterator.hasNext()){
                    long targetID = neighbourIterator.nextLong();
                    writer.append(Long.toString(sourceID))
                            .append("\t")
                            .append(Long.toString(targetID))
                            .append(eol);
                }
            }
            logger.info("Normalized edgelist file saved in {}", edgelistOutputFilePath);
            //update input file path with normalized file
            this.inputFilePath = inputDir + File.separator + normalizedFileName;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runAlgorithm() {
        //load of the graph through edgelist file
        ImmutableGraph graph = null;
        try {
            graph = ArcListASCIIGraph.load(inputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //store of the graph in webgraph format
        String graphName = new File(inputFilePath).getName();
        try {
            String outputFilePath = ofp + File.separator + graphName;
            BVGraph.store(graph,outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Nodes number: " + graph.numNodes() + ", edges number: " + graph.numArcs());
    }


    public static void main(String args[]) {
        EdgeList2WebGraph t = new EdgeList2WebGraph();
        t.runAlgorithm();
    }

}


