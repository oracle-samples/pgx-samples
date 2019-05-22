# Movie Recommendation

## Introduction

<!-- TODO: Copy from old documentation -->

## Usage

The Movie Recommendation example operates on the [MovieLens](https://grouplens.org/datasets/movielens/) data set.
To run the example:

1. Download and install [Gradle](https://gradle.org/install/) (version 5.2.1 or higher).
2. Download [ml-latest-small.zip](http://files.grouplens.org/datasets/movielens/ml-latest-small.zip) from the [MovieLens datasets](https://grouplens.org/datasets/movielens/) page. <!-- `wget -O /tmp/ml-latest-small.zip http://files.grouplens.org/datasets/movielens/ml-latest-small.zip` -->
3. Extract `ml-latest-small.zip` to some writable directory `/tmp/foo/bar`. <!-- `unzip /tmp/ml-latest-small.zip -d /tmp/` -->
4. Run `gradle run -Pdata="/tmp/foo/bar"` for training a model and testing it. Or run `gradle run -Pdata="/tmp/foo/bar" -PuserID=1 -PtopK=10` for training, testing and generate recommendations, where `-PuserID` must be a valid user ID from the `userId` column of the `ratings.csv` file in the dataset, and `-PtopK` is the number of ranked and recommended movies to show as example. <!-- gradle run -Pdata="/tmp/ml-latest-small" -PuserID=1 -PtopK=10 -->

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
