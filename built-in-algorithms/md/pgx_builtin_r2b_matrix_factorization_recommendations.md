# Estimate Rating

- **Category:** matrix factorization
- **Algorithm ID:** pgx_builtin_r2b_matrix_factorization_recommendations
- **Time Complexity:** O(V) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#(BipartiteGraph graph, PgxVertex<ID> user, int vector_length, VertexProperty<ID,PgxVect<java.lang.Double>> feature, VertexProperty<ID,java.lang.Double> estimated_rating)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationRecommendations-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.PgxVertex-int-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexProperty-)

This algorithm is a complement for Matrix Factorization, thus it needs a bipartite graph and the generated feature vectors from such algorithm. The generated feature vectors will be used for making predictions in cases where the given user vertex has not been related to a particular item from the item set. Similarly to the recommendations from matrix factorization, this algorithm will perform dot products between the given user vertex and the rest of vertices in the graph, giving a score of 0 to the items that are already related to the user and to the products with other user vertices, hence returning the results of the dot products for the unrelated item vertices. The scores from those dot products can be interpreted as the predicted scores for the unrelated items given a particular user vertex.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `user` | node | vertex from the left (user) side of the graph. |
| `is_left` | vertexProp<node> | boolean vertex property stating the side of the vertices in the bipartite graph (left for users, right for items). |
| `vector_length` | int | size of the feature vectors. |
| `feature` | vertexProp<vect<double>[vector_length]> | vertex property holding the feature vectors for each vertex. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `estimated_rating` | vertexProp<vect<double>[vector_length]> | vertex property holding the estimated rating score for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVect;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Length;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class EstimateRating {
  public void computeEstimatedRating(PgxGraph g, PgxVertex user, VertexProperty<Boolean> isLeft, int vectorLength,
      VertexProperty<@Length("vectorLength") PgxVect<Double>> feature, @Out VertexProperty<Double> estimatedRating) {
    g.getVertices().forEach(n -> {
      if (isLeft.get(n)) {
        estimatedRating.set(n, 0d);
      } else if (n.hasEdgeFrom(user)) {
        // already seen the movie
        estimatedRating.set(n, 0d);
      } else {
        // inner product of two feature vectors
        estimatedRating.set(n, feature.get(user).multiply(feature.get(n)));
      }
    });
  }
}
```
