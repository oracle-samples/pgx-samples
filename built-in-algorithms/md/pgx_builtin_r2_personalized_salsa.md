# Personalized SALSA

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_r2_personalized_salsa
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#personalizedSalsa(BipartiteGraph graph, PgxVertex<ID> v)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedSalsa_oracle_pgx_api_BipartiteGraph_oracle_pgx_api_PgxVertex_)
  - [Analyst#personalizedSalsa(BipartiteGraph graph, PgxVertex<ID> v, double d, int maxIter, double maxDiff)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedSalsa_oracle_pgx_api_BipartiteGraph_oracle_pgx_api_PgxVertex_double_int_double_)
  - [Analyst#personalizedSalsa(BipartiteGraph graph, PgxVertex<ID> v, double d, int maxIter, double maxDiff, VertexProperty<ID,java.lang.Double> salsaRank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedSalsa_oracle_pgx_api_BipartiteGraph_oracle_pgx_api_PgxVertex_double_int_double_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedSalsa(BipartiteGraph graph, PgxVertex<ID> v, VertexProperty<ID,java.lang.Double> salsaRank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedSalsa_oracle_pgx_api_BipartiteGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_VertexProperty_)

This Personalized version of SALSA allows to select a particular vertex or set of vertices from the given graph in order to give them a greater importance when computing the ranking scores, which will have as result a personalized SALSA score and show relevant (or similar) vertices to the ones chosen for the personalization.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `h` | node | the chosen vertex from the graph for personalization. |
| `is_left` | vertexProp<bool> | boolean vertex property stating the side of the vertices in the [bipartite](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgdg/graph-mutation-and-subgraphs.html) graph (left for hubs, right for auths). |
| `damp` | double | damping factor to modulate the degree of personalization of the scores by the algorithm. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `max_iter` | int | maximum number of iterations that will be performed. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `rank` | vertexProp<double> | vertex property holding the normalized authority/hub ranking score for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.abs;

@GraphAlgorithm
public class SalsaPersonalized {
  public void personalizedSalsa(PgxGraph g, PgxVertex v, VertexProperty<Boolean> isLeft, double damp, double tol,
      int maxIter, @Out VertexProperty<Double> rank) {
    long numVertices = g.getNumVertices();

    if (numVertices == 0) {
      return;
    }

    VertexProperty<Double> deg = VertexProperty.create();
    Scalar<Double> diff = Scalar.create();
    int cnt = 0;

    deg.setAll(w -> (double) (isLeft.get(w) ? w.getOutDegree() : w.getInDegree()));
    long numHubs = g.getVertices().filter(isLeft).size();
    long numAuths = numVertices - numHubs;

    if (isLeft.get(v)) {
      rank.setAll(w -> isLeft.get(w) ? 0.0 : 1.0 / numAuths);
    } else {
      rank.setAll(w -> isLeft.get(w) ? 1.0 / numHubs : 0.0);
    }

    rank.set(v, 1.0);

    do {
      diff.set(0.0);
      g.getVertices().filter(n -> n.getOutDegree() + n.getInDegree() > 0).forEach(n -> {
        Scalar<Double> val = Scalar.create(0.0);
        if (isLeft.get(n)) {
          n.getOutNeighbors()
              .forEach(x -> val.reduceAdd(x.getInNeighbors().sum(w -> rank.get(w) / (deg.get(x) * deg.get(w)))));
          if (isLeft.get(v)) {
            val.set((n == v ? damp : 0.0) + (1.0 - damp) * val.get());
          }
        } else {
          n.getInNeighbors()
              .forEach(x -> val.reduceAdd(x.getOutNeighbors().sum(w -> rank.get(w) / (deg.get(x) * deg.get(w)))));
          if (!isLeft.get(v)) {
            val.set((n == v ? damp : 0.0) + (1.0 - damp) * val.get());
          }
        }
        diff.reduceAdd(abs(val.get() - rank.get(n)));
        rank.setDeferred(n, val.get());
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);
  }
}
```
