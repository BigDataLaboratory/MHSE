# Propagate
We propose two algorithms to efficiently estimate the effective diameter and other distance metrics on very
large graphs that are based on the neighborhood function such as the exact diameter, the (effective) radius or the average
distance. We exploit the MinHash approach to derive compressed representations of large and sparse datasets that
preserve similarity (signatures), thus to provide a good approximation of the size of the neighborhood of a node. The two
algorithms are based on a technique named MHSE (MinHash Signature Estimation) that exploits the similarity between
signatures to estimate the size of the neighborhood sets. The first algorithm, MHSE, is as effective as HyperANF, the state
of art method for the estimation of the neighborhood function in a very large graph. Indeed, the p-values of both parametric
(t-test) and non-parametric (Wilcoxon) statistical tests on residuals for average distance, effective diameter and number of
connected pairs, show that MHSE tends to produce results that are statistically similar to the correct diameter in more
tested graphs than HyperANF. The second algorithm, SE-MHSE (Space Efficient MHSE), produces the same outcomes of
Propagate but with less space complexity.
These two approaches could be easily extended and optimized in two different ways:
- first, we used a different representation of data (we sign only the minhash node, without storing informations on
  hashes of the other nodes), reducing the amount of memory needed to store essential informations for the algorithm.
  This optimization lead us to the versions Propagate-P and Propagate-S;
- second, we implemented a multi-thread version of the algorithms. This was possible due to the nature of the algorithm, 
 that is completely parallelizable. This second optimization lead us to the versions MultiPropagateP 
 and MultiPropagateS.

# Table of contents


