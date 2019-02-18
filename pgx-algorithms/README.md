# Implementing Graph Algorithms using PGX Algorithm API

## Table of Contents:

1. [Overview](#overview)
2. [Article Ranking](#article)
3. [Movie Recommendation](#movie)

## Overview <a name="overview"></a>

This repository contains two examples: _Article Ranking_ and _Movie Recommendation_.
Both examples use an algorithm that is written in _PGX Algorithm_, a Java-like language that makes it easy to write parallel graph algorithms.

## Article Ranking <a name="article"></a>

ArticleRank is an algorithm that has been derived from Google's PageRank algorithm to measure the influence of journal articles.
PageRank has an inherent bias in that a paper with very few references will make a greater contribution to other papers' PageRank scores than will a paper with many references.
Specifically, the PageRank algorithm defines the PageRank of a vertex as the sum of the pagerank of its neighboring vertices divided by the neighbor's out-degree.
With ArticleRank each neighbor's ArticleRank is divided by the out-degree of the neighbor _plus the average out-degree of all vertices_.
That way a vertex with a low out-degree contributes less to the rank of its neighbors.

The ArticleRank example operates on the [WebGraph](https://snap.stanford.edu/data/web-Google.html) data set.
To run the example:

1. Download and install [Gradle](https://gradle.org/install/) (version 5.2.1 or higher).
2. Download [web-Google.txt.gz](https://snap.stanford.edu/data/web-Google.txt.gz) from the [SNAP  Google web graph](https://snap.stanford.edu/data/web-Google.html) page. <!-- `wget -O /tmp/web-Google.txt.gz https://snap.stanford.edu/data/web-Google.txt.gz` -->
3. Extract `web-Google.txt.gz` to some writable directory `/tmp/foo/bar`. <!-- `gunzip /tmp/web-Google.txt.gz` -->
4. Run `gradle runArticleRanker -Pargs="/tmp/foo/bar"`. <!-- `gradle runArticleRanker -Pargs="/tmp/"` -->

This command runs the `oracle.pgx.algorithms.ArticleRanker` class, which:

1. Prepares the graph data.
2. Loads the graph.
3. Compiles and runs the ArticleRank algorithm on the graph.
4. Prints the ArticleRank for the first 10 vertices.

## Movie Recommendation <a name="movie"></a>

The Movie Recommendation example operates on the [MovieLens](https://grouplens.org/datasets/movielens/) data set.
To run the example:

1. Download and install [Gradle](https://gradle.org/install/) (version 5.2.1 or higher).
2. Download [ml-latest-small.zip](http://files.grouplens.org/datasets/movielens/ml-latest-small.zip) from the [MovieLens datasets](https://grouplens.org/datasets/movielens/) page. <!-- `wget -O /tmp/ml-latest-small.zip http://files.grouplens.org/datasets/movielens/ml-latest-small.zip` -->
3. Extract `ml-latest-small.zip` to some writable directory `/tmp/foo/bar`. <!-- `unzip /tmp/ml-latest-small.zip -d /tmp/` -->
4. Run `gradle runMovieRecommender -Pargs="/tmp/foo/bar"`. <!-- gradle runMovieRecommender -Pargs="/tmp/ml-latest-small"` -->

This command runs the `oracle.pgx.algorithms.MovieRecommender` class, which:

1. Prepares the graph data (e.g. partition in a training- and test set).
2. Loads the graph.
3. Compiles and runs the Matrix Factorization Gradient Descent algorithm on the graph.
4. Prints the Root Mean Squared Error for the test set.
