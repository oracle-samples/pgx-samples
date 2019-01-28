# Sample applications using Parallel Graph AnalytiX (PGX)

## Table of contents:

1. [Overview](#overview)
2. [Download PGX](#pgx-download)
3. [Graph-based ML applications using PgxML](#pgxml-apps)
    1. [Graphlet classification](#graphlet-classification)
    2. [Node classification](#node-classification)
4. [Graph Algorithms](#graph-algorithms)

****
    
## Overview <a name="overview"></a>
This repository contains some sample applications for Graph-based ML using [PgxML library](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/index.html)
as well as some graph algorithm implementations using [PGX Algorithm](https://docs.oracle.com/cd/E56133_01/latest/tutorials/algorithm/index.html).

## Download PGX <a name="pgx-download"></a>
PGX could be downloaded from Oracle Technology Network (OTN): [download link](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads/index.html).
PgxML and PGX Algorithm are available from the 3.2.0 version of PGX and released under [OTN license](https://www.oracle.com/technetwork/licenses/standard-license-152015.html).
Obtain the `pgx-3.2.0-server` zip file and unzip it in `libs` folder. These set of PGX libraries support standalone java applications. (Also mentioned [here](libs/README.md).)

## Graph-based ML applications using PgxML <a name="pgxml-apps"></a>
We provide two Graph-based ML applications, namely, `Graphlet classification` and `Node classification`.

### Graphlet classification <a name="graphlet-classification"></a>
This application demostrates how we can extract vector representation for each graphlet in a cluster of graphlets.
For this application, we use the [PG2Vec](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/pg2vec.html) algorithm.
More details regarding this application are available [here](graphlet-classification/README.md).

### Node classification <a name="node-classification"></a>
This application demonstrates how we can extract vector representation for each node in a graph.
For this application, we use the [DeepWalk](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/deepwalk.html) algorithm. 

## Graph Algorithms <a name="graph-algorithms"></a>
We provide some implementations of widely-used classical graph algorithms like pagerank.