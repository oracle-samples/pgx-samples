# Closeness Centrality (with weights)

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k4b_closeness_centrality_double_length
- **Time Complexity:** O(V * E * d) with E = number of edges, V = number of vertices, d = diameter of the graph
- **Space Requirement:** O(5 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#closenessCentralityDoubleLength(PgxGraph graph, EdgeProperty<java.lang.Double> cost)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#closenessCentralityDoubleLength-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#closenessCentralityDoubleLength(PgxGraph graph, EdgeProperty<java.lang.Double> cost, VertexProperty<ID,java.lang.Double> cc)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#closenessCentralityDoubleLength-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-oracle.pgx.api.VertexProperty-)

This variant of Closeness Centrality takes into account the weights from the edges when computing the reciprocal of the sum of all the distances from the possible shortest paths starting from the vertex V, for every vertex in the graph. The weights of the edges must be positive values greater than 0.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `weight` | edgeProp<double> | edge property holding the weight of each edge in the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `cc` | vertexProp<double> | vertex property holding the closeness centrality value for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | bool | boolean flag that checks if the graph is connected. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Reduction.updateMinValue;
import static java.lang.Double.POSITIVE_INFINITY;

@GraphAlgorithm
public class ClosenessCentralityDoubleLength {
  public boolean closenessCentralityDoubleLength(PgxGraph g, EdgeProperty<Double> weight,
      @Out VertexProperty<Double> cc) {
    VertexProperty<Double> dist = VertexProperty.create(POSITIVE_INFINITY);

    Scalar<Boolean> disconnected = Scalar.create(false);

    // for every vertex in G
    g.getVertices().forSequential(root -> {
      // Run Bellman-Ford Algorithm
      bellmanFord(g, root, weight, dist);

      // Update cc
      boolean b = g.getVertices().anyMatch(v -> dist.get(v) == POSITIVE_INFINITY);
      double levelSum = g.getVertices().sum(dist);
      dist.setAll(POSITIVE_INFINITY);

      if (b) {
        cc.setAll(0d); // disconnected graph
        disconnected.set(true);
      } else {
        cc.set(root, 1.0 / levelSum);
      }
    });

    return !disconnected.get();
  }

  void bellmanFord(PgxGraph g, PgxVertex root, EdgeProperty<Double> weight, @Out VertexProperty<Double> dist) {
    VertexProperty<Boolean> updated = VertexProperty.create();
    VertexProperty<Boolean> updatedNxt = VertexProperty.create();
    VertexProperty<Double> distNxt = VertexProperty.create();

    dist.setAll(v -> (v == root) ? 0d : POSITIVE_INFINITY);
    updated.setAll(v -> v == root);
    distNxt.setAll(dist::get);
    updatedNxt.setAll(updated::get);
    boolean done = false;

    while (!done) {
      g.getVertices().filter(updated).forEach(n -> n.getNeighbors().forEach(s -> {
        PgxEdge e = s.edge(); // the edge to s
        // updatedNxt becomes true only if distNxt is
        // actually updated
        updateMinValue(s, distNxt, dist.get(n) + weight.get(e)).andUpdate(s, updatedNxt, true);
      }));

      dist.setAll(distNxt::get);
      updated.setAll(updatedNxt::get);
      updatedNxt.setAll(false);

      done = !g.getVertices().anyMatch(updated::get);
    }
  }
}
```
