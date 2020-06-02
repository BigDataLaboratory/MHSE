# MHSE
We propose two algorithms to efficiently estimate the effective diameter and other distance metrics on very
large graphs that are based on the neighborhood function such as the exact diameter, the (effective) radius or the average
distance. We exploit the MinHash approach to derive compressed representations of large and sparse datasets that
preserve similarity (signatures), thus to provide a good approximation of the size of the neighborhood of a node. The two
algorithms are based on a technique named MinHash Signature Estimation (MHSE) that exploits the similarity between
signatures to estimate the size of the neighborhood sets. The first algorithm, MHSE, is as effective as HyperANF, the state
of art method for the estimation of the neighborhood function in a very large graph. Indeed, the p-values of both parametric
(t-test) and non-parametric (Wilcoxon) statistical tests on residuals for average distance, effective diameter and number of
connected pairs, show that MHSE tends to produce results that are statistically similar to the correct diameter in more
tested graphs than HyperANF. The second algorithm, SE-MHSE (Space Efficient MHSE), produces the same outcomes of
MHSE but with less space complexity. We show when SE-MHSE is also more space-efficient than HyperANF. We finally
discuss how SE-MHSE can be easily distributed.

## How to run the algorithm
To run the MHSE algorithm (or the equivalent SE-MHSE version) clone this repository and run the application class MinHashMain.
This is the main class for the execution of the algorithm that will output some statistics (number of nodes, number of edges, effective diameter, average distance, lower bound diameter etc.) on a given input graph.
Before to execute the code, you will have to set some properties on the */etc/mhse.properties* file (see next sections).
The application will use as input file a graph in WebGraph format (see [this link](http://law.di.unimi.it/datasets.php) for more informations about this graph encoding and datasets).
If you have a graph encoded in *edgelist* format, before to run MHSE you have to execute *EdgeList2WebGraph* application to have a WebGraph version of your edgelist-encoded graph.  
If you need to translate a graph in *WebGraph* format to an *edgelist* format you can execute *WebGraph2EdgeList* application. 
To have more informations about configuration of the application of your interest, see relative section in the *mhse.properties* file.  

## The /etc/mhse.properties file
mhse.properties contains properties for all the applications of the project and it is divided in sections, one for each application.
Here the explanation of sections and properties.

### MinHash section
In this section there are all properties for all MinHash-based applications.
At the time of the writing, there are MHSE and SE-MHSE algorithms implemented.
Below all the possible properties of this section:
- **minhash.suggestedNumberOfThreads** is the property that manages the parallelization of the algorithm. This property has to be a number that indicates the number of parallel threads that have to be run.
- **minhash.inputFilePath** is a String that is the path to the input file representing a graph in a *WebGraph* format. If your input graph has an *edgelist* format, see *EdgeList2WebGraph* application to make a conversion.
- **minhash.outputFolderPath**  is a String that is the path to the output folder path that will contain results of the execution of the algorithm
- **minhash.isSeedsRandom** is a boolean value. If it is True, the list of seeds used in the hash functions will be random, else it will be loaded from *minhash.seeds* property 
- **minhash.algorithmName** is a String representing the name of the MinHash algorithm to be executed. To see a list of acceptable name values, see class it.uniroma2.algorithm.AlgorithmEnum.  
- **minhash.threshold** is a float value that is the threshold used for the *effective diameter*. Usually it is set to 0.9 (90% of total reachable couples of nodes)
- **minhash.direction** direction of the minhash messages. Acceptable values are *in* or *out*. If *in* the minhash will propagate from the destination node to the source node. If *out*, from the source node to the destination.
- **minhash.numSeeds** is the number of seeds used for minhash algorithm
- **minhash.seeds** is the list of seeds (comma separated values) to be used for the hash functions of the minhash algorithm. Single test

In this section, there are properties that gives to the user the possibility to run multiple tests of the same algorithm.
These properties are: 
- **minhash.runTests** is a boolean value. If it is *True*, the Test mode will be activated and will be run multiple tests pf the same algorithm. 
- **minhash.numTests** is an integer value representing the number of tests to be done.
- **minhash.seeds1** to **minhash.seedsX** are a series of lists of seeds to be used in multiple executions of the algorithm. Of course, the number *X* has to be the same of the number specified in *minhash.numTests* property.    

### EdgeList2WebGraph section
In this section there are all properties for the application used to translate a graph encoded in *edgelist* format into a *WebGraph* encoded file.
- **edgeList2WebGraph.inputEdgelistFilePath** is a String that is the path to the input file representing a graph in an *edgelist* format
- **edgeList2WebGraph.outputFolderPath** is a String that is the path to the output folder where will be stored the graph encoded in *WebGraph* format
- **edgeList2WebGraph.fromJanusGraph** is a boolean value. If True there will be a normalization on nodes IDs (JanusGraph encode node IDs as multiple of 4. This property will divide IDs to have sequential IDs in the output)

### WebGraph2EdgeList section
In this section there are all properties for the application used to translate a graph encoded in *WebGraph* format into an *edgelist* encoded file.
- **webGraph2EdgeList.inputFilePath** is a String that is the path to the input file representing a graph in an *WebGraph* format
- **webGraph2EdgeList.outputFolderPath** is a String that is the path to the output folder where will be stored the graph encoded in an *edgelist* format

##Verify results of MHSE/SE-MHSE tests
If you want to test MHSE and verify our results, download the Java code from the repository and modify the properties file in etc folder according to the graph (and results) you are interested into. 
For example, if you are interested in the replication of tests stored in */results/amazon-2008* modify */etc/mhse.properties* according to the corresponding json object in */results/amazon-2008* and then execute the *MinHashMain* application. 
For test replication purposes, you have to know that there are 2 types of graph. The replication of tests differs according to the type of the input graph:
- *WebGraph* graphs: *amazon-2008*, *cnr-2000*, *com-dblp*, *dblp-2010*, *email-EuAll*, *enron*, *uk-2007-05@100000*, *web-NotreDame*. For these graphs you can download data from [this link](http://law.di.unimi.it/datasets.php), modify *MinHash* section of the */etc/mhse.properties* file and execute *MinHashMain* application directly. 
- *Custom* graphs in *edgelist* format: *blackFridayRetweets*, *samplingItalianoRetweets*, *worldSeriesRetweets*. For these custom graphs you can download data from [this repository](https://github.com/BigDataLaboratory/Twitter), modify *EdgeList2WebGraph* section of the */etc/mhse.properties* file and execute *EdgeList2WebGraph* application to make a conversion into *WebGraph* format. After that you have to modify *MinHash* section of the */etc/mhse.properties* file and execute *MinHashMain* application. For further informations about these graphs see the corresponding [*README*](https://github.com/BigDataLaboratory/Twitter/blob/master/Dataset/README.txt)

## Results
You can find the results of MHSE (JSON format) in the results folder. For each graph, we have run the algorithm twenty times with different seed lists (you can find the seed lists in the properties file in etc folder)
