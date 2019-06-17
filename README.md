# Sample applications using Parallel Graph AnalytiX (PGX)

## Table of contents:

1. [Overview](#overview)
2. [Download PGX](#pgx-download)
3. [Use Cases and Samples](#use-cases)
4. [Graph-based ML applications using PgxML](#pgxml-apps)
    1. [Graphlet representation](#graphlet-representation)
    2. [Node representation](#node-representation)
5. [Article Ranking](#article-ranking)
6. [Movie Recommendation](#movie-recommender)

****
    
## Overview <a name="overview"></a>
This repository contains a set of examples and use cases that illustrate the the capabilities of [PGX](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix).
The use cases are are included in the [use-cases folder](use-cases) and include:

* [Healthcare Fraud Detection](healthcare/README.md)
* [Super Hero Network Analysis](superhero/README.md)

Besides "classic" PGX use cases, we also provide illustrative examples for some advanced funcionalities, such as:

* The [PgxML library](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/index.html), for Graph-based ML
* The [PGX Algorithm](https://docs.oracle.com/cd/E56133_01/latest/tutorials/algorithm/index.html) API, a high-level DSL for developing optimized graph algorithms.

## Download PGX <a name="pgx-download"></a>
PGX can be downloaded from Oracle Technology Network (OTN): [download link](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads).
PgxML and PGX Algorithm are available as of version 3.2.0 and released under the [OTN license](https://www.oracle.com/technetwork/licenses/standard-license-152015.html).
Obtain the latest `pgx-x.y.z-server` zip file from the [PGX download page](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads) and unzip it in the `libs` folder.

## Use Cases and Samples <a name="pgx-use-cases"></a>
We provide a set of use cases and examples to demonstre the capabilities of PGX.
The use cases are are included in the [use-cases folder](use-cases) folder.

## Graph-based ML applications using PgxML <a name="pgxml-apps"></a>
We provide two Graph-based ML applications, namely, `Graphlet representation` and `Node representation`.

### Graphlet representation <a name="graphlet-representation"></a>
This application demostrates how we can extract vector representation for each graphlet in a cluster of graphlets.
For this application, we use the [PG2Vec](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/pg2vec.html) algorithm.
More details regarding this application are available [here](graphlet-representation/README.md).

### Node representation <a name="node-representation"></a>
This application demonstrates how we can extract vector representation for each node in a graph.
For this application, we use the [DeepWalk](https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/deepwalk.html) algorithm.
More details regarding this application are available [here](node-representation/README.md).

## Article Ranking <a name="article-ranking"></a>
This application demonstrates how ArticleRank could be employed to measure the influence of journal articles.
More details regarding this application are available [here](article-ranking/README.md).

## Movie Recommendation <a name="movie-recommendation"></a>
This application demonstrates how Matrix Factorization could be employed to recommend movies to users.
More details regarding this application are available [here](movie-recommendation/README.md).

