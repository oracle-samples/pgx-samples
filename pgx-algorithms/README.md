# Implementing Graph Algorithms using PGX Algorithm API

## Table of Contents:

1. [Overview](#overview)
3. [Usage](#usage)

## Overview <a name="overview"></a>

PGX Algorithm allows you to write your graph algorithm in Java and have it automatically compiled
to an efficient parallel implementation targeting either the single-machine (shared-memory) or
distributed runtime.

An implementation of the pagerank algorithm can be found at `src/main/resources/Pagerank.java`.
This project contains a single class `oracle.pgx.algorithms.Main` which compiles and runs this
algorithm on a sample graph.

## Usage <a name="usage"></a>

Run `gradle run` to run the `oracle.pgx.algorithms.Main` class. This class does the following:

1. Initialize PGX.
2. Compile the Pagerank algorithm.
3. Run the compiled Pagerank algorithm on a sample graph (V = 8, E = 6).
4. Print the pageranks for the eight vertices.