* [Propagate](#Propagate)
* [Table of contents](#table-of-contents)
* [How to run the algorithm](#how-to-run-the-algorithm)
  + [Working example: run test on enron graph](#working-example-run-test-on-enron-graph)
  + [Working example: run test on worldSeriesRetweets graph](#working-example-run-test-on-worldseriesretweets-graph).
  + [The /etc/propagate.properties file](#the-etcpropagateproperties-file)
    - [MinHash section](#minhash-section)
    - [EdgeList2WebGraph section](#edgelist2webgraph-section)
    - [WebGraph2EdgeList section](#webgraph2edgelist-section)
    - [Hyperball section](#hyperball-section)
    - [GroundTruth section](#groundtruth-section)
    - [Seed generation section](#seed-generation-section)
    - [InOut degree section](#inout-degree-section)
* [Results](#results)
  + [Verify results of Propagate P/S tests](#verify-results-of-propagatese-propagate-tests)
* [Link utili](#link-utili)
* [Licenses](#licenza)


# How to run the algorithm
To run one of the minhash-based algorithms, clone this repository and run the application class PropagateMain.
This is the main class for the execution of one of the MinHash-based algorithms. It will output some statistics (number of nodes, number of edges, effective diameter, average distance, lower bound diameter and so on) on a given input graph.
Before executing the code, you will have to set some properties on the */etc/propagate.properties* file (see next sections).
The application will use as input file a graph in WebGraph format (see [this link](http://law.di.unimi.it/datasets.php) for more info about this graph encoding and datasets).
If you have a graph encoded in *edgelist* format, before running Propagate (or similar algorithms) you have to execute *EdgeList2WebGraph* application to have a WebGraph version of your edgelist-encoded graph.  
There is also the possibility to translate a graph from *WebGraph* format to an *edgelist* format. In this case you have to execute *WebGraph2EdgeList* application.
More informations about applications configuration can be seen in the [*propagate.properties* file section](#the-etcpropagateproperties-file).

## Working example: run test on enron graph
To run tests on a graph encoded as *WebGraph* file you can follow the steps below (in this example we are going to run test on [enron graph](http://law.di.unimi.it/webdata/enron/)).
We are going to assume that you have correctly cloned the [Propagate repository](https://github.com/BigDataLaboratory/MHSE) and we are going to refer to the root of the project as **PropagateRoot**:
- download [enron.graph and enron.properties](http://law.di.unimi.it/webdata/enron/) into a folder of your choice (we are going to refer to the path to this folder as **enronFolder**);
- copy the content of */etc/enronPropagate.properties* and overwrite it into */etc/propagate.properties*;
- the *enronFolder* will contain the 2 enron files with the same name but different extension. We are going to refer to the path to one of this files **without extension** as **enronWebGraph**

- from *PropagateRoot* folder execute command `java -cp ./jar/propagate-1.0.jar it.unimi.dsi.webgraph.BVGraph -o -O -L enronWebGraph`, where you have to change enronWebGraph with your *enronWebGraph* path;

- modify the *minhash.inputFilePath* and *minhash.outputFolderPath* properties of the */etc/propagate.properties* file according to **enronWebGraph** and to a folder that will contain final results and statistics of the algorithm. We are going to refer to this output folder path as **enronResultsFolder**;

- from *propagateRoot* folder execute *MinHashMain* application to execute Propagate algorithm with the command `java -jar ./jar/propagate-1.0.jar`.

- you can find results of the execution of the algorithm into *enronResultsFolder*.
The default minhash algorithm to be executed is *Propagate*. If you want to run the *Space Efficient* version of the algorithm, just modify *minhash.algorithmName* property of the */etc/propagate.properties* to the value **PropagateSE** before last step.
Results of your execution should be the same of the first JSON block of the */results/enron* file.

## Working example: run test on worldSeriesRetweets graph
To run tests on a custom graph encoded as *edgelist* file you can follow the steps below (in this example we are going to run test on [worldSeriesRetweets graph](https://github.com/BigDataLaboratory/Twitter/blob/master/Dataset/)).
We are going to assume that you have correctly cloned the [Propagate repository](https://github.com/BigDataLaboratory/MHSE) and we are going to refer to the root of the project as **propagateRoot**:
- download [worldSeriesRetweets zip file](https://github.com/BigDataLaboratory/Twitter/blob/master/Dataset/worldSeriesRetweets.zip) into a folder of your choice (we are going to refer to the path to this folder as **worldSeriesRetweetsFolder**);

- extract *worldSeriesRetweets graph* (we are going to refer to the path to this file as **worldSeriesRetweetsGraph**) from the zip file previously downloaded with the command `unzip worldSeriesRetweets.zip` (if you don't have *unzip* command installed, please install it with `sudo apt-get install unzip`);

- copy the content of */etc/worldSeriesRetweetsPropagate.properties* and overwrite it into */etc/propagate.properties*;

- modify the *edgeList2WebGraph.inputEdgelistFilePath* and *edgeList2WebGraph.outputFolderPath* properties of the */etc/propagate.properties* file according to **worldSeriesRetweetsGraph** and **worldSeriesRetweetsFolder**;  

- from *propagateRoot* folder execute *EdgeList2WebGraph* application to make a conversion into *WebGraph* format with the command `java -cp ./jar/propagate-1.0.jar it.bigdatalab.applications.EdgeList2WebGraph`. The output of this command will be the creation of the *worldSeriesRetweetsFolder* containing 3 files with the same name but different extension. We are going to refer to the path to one of this files **without extension** as **worldSeriesRetweetsWebGraph**;

- modify the *minhash.inputFilePath* and *minhash.outputFolderPath* properties of the */etc/propagate.properties* file according to **worldSeriesRetweetsWebGraph** and to a folder that will contain final results and statistics of the algorithm. We are going to refer to this output folder path as **worldSeriesResultsFolder**;

- from *propagateRoot* folder execute *MinHashMain* application to execute Propagate algorithm with the command `java -jar ./jar/propagate-1.0.jar`.

- you can find results of the execution of the algorithm into *worldSeriesResultsFolder*.
The default minhash algorithm to be executed is *Propagate*. If you want to run the *Space Efficient* version of the algorithm, just modify *minhash.algorithmName* property of the */etc/propagate.properties* to the value **PropagateSE** before last step.
Results of your execution should be the same of the first JSON block of the */results/worldSeriesRetweets* file.

## The /etc/propagate.properties file
propagate.properties contains properties for all the applications of the project and it is divided in sections, one for each application.
Here the explanation of sections and properties.

### MinHash section
Right now, Propagate, PropagateP (with a multithread version), PropagateSE, PropagateSe (with a multithread version) algorithms are developed.
List of the properties for all the MinHash-based applications.
- **minhash.suggestedNumberOfThreads** handles the parallelization of the algorithm. This property has to be an integer that indicates the number of parallel threads that have to be run
- **minhash.persistCollisionTable** persist the collision table on the output file
- **minhash.inputFilePath** string path of the input file representing a graph in a *WebGraph* format. If your input graph has an *edgelist* format, see *EdgeList2WebGraph* application to make a conversion.
- **minhash.outputFolderPath**  string path of the output folder path, that will contain results of the execution of the algorithm
- **minhash.reorder**  is a boolean value. if True reorder the input graph by degree. **Deprecated**
- **minhash.transpose**  is a boolean value. if True, the input graph is the transpose version
- **minhash.isolatedVertices**  is a boolean value. Keep the isolated nodes if True is set, else it will be removed from input graph
- **minhash.isSeedsRandom** is a boolean value. If it is True, the list of seeds used in the hash functions will be random, else it will be loaded from *minhash.inputFilePathSeedNode* property
- **minhash.algorithmName** string name of the MinHash algorithm to be executed. A list of acceptable name values is available in the following class: it.bigdatalab.algorithm.AlgorithmEnum.
- **minhash.threshold** float value that is the threshold used for the *effective diameter*. Usually it is set to 0.9 (90% of total reachable couples of nodes)
- **minhash.direction** direction of the MinHash messages. Acceptable values are *in* or *out*. If you set *in*, the MinHash is propagated from the destination node to the source node. If you set *out*, from the source to the destination node. This choice doesn't affect computation of all metrics (effective diameter, average distance and so on) but it could make a difference in convergence time.
- **minhash.numSeeds** number of seeds used for MinHash algorithm. If isSeedsRandom is False, you can set numSeeds to 0 to compute GroundTruth (**Important**: you must set nodeIDRange)
- **minhash.nodeIDRange** graph nodes to compute Ground Truth (pattern to follow: "0,n-1" with n as number of nodes)
- **minhash.inputFilePathSeedNode** string path of the external json file containing seeds list and nodes list
- **minhash.inMemory** is a boolean value. If True is set, load the entire graph in memory.
- **minhash.computeCentrality** This property will be used in a **future development**
In this section, we list properties to run multiple tests of the same algorithm:
- **minhash.numTests** integer value representing the number of tests to be done. We need to run algorithm multiple times to get significance test e.g. mean and variance of all tests. All output results will be written in JSON format (see Results section).

### EdgeList2WebGraph section
In this section, we list properties used to translate a graph encoded in *edgelist* format into a *WebGraph* encoded file.
- **edgeList2WebGraph.inputEdgelistFilePath** string path of the input file representing a graph in an *edgelist* format
- **edgeList2WebGraph.outputFolderPath** string path of the output folder where the application will persist the graph encoded in *WebGraph* format
- **edgeList2WebGraph.fromJanusGraph** is a boolean value. If True, normalize nodes IDs (JanusGraph encode node IDs as multiple of 4. This property divide IDs to have sequential IDs in the output)

### WebGraph2EdgeList section
In this section, we list properties used to translate a graph encoded in *WebGraph* format into an *edgelist* encoded file.
- **webGraph2EdgeList.inputFilePath** string path of the input file, representing a graph in an *WebGraph* format
- **webGraph2EdgeList.outputFolderPath** string path of the output folder where the application will persist the graph encoded in an *edgelist* format

### Hyperball section
In this section, we list properties to run Hyperball algorithm.
- **hyperball.suggestedNumberOfThreads** handles the parallelization of the algorithm. This property has to be an integer that indicates the number of parallel threads that have to be run
- **hyperball.inputFilePath** string path of the input file representing a graph in a *WebGraph* format. If your input graph has an *edgelist* format, see *EdgeList2WebGraph* application to make a conversion.
- **hyperball.outputFolderPath**  string path of the output folder path, that will contain results of the execution of the algorithm
- **hyperball.log2m** number of seeds used for Hyperball algorithm
- **hyperball.isolatedVertices**  is a boolean value. Keep the isolated nodes if True is set, else it will be removed from input graph
- **hyperball.threshold** float value that is the threshold used for the *effective diameter*. Usually it is set to 0.9 (90% of total reachable couples of nodes)
- **hyperball.numTests** integer value representing the number of tests to be done. We need to run algorithm multiple times to get significance test e.g. mean and variance of all tests. All output results will be written in JSON format (see Results section).
- **hyperball.direction** direction of the messages. Acceptable values are *in* or *out*. If you set *in*, the message is propagated from the destination node to the source node. If you set *out*, from the source to the destination node. This choice doesn't affect computation of all metrics (effective diameter, average distance and so on) but it could make a difference in convergence time.
- **hyperball.inMemory** is a boolean value. If True is set, load the entire graph in memory.

### GroundTruth section
In this section, we list properties to run GroundTruth implementation developed by WebGraph.
- **groundTruth.threadNumber** handles the parallelization of the algorithm. This property has to be an integer that indicates the number of parallel threads that have to be run
- **groundTruth.inputFilePath** string path of the input file representing a graph in a *WebGraph* format. If your input graph has an *edgelist* format, see *EdgeList2WebGraph* application to make a conversion.
- **groundTruth.outputFolderPath**  string path of the output folder path, that will contain results of the execution of the algorithm
- **groundTruth.isolatedVertices**  is a boolean value. Keep the isolated nodes if True is set, else it will be removed from input graph
- **groundTruth.inMemory** is a boolean value. If True is set, load the entire graph in memory

### Seed generation section
In this section, we list properties to generate the seeds lists for a graph
- **seed.inputFilePath** string path of the input file representing a graph in a *WebGraph* format. If your input graph has an *edgelist* format, see *EdgeList2WebGraph* application to make a conversion.
- **seed.outputFolderPath**  string path of the output folder path, that will contain results of the execution of the algorithm (results in json format)
- **seed.numSeeds** an integer value representing the number of seeds to generate
- **seed.isolatedVertices**  is a boolean value. Keep the isolated nodes if True is set, else it will be removed from input graph
- **seed.numTest** integer value representing the number of seeds lists to generate
- **seed.inMemory** is a boolean value. If True is set, load the entire graph in memory.

### InOut degree section
In this section, we list properties to compute in and out degree of each node of an input graph
- **inoutdegree.inputFilePath** string path of the input file representing a graph in a *WebGraph* format. If your input graph has an *edgelist* format, see *EdgeList2WebGraph* application to make a conversion.
- **inoutdegree.outputFolderPath**  string path of the output folder path, that will contain results of the execution of the algorithm (results in json format)
- **inoutdegree.isolatedVertices**  is a boolean value. Keep the isolated nodes if True is set, else it will be removed from input graph
- **inoutdegree.inMemory** is a boolean value. If True is set, load the entire graph in memory.


## Results
You can find the results of Propagate (JSON format) in the results folder. For each graph, we have run the algorithm twenty times with different seed lists (you can find the seed lists in the properties file in etc folder)

### Verify results of Propagate/PropagateSE tests
If you want to test Propagate and verify our results, download the Java code from the repository and modify the properties file in etc folder according to the graph (and results) you are interested into. 
For example, if you are interested in the replication of tests stored in */results/amazon-2008* modify */etc/propagate.properties* according to the corresponding json object in */results/amazon-2008* and then execute the *MinHashMain* application. 
For test replication purposes, you have to know that there are 2 types of graph. The replication of tests differs according to the type of the input graph:
- *WebGraph* graphs: *amazon-2008*, *cnr-2000*, *com-dblp*, *dblp-2010*, *email-EuAll*, *enron*, *uk-2007-05@100000*, *web-NotreDame*. For these graphs you can download data from [this link](http://law.di.unimi.it/datasets.php), modify *MinHash* section of the */etc/propagate.properties* file and execute *PropagateMain* application directly. 
- *Custom* graphs in *edgelist* format: *blackFridayRetweets*, *worldSeriesRetweets* (from [this repository](https://github.com/BigDataLaboratory/Twitter)), and *com-youtube*, *soc-Slashdot*, *web-BerkStan*, *web-Google* (from [this link](http://snap.stanford.edu/data/index.html)),
modify *EdgeList2WebGraph* section of the */etc/propagate.properties* file and execute *EdgeList2WebGraph* application to make a conversion into *WebGraph* format. 
After that you have to modify *MinHash* section of the */etc/propagate.properties* file and execute *MinHashMain* application. 
For further information about these graphs see the corresponding [*README*](https://github.com/BigDataLaboratory/Twitter/blob/master/Dataset/README.txt)