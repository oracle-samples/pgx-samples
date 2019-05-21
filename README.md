# Sample applications using Parallel Graph AnalytiX (PGX)

## Table of contents:

1. [Overview](#overview)
2. [Download PGX](#pgx-download)
3. [Use Cases and Samples](#use-cases)
4. [Graph-based ML applications using PgxML](#pgxml-apps)
    1. [Graphlet representation](#graphlet-representation)
    2. [Node representation](#node-representation)
5. [PGX Algorithms](#pgx-algorithms)
    1. [Article Ranking](#article-ranking)
    2. [Movie Recommendation](#movie-recommender)

****
    
## Overview <a name="overview"></a>
This repository contains some sample applications for Graph-based ML using [PgxML library](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/index.html)
as well as some graph algorithm implementations using [PGX Algorithm](https://docs.oracle.com/cd/E56133_01/latest/tutorials/algorithm/index.html).

## Download PGX <a name="pgx-download"></a>
PGX can be downloaded from Oracle Technology Network (OTN): [download link](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads/index.html).
PgxML and PGX Algorithm are available as of version 3.2.0 and released under [OTN license](https://www.oracle.com/technetwork/licenses/standard-license-152015.html).
Obtain the latest `pgx-x.y.z-server` zip file and unzip it in the `libs` folder. These set of PGX libraries support standalone java applications.

## Use Cases and Sample <a name="pgx-download"></a>
We provide a set of extended examples, demonstrating the capabilities of PGX. The examples are listed on a [separate page](use-cases/README.md).

## Graph-based ML applications using PgxML <a name="pgxml-apps"></a>
We provide two Graph-based ML applications, namely, `Graphlet representation` and `Node representation`.

### Graphlet representation <a name="graphlet-representation"></a>
This application demostrates how we can extract vector representation for each graphlet in a cluster of graphlets.
For this application, we use the [PG2Vec](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/pg2vec.html) algorithm.
More details regarding this application is available [here](graphlet-representation/README.md).

### Node representation <a name="node-representation"></a>
This application demonstrates how we can extract vector representation for each node in a graph.
For this application, we use the [DeepWalk](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/deepwalk.html) algorithm.
More details regarding this application is available [here](node-representation/README.md).

## PGX Algorithms <a name="pgx-algorithms"></a>
We provide implementations of two classical graph algorithms, namely, `Article Rank` and `Matrix Factorization`.

### Article Ranking <a name="article-ranking"></a>
This application demonstrates how ArticleRank could be employed to measure the influence of journal articles.
More details regarding this application is available [here](pgx-algorithms/README.md)

### Movie Recommendation <a name="movie-recommender"></a>
This application demonstrates how Matrix Factorization could be employed to recommend movies to users.
More details regarding this application is available [here](pgx-algorithms/README.md)

