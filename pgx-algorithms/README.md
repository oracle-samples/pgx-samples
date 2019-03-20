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
4. Run `gradle runMovieRecommender -Pdata="/tmp/foo/bar"` for training a model and testing it. Or run `gradle runMovieRecommender -Pdata="/tmp/foo/bar" -PuserID=1 -PtopK=10` for training, testing and generate recommendations, where `-PuserID` must be a valid user ID from the `userId` column of the `ratings.csv` file in the dataset, and `-PtopK` is the number of ranked and recommended movies to show as example. <!-- gradle runMovieRecommender -Pargs="/tmp/ml-latest-small" -PuserID=1 -PtopK=10` -->

This command runs the `oracle.pgx.algorithms.MovieRecommender` class, which:

1. Prepares the graph data
 1. Shuffles the ratings. In step (2) the ratings are partitioned; by shuffling we avoid bias in the test graph.
 2. Partitions the edges (ratings) into two sets: training (80%) and test (20%).
 3. Prepends a "1" to user IDs and a "2" to movie IDs to ensure that all vertex IDs are unique.
 4. Adds an `is_left` property which is `true` for users and `false` for movies.
2. Loads the training- and test graph.
3. Compiles the Matrix Factorization Gradient Descent algorithm, runs it on the training graph and prints its Root Mean Squared Error (RMSE) associated.
4. Computes and prints the Root Mean Squared Error (RMSE) for the test set.

This computes a RMSE of 0.88, similar to the RMSE that can be obtained with different algorithms.
