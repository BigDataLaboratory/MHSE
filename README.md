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
If you want to test MHSE and verify our results, download the Java code from the repository and modify the properties file in etc folder, setting the input file path and output folder path, but also the direction of the messages (in or out). 
This version is for test purpose only.

## Results
You can find the results of MHSE (JSON format) in the results folder. For each graph, we have run the algorithm twenty times with different seed lists (you can find the seed lists in the properties file in etc folder)
