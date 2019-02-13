# Implementing Graph Algorithms using PGX Algorithm API

## Table of Contents:

1. [Overview](#overview)
3. [Usage](#usage)

## Overview <a name="overview"></a>

PGX Algorithm allows you to write your graph algorithm in Java and have it automatically compiled
to an efficient parallel implementation targeting either the single-machine (shared-memory) or
distributed runtime.

An implementation of the Matrix Factorization Gradient Descent can be found in `src/main/resources/`.
This project contains a single class `oracle.pgx.algorithms.Main` that takes the path to a graph, compiles the Matrix
Factorization Gradient Descent algorithm, runs the algorithm on the graph, and reports the precision.

## Usage <a name="usage"></a>

0. Download and Install Gradle.
1. Download `ml-latest-small.zip` from https://grouplens.org/datasets/movielens/, e.g. `wget /tmp/ml-latest-small.zip http://files.grouplens.org/datasets/movielens/ml-latest-small.zip`.
2. Extract the zip, e.g. `unzip /tmp/ml-latest-small.zip -d /tmp/`.
3. Run `gradle run --args="<path-to-data>"` where `<path-to-data>` is the path to the directory where you extracted the `ml-latest-small.zip`. This command runs the `oracle.pgx.algorithms.Main` class, which does the following:
  1. Massage the data.
   1. Create users.csv from ratings.csv.
   2. Create a movies.csv with the correct header.
   3. Prepend a `1` to user identifiers and a `2` to movie identifiers.
   4. Create `is_left` property. Users are left, movies are not left (right).
  2. Initialize PGX.
  3. Compile the Matrix Factorization Gradient Descent algorithm.
  4. Load the MovieLens graph.
  5. Run the compiled algorithm on the MovieLens graph.
  6. Print the feature vectors for the first 10 vertices.
