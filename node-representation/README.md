# Node representation using DeepWalk

## Table of contents:

1. [Overview](#overview)
2. [Obtain data](#obtain-data)
3. [Graph creation](#graph-creation)
4. [Generate node vectors using DeepWalk](#generate-node-vectors)
5. [Node classifier](#graphlet-classifier)

****
    
## Overview <a name="overview"></a>
DeepWalk is a widely employed vertex representation learning algorithm used in industry (e.g., in [Taobao from Alibaba](https://dl.acm.org/citation.cfm?doid=3219819.3219869)). 
It consists of two main steps. First, the random walk generation step computes random walks for each vertex (with a pre-defined walk length 
and a pre-defined number of walks per vertex). Second, these generated walks are fed to a word2vec algorithm to generate the vector representation 
for each vertex (which is the word in the input provided to the word2vec algorithm). Further details regarding the DeepWalk algorithm is available in the [KDD paper](https://dl.acm.org/citation.cfm?id=2623732).

DeepWalk creates vertex embeddings for a specific graph and cannot be updated to incorporate modifications on the graph. 
Instead, a new DeepWalk model should be trained on this modified graph.

## Obtain data <a name="obtain-data"></a>


## Graph creation <a name="graph-creation"></a>


## Generate node vectors using DeepWalk <a name="generate-node-vectors"></a>


## Node classifier <a name="node-classifier"></a>
