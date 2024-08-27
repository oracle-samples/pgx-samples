# Adamic-Adar index (Ignoring edge direction)

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s2d_adamic_adar_counting
- **Time Complexity:** O(E) with E = number of edges
- **Space Requirement:** O(E) with E = number of edges
- **Javadoc:** 
  - [Analyst#adamicAdarCounting(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#adamicAdarCounting-oracle.pgx.api.PgxGraph-)
  - [Analyst#adamicAdarCounting(PgxGraph graph, EdgeProperty aa)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#adamicAdarCounting-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-)

The Adamic-Adar index is meant for undirected graphs, since it is computed using the degree of the shared neighbors by two vertices in the graph. This implementation is intended for directed graphs but ignores edge direction to interpret the graph as undirected. It computes the index for every pair of vertices connected by an edge and associates it with that edge.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `adamic_adar` | edgeProp<double> | edge property holding the Adamic-Adar index of each edge in the graph. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.log10;

@GraphAlgorithm
public class AdamicAdarDirected {
  public void adamicAdarDirected(PgxGraph g, @Out EdgeProperty<Double> aa) {
    VertexProperty<Double> index = VertexProperty.create();

    g.getVertices().forEach(v -> {
      double filter = v.getInNeighbors().filter(v1 -> (v1.hasEdgeFrom(v) && v.hasEdgeFrom(v1))).sum(h -> 1);
      index.set(v, 1 / log10(v.getInDegree() + v.getDegree() - filter));
    });

    g.getEdges()
        .forEach(e -> {
          PgxVertex src = e.sourceVertex();
          PgxVertex dst = e.destinationVertex();

          double valueOut = src.getOutNeighbors()
              .filter(n -> (n.hasEdgeFrom(dst) || dst.hasEdgeFrom(n)))
              .sum(n -> index.get(n));

          double valueIn = src.getInNeighbors()
              .filter(n -> (!n.hasEdgeFrom(src) && (n.hasEdgeFrom(dst) || dst.hasEdgeFrom(n))))
              .sum(n -> index.get(n));

          aa.set(e, valueIn + valueOut);
        });
  }
}
```
