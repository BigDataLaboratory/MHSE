# README

Python program that summarizes the results computed by the MHSE java package

## Table of Contents

- [MinHash Signature Estimation Algorithm](#MHSE)
  - [Description](#Short-Intro)
  - [Output structure](#Java package output)
- [The Configuration file](#Configuration-File)

# Description

This script allows you to analyze and fully reproduce our MHSE experiments.

# MHSE

## Short-Intro

MHSE is an algorithm to efficiently estimate the effective diameter and other distance metrics on very large graphs that are based on the neighborhood function such as the exact diameter, the (effective) radius or the average distance ([more details](https://www.semanticscholar.org/paper/Estimation-of-distance-based-metrics-for-very-large-Amati-Angelini/ca07e5fa517fc7567406ebc683dad35aa43758d4)) .
Currently, we have published two version of the algorithm: the original one (MHSE), and the space efficient one (SE-MHSE) that, produces the same outcomes of MHSE but with less space complexity.
SE-MHSE allows you to run this algorithm on machines with limited memory and also to easily parallelize it using any map-reduce framework.
You can find our algorithm at the following [link](https://github.com/BigDataLaboratory/MHSE) .

## Java package output

The java package gives an outputs composed by a list of JSON structured as follows:

```
[
 .
 .
 .,
   {
        "type": "it.bigdatalab.model.GraphMeasureOpt",
        "hop_table": [ .... ],
        "memory_used": 
        "lower_bound": 
        "avg_distance":
        "effective_diameter":
        "total_couples":
        "total_couples_perc":
        "time":
        "algorithm":
        "threshold":
        "num_seed": 
        "nodes": 
        "edges":
        "direction": 
        "run": 
    },
 .
 .
 .
]
```

# Configuration-File

To run the program you need to write in the config.ini file the required informations.

The .ini file is divided in two sections:

#### <u>[VALUES_FROM_COLLISION_TABLE]</u>

**Parameters**:

- input_file : input/path/file.json  input path  of the collision table file given by  package ( it can be obtained by setting persistCollisionTable = True in the properties file)

- seed_number: 16,32,....,n_{seedMax} Is the number of seeds that you want to retrieve the estimations of our algorithms. The format isn_1 , n_2 , n_3,\dots ,n_j i.e., n_1 comma n2 comma n_3,....

- output_folder : output/path/file output path and name of the output file

- additional_information : Additional information (advanced option), leave it empty

#### <u>[COMPUTE_RESULTS]</u>

**Parameters:**

- input_file_exact_measures : input/path/file.json input file (.json) that contains the                                                  exact values (ground truth values) of the analyzed                                                  graph.

- input_file_estimation : input/path/file.json input file (.json) that contains the                                                  estimated values (results of the algorithms) on  graph.

- output_folder : output/path/file output path and name of the output file (it will create a .csv file with all the results: residuals, t-test/wilcoxon pvalues, confidence intervals, etc)

- label_path : input/path/label_map.json (advanced) file that has a map old_name, new_name that makes the program write the output names as given in the mapping. Suggestion: leave it empty



#### Examples

In the examples folder there are two examples on how to compute the algorithm scores from the collsion table and how to estimate the errors of the algorithms