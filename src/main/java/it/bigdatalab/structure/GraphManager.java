package it.bigdatalab.structure;

import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Preprocessing;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GraphManager {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.GraphManager");

    private CompressedGraph cGraph;
    private ImmutableGraph mGraph;
    private boolean  webGraph = false;
    private boolean compressedGraph = false;
    private boolean differentialCompression =  Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("graph.differentialCompression"));
    private boolean inMemory = true;
    private int [] nodes;
    private boolean isolatedVertices;

    public GraphManager(boolean WG, boolean CG,String inputFilePath, boolean transpose,boolean inM,boolean isoV,String direction) throws IOException {
        int i;

        if(WG){
            webGraph = true;
            inMemory = inM;
            isolatedVertices = isoV;
            ImmutableGraph graph = inMemory ?
                Transform.transpose(Transform.transpose(ImmutableGraph.load(inputFilePath))) :
                ImmutableGraph.load(inputFilePath);

            if (!isolatedVertices) {
                    graph = Preprocessing.removeIsolatedNodes(graph);
           }
            if (transpose) {
                if (direction.equals(Constants.IN_DIRECTION)) {
                    logger.info("Transposing graph cause direction is {}", direction);
                    graph = Transform.transpose(graph);
                    logger.debug("Transposing graph ended");
                }
            } else {
                    if (direction.equals(Constants.OUT_DIRECTION)) {
                        logger.info("Transposing graph cause direction is {}", direction);
                        graph = Transform.transpose(graph);
                        logger.debug("Transposing graph ended");
                    }
            }
            mGraph = graph;
            nodes = new int[mGraph.numNodes()];
            // populate the array of nodes
            NodeIterator nodeIter;
            nodeIter = mGraph.nodeIterator();
            i = 0;
            while(nodeIter.hasNext()) {
                nodes[i] = nodeIter.nextInt();
                i++;
            }



        }
        if(CG){
            compressedGraph = true;
            String[] SplitInputFilePath = inputFilePath.split("[.]");
            if(transpose){
                if (direction.equals(Constants.IN_DIRECTION)) {
                    logger.info("Loading the transposed compressed graph");
                    System.out.println(SplitInputFilePath[0] + "_transposed."+SplitInputFilePath[1]+".txt");
                    cGraph = new CompressedGraph(SplitInputFilePath[0] + "_transposed."+SplitInputFilePath[1]+".txt", SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + "_offset.txt", true);
                    logger.info("Transposed compressed graph loaded.");

                }else{
                    logger.info("Loading the compressed graph");
                    cGraph = new CompressedGraph(SplitInputFilePath[0] + "."+SplitInputFilePath[1]+".txt", SplitInputFilePath[0] + "." + SplitInputFilePath[1] + "_offset.txt", true);
                    logger.info("Loading completed");

                }

            }else {

                if (direction.equals(Constants.OUT_DIRECTION)) {
                    logger.info("Transposing graph cause direction is {}", direction);
                    cGraph = new CompressedGraph(SplitInputFilePath[0] + "_transposed."+SplitInputFilePath[1]+".txt", SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + "_offset.txt", true);
                    logger.debug("Transposing graph ended");
                }else{
                    logger.info("Loading the compressed graph");
                    cGraph = new CompressedGraph(SplitInputFilePath[0] + "."+SplitInputFilePath[1]+".txt", SplitInputFilePath[0] + "." + SplitInputFilePath[1] + "_offset.txt", true);
                    logger.info("Loading completed");

                }




            }
            nodes = cGraph.get_nodes();
        }

    }
    public int[] get_neighbours(int node){

        if(webGraph){


            return mGraph.successorArray(node);
        }

        return cGraph.get_neighbours(node,differentialCompression);
    }

    public int get_degree(int node){
        if(webGraph){
            return mGraph.outdegree(node);
        }
        return cGraph.get_neighbours(node,differentialCompression).length;
    }

    public int []get_nodes(){
        return nodes;
    }

    public int numNodes(){
        return nodes.length;
    }
    public long numArcs(){
        if(webGraph){
            return mGraph.numArcs();
        }
        return cGraph.numArcs();
    }

    public int[] successorArray(int node){
        if(!webGraph) {
            logger.error("Error you must define a Web Graph data structure");
            System.exit(-1);
        }
        return mGraph.successorArray(node);


    }

    public ImmutableGraph get_mGraph(){
        return mGraph;
    }
    public  CompressedGraph get_cGraph(){
        return cGraph;
    }

    public boolean isWebGraph(){
        return webGraph;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    public void set_compressed_graph(byte []cG){
        cGraph.set_compressed_graph(cG);
    }

    public void set_offset(int [][] off){
        cGraph.set_offset(off);
    }




}
