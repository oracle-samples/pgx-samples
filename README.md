# Sample applications using Parallel Graph AnalytiX (PGX)

## Table of contents:

1. [Overview](#overview)
2. [Download PGX](#pgx-download)
3. [Healthcare Fraud Detection](#healthcare-fraud-detection)
4. [Super Hero Network Analysis](#super-hero-network-analysis)
5. [Graph-based ML applications using PgxML](#pgxml-apps)
    1. [Graphlet representation](#graphlet-representation)
    2. [Node representation](#node-representation)
6. [Article Ranking](#article-ranking)
7. [Movie Recommendation](#movie-recommender)
8. [Entity Linking](#entity-linking)
9. [Research Paper Classification](#paper-classification)

****
    
## Overview <a name="overview"></a>
This repository contains a set of examples and use cases that illustrate the capabilities of [PGX](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix).
Some of these use cases act as examples for some advanced functionalities, such as:

* The [PgxML library](https://docs.oracle.com/cd/E56133_01/latest/prog-guides/mllib/index.html), for Graph-based ML
* The [PGX Algorithm](https://docs.oracle.com/cd/E56133_01/latest/reference/analytics/pgx-algorithm.html) API, a high-level DSL for developing optimized graph algorithms.

## Download PGX <a name="pgx-download"></a>
PGX can be downloaded from Oracle Technology Network (OTN): [download link](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads).
PgxML and PGX Algorithm are available as of version 3.2.0 and released under the [OTN license](https://www.oracle.com/technetwork/licenses/standard-license-152015.html).
Obtain the latest `pgx-x.y.z-server` zip file from the [PGX download page](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads) and unzip it in the `libs` folder.

## Healthcare Fraud Detection <a name="healthcare-fraud-detection"></a>
The healthcare fraud detection example detects anomalies in medical transactions through a graph analysis procedure implemented in PGX.
More details regarding this use-case are available [here](healthcare/README.md).

## Super Hero Network Analysis <a name="super-hero-network-analysis"></a>
The Super Hero Network Analysis example describes how to combine computational graph analysis and graph pattern matching with PGX.
More details regarding this use-case are available [here](superhero/README.md).

## Graph-based ML applications using PgxML <a name="pgxml-apps"></a>
We provide two Graph-based ML applications, namely, `Graphlet representation` and `Node representation`.

### Graphlet representation <a name="graphlet-representation"></a>
This application demostrates how we can extract vector representation for each graphlet in a cluster of graphlets.
For this application, we use the [PG2Vec](https://docs.oracle.com/cd/E56133_01/latest/prog-guides/mllib/pg2vec.html) algorithm.
More details regarding this application are available [here](graphlet-representation/README.md).

### Node representation <a name="node-representation"></a>
This application demonstrates how we can extract vector representation for each node in a graph.
For this application, we use the [DeepWalk](https://docs.oracle.com/cd/E56133_01/latest/prog-guides/mllib/deepwalk.html) algorithm.
More details regarding this application are available [here](node-representation/README.md).

## Article Ranking <a name="article-ranking"></a>
This application demonstrates how ArticleRank could be employed to measure the influence of journal articles.
More details regarding this application are available [here](article-ranking/README.md).

## Movie Recommendation <a name="movie-recommendation"></a>
This application demonstrates how Matrix Factorization could be employed to recommend movies to users.
More details regarding this application are available [here](movie-recommendation/README.md).

## Entity Linking <a name="entity-linking"></a>
Entity Linking allows to connect Named Entities (for example, names of famous people) to their Wikipedia/DBpedia page.
 This application leverages vertex embeddings to provide high-quality results. More details available [here](entity-linking/README.md) and in our [paper](https://dl.acm.org/citation.cfm?doid=3327964.3328499).

## Research Paper Classification <a name="paper-classification"></a>
This application demonstrates how graph data can be used to enhance classification performance of a research paper classifier.
More details regarding this application are available [here](paper-classification/README.md).
