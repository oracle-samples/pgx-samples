# Graphlet classification using PG2Vec

## Table of contents:

1. [Overview](#overview)
2. [Obtain data](#obtain-data)
3. [Graph creation](#graph-creation)
4. [Generate graphlet vectors using PG2Vec](#generate-graphlet-vectors)
5. [Graphlet classifier](#graphlet-classifier)

****
    
## Overview <a name="overview"></a>
Pg2vec learns representations of graphlets (partitions inside a graph) by employing edges as the principal learning units 
and thereby packing more information in each learning unit (as compared to previous approaches employing vertices as learning units) 
for the representation learning task. It consists of three main steps. First, we generate random walks for each vertex (with pre-defined length 
per walk and pre-defined number of walks per vertex). Second, each edge in this random walk is mapped as a property edge-word in the created document 
(with the document label as the graph-id) where the property edge-word is defined as the concatenation of the properties of the source and 
destination vertices. Lastly, we feed the generated documents (with their attached document labels) to a [doc2vec](https://dl.acm.org/citation.cfm?id=3044805.3045025) algorithm which generates the vector representation for each document (which is a graph in this case).

Pg2vec creates graphlet embeddings for a specific set of graphlets and cannot be updated to incorporate modifications on these graphlets. 
Instead, a new Pg2vec model should be trained on these modified graphlets.


## Obtain data <a name="obtain-data"></a>
The data for the PG2Vec application should be download and placed in the `data` folder.
We provide one example for the [NCI109 dataset](https://ls11-www.cs.tu-dortmund.de/staff/morris/graphkerneldatasets) which could be
fetched by executing the `get_data.sh` script.


## Graph creation <a name="graph-creation"></a>
This phase of the application builds up the graph (to be consumed by the PgxML application).
We provide the `graph-creator.py` script for this functionality.


## Generate graphlet vectors using PG2Vec <a name="generate-graphlet-vectors"></a>
In this phase, we run the PG2Vec algorithm using the provide gradle script by executing `gradle clean run`.
The output of this step is a `csv` file with the graphlet vectors learnt using PG2Vec algorithm.


## Graphlet classifier <a name="graphlet-classifier"></a>
In this last phase, we provide a Convolutional Neural Network to classify the graphlet vectors.
We provide two python scripts, one for generating the train/test dataset for the classifier (`prepare-classifier-data.py`) and second for the classifier itself (`classifier.py`).
