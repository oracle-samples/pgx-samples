# Personalized SALSA (for a set of vertices)

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_r3_personalized_salsa_from_set
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#personalizedSalsa(BipartiteGraph graph, VertexSet<ID> vertices)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedSalsa-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.VertexSet-)
  - [Analyst#personalizedSalsa(BipartiteGraph graph, VertexSet<ID> vertices, double d, int maxIter, double maxDiff)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedSalsa-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.VertexSet-double-int-double-)
  - [Analyst#personalizedSalsa(BipartiteGraph graph, VertexSet<ID> vertices, double d, int maxIter, double maxDiff, VertexProperty<ID,java.lang.Double> salsaRank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedSalsa-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.VertexSet-double-int-double-oracle.pgx.api.VertexProperty-)
  - [Analyst#personalizedSalsa(BipartiteGraph graph, VertexSet<ID> vertices, VertexProperty<ID,java.lang.Double> salsaRank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedSalsa-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.VertexSet-oracle.pgx.api.VertexProperty-)

This Personalized version of SALSA allows to select a particular vertex or set of vertices from the given graph in order to give them a greater importance when computing the ranking scores, which will have as result a personalized SALSA score and show relevant (or similar) vertices to the ones chosen for the personalization.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `source` | nodeSet | the set of chosen vertices from the graph for personalization. |
| `is_left` | vertexProp<bool> | boolean vertex property stating the side of the vertices in the [bipartite](prog-guides/mutation-subgraph/subgraph.html#create-a-bipartite-subgraph-based-on-a-vertex-list) graph (left for hubs, right for auths). |
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
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.abs;

@GraphAlgorithm
public class SalsaPersonalizedFromSet {
  public void personalizedSalsaFromSet(PgxGraph g, VertexSet source, VertexProperty<Boolean> isLeft, double damp,
      double tol, int maxIter, @Out VertexProperty<Double> rank) {
    long numVertices = g.getNumVertices();
    long m = source.size();

    if (m == 0 || numVertices == 0) {
      return;
    }

    VertexProperty<Double> deg = VertexProperty.create();
    VertexProperty<Boolean> isStart = VertexProperty.create();

    Scalar<Double> diff = Scalar.create();
    int cnt = 0;

    deg.setAll(w -> (double) (isLeft.get(w) ? w.getOutDegree() : w.getInDegree()));
    long numHubs = g.getVertices().filter(isLeft).size();
    long numAuths = numVertices - numHubs;

    long sHub = source.filter(isLeft).size();
    long sAut = source.filter(n -> !isLeft.get(n)).size();

    if (sHub > 0 && sAut > 0) { // mixed personalization
      rank.setAll(0.0);
    } else if (sHub > 0 && sAut == 0) { // personalization for hubs
      rank.setAll(v -> isLeft.get(v) ? 0.0 : 1.0 / numAuths);
    } else if (sHub == 0 && sAut > 0) { // personalization for auths
      rank.setAll(v -> isLeft.get(v) ? 1.0 / numHubs : 0.0);
    }

    isStart.setAll(false);

    source.forEach(n -> {
      if (isLeft.get(n) && sHub > 0) {
        rank.set(n, 1.0 / sHub);
      } else if (!isLeft.get(n) && sAut > 0) {
        rank.set(n, 1.0 / sAut);
      }
      isStart.set(n, true);
    });

    do {
      diff.set(0.0);
      g.getVertices().filter(n -> n.getOutDegree() + n.getInDegree() > 0).forEach(n -> {
        Scalar<Double> val = Scalar.create(0.0);
        if (isLeft.get(n)) {
          n.getOutNeighbors()
              .forEach(v -> val.reduceAdd(v.getInNeighbors().sum(w -> rank.get(w) / (deg.get(v) * deg.get(w)))));
          if (sHub > 0) {
            val.set((isStart.get(n) ? damp : 0) + (1.0 - damp) * val.get());
          }
        } else {
          n.getInNeighbors()
              .forEach(v -> val.reduceAdd(v.getOutNeighbors().sum(w -> rank.get(w) / (deg.get(v) * deg.get(w)))));
          if (sAut > 0) {
            val.set((isStart.get(n) ? damp : 0) + (1.0 - damp) * val.get());
          }
        }
        diff.reduceAdd(abs(val.get() - rank.get(n)));
        rank.setDeferred(n, val.get());
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);

    g.getVertices().forEach(n -> {
      if (isLeft.get(n) && sHub > 0) {
        rank.set(n, rank.get(n) / sHub);
      } else if (!isLeft.get(n) && sAut > 0) {
        rank.set(n, rank.get(n) / sAut);
      }
    });
  }
}
```
