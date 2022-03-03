package it.bigdatalab.structure;

import com.google.common.graph.Graph;
import it.bigdatalab.compression.GroupVarInt;
import it.bigdatalab.utils.Constants;
import it.bigdatalab.utils.Preprocessing;
import it.bigdatalab.utils.PropertiesManager;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

/**
 * Implementation of the new data structure
 *
 * @author Giambattista Amati
 * @author Simone Angelini
 * @author Antonio Cruciani
 * @author Daniele Pasquini
 * @author Paola Vocca
 */
public class GraphManager {
    public static final Logger logger = LoggerFactory.getLogger("it.bigdatalab.structure.GraphManager");

    private CompressedGraph cGraph;
    private CompressedEliasFanoGraph eGraph;
    private ImmutableGraph mGraph;
    private boolean  webGraph = false;
    private boolean compressedGraph = false;
    private boolean EG = false;
    private boolean differentialCompression =  Boolean.parseBoolean(PropertiesManager.getPropertyIfNotEmpty("graph.differentialCompression"));
    private boolean inMemory = true;
    private int [] nodes;
    private boolean isolatedVertices;
    private boolean transposed;

    /**
     * Define a new instance of the Graph Manager loading the desired graph type
     * @param WG Boolean is webgraph?
     * @param CG Boolean is compressed graph?
     * @param inputFilePath String input path of the instance
     * @param transpose Boolean is transposed?
     * @param inM Boolean in memory ? (Only for webgraph)
     * @param isoV Boolean isolated nodes? (Preprocessing for webgraph)
     * @param direction String direction of the message passing
     * @throws IOException
     */
    public GraphManager(boolean WG, boolean CG,String inputFilePath, boolean transpose,boolean inM,boolean isoV,String direction,boolean EliasGamma) throws IOException {
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
            transposed = transpose;
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
            EG = EliasGamma;

            compressedGraph = true;
            String[] SplitInputFilePath = inputFilePath.split("[.]");
            transposed = transpose;
            if(transpose){
                if (direction.equals(Constants.IN_DIRECTION)) {
                    logger.info("Loading the transposed compressed graph");
                    System.out.println(SplitInputFilePath[0] + "_transposed."+SplitInputFilePath[1]+".txt");
                    if(!EG) {
                        logger.info("Group Varint");

                        cGraph = new CompressedGraph(SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + ".txt", SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + "_offset.txt", true);
                    }else{
                        logger.info("Elias Fano ");

                        eGraph = new CompressedEliasFanoGraph(SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + "_elias_.txt", SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + "_offset_elias.txt", true);
                        System.out.println("TRANSPOSED ");

                    }
                    logger.info("Transposed compressed graph loaded.");

                }else{
                    logger.info("Loading the compressed graph");
                    if(!EG) {
                        logger.info("Group Varint");

                        cGraph = new CompressedGraph(SplitInputFilePath[0] + "." + SplitInputFilePath[1] + ".txt", SplitInputFilePath[0] + "." + SplitInputFilePath[1] + "_offset.txt", true);
                    }else {
                        logger.info("Elias Fano ");

                        eGraph = new CompressedEliasFanoGraph(SplitInputFilePath[0] + "."+SplitInputFilePath[1]+"_elias_.txt", SplitInputFilePath[0] + "." + SplitInputFilePath[1] + "_offset_elias.txt", true);

                    }
                        logger.info("Loading completed");


                }

            }else {

                if (direction.equals(Constants.OUT_DIRECTION)) {
                    logger.info("Transposing graph cause direction is {}", direction);
                    if(!EG) {
                        logger.info("Group Varint");

                        cGraph = new CompressedGraph(SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + ".txt", SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + "_offset.txt", true);
                    }else{
                        logger.info("Elias Fano");

                        eGraph = new CompressedEliasFanoGraph(SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + "_elias_.txt", SplitInputFilePath[0] + "_transposed." + SplitInputFilePath[1] + "_offset_elias.txt", true);
                        System.out.println("TRANSPOSED ");

                    }
                    logger.debug("Transposing graph ended");
                }else{
                    logger.info("Loading the compressed graph");
                    if(!EG) {
                        logger.info("Group Varint");

                        cGraph = new CompressedGraph(SplitInputFilePath[0] + "." + SplitInputFilePath[1] + ".txt", SplitInputFilePath[0] + "." + SplitInputFilePath[1] + "_offset.txt", true);
                    }
                    else{
                        logger.info("Elias Fano ");

                        eGraph = new CompressedEliasFanoGraph(SplitInputFilePath[0] + "." + SplitInputFilePath[1] + "_elias_.txt", SplitInputFilePath[0] + "." + SplitInputFilePath[1] + "_offset_elias.txt", true);
                    }
                    logger.info("Loading completed");

                }




            }
            if(!EliasGamma) {
                nodes = cGraph.get_nodes();
            }else {
                nodes = eGraph.get_nodes();

            }
        }

    }


    /**
     * Returns the neighbours of a node
     * @param node Int node
     * @return Int array neighbours
     */
    public int[] get_neighbours(int node){

        if(webGraph){


            return mGraph.successorArray(node);
        }
        if(!EG){
            return cGraph.get_neighbours(node,differentialCompression);

        }
        return eGraph.get_neighbours(node);

    }

    /**
     * Returns the number of neighbours of a node
     * @param node Int ndoe
     * @return Int number of neighbours
     */
    public int get_degree(int node){
        if(webGraph){
            return mGraph.outdegree(node);
        }
        if(!EG) {
            return cGraph.get_neighbours(node, differentialCompression).length;
        }
        return eGraph.get_neighbours(node).length;

    }

    /**
     * Returns the nodes of the graph
     * @return Int array of nodes
     */
    public int []get_nodes(){
        return nodes;
    }

    /**
     * Returns the number of nodes
     * @return Int number of nodes
     */
    public int numNodes(){
        return nodes.length;
    }

    /**
     * Returns the number of arcs
     * @return Long number of arcs
     */
    public long numArcs(){
        if(webGraph){
            return mGraph.numArcs();
        }
        if(!EG) {
            return cGraph.numArcs();
        }
        return eGraph.numArcs();

    }

    /**
     * Returns the successor array (same as get neighbours)
     * @param node Int node
     * @return Int array neighbours
     */
    public int[] successorArray(int node){
        if(!webGraph) {
            logger.error("Error you must define a Web Graph data structure");
            System.exit(-1);
        }
        return mGraph.successorArray(node);


    }

    /**
     * Returns the immutable graph
     * @return ImmutableGraph mGraph
     */
    public ImmutableGraph get_mGraph(){
        return mGraph;
    }

    /**
     * Returns the compressed graph
     * @return compressed graph
     */

    public  CompressedGraph get_cGraph(){
        return cGraph;

    }
    public  CompressedEliasFanoGraph get_eGraph() {

        return eGraph;
    }

    /**
     * Returns transposed graph
     * @return
     */
    public boolean get_transposed(){
        return transposed;
    }

    /**
     * Returns True if the loaded graph is webgraph
     * @return Boolean
     */
    public boolean isWebGraph(){
        return webGraph;
    }


    /**
     * Set the compressed graph
     * @param cG byte array compressed graph
     */
    public void set_compressed_graph(byte []cG){
        cGraph.set_compressed_graph(cG);
    }

    /**
     * Set the offset file
     * @param off 2D array
     */
    public void set_offset(int [][] off){
        cGraph.set_offset(off);
    }

    /**
     * Set the web graph
     * @param g ImmutableGraph
     */
    public void set_mGraph(ImmutableGraph g){
        mGraph = g;
    }

    /**
     * Set the boolean value of webgraph
     * @param WG Boolean
     */
    public void set_webGraph(boolean WG){
        webGraph = WG;
    }

    /**
     * Set the boolean value of compressed graph
     * @param nodeList Boolean
     */
    public void set_nodes(int [] nodeList){
        nodes = nodeList;
    }

}
