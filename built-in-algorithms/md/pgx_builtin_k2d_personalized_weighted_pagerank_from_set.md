# Personalized Weighted PageRank (for a set of vertices)

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k2d_personalized_weighted_pagerank_from_set
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, VertexSet<ID> vertices, boolean norm, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-boolean-oracle.pgx.api.EdgeProperty-)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, VertexSet<ID> vertices, boolean norm, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-boolean-oracle.pgx.api.EdgeProperty-oracle.pgx.api.VertexProperty-)
  - [Analyst#personalizedWeightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-double-double-int-boolean-oracle.pgx.api.EdgeProperty-](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, boolean norm, EdgeProperty<java.lang.Double> weight))
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, boolean norm, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-double-double-int-boolean-oracle.pgx.api.EdgeProperty-oracle.pgx.api.VertexProperty-)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-double-double-int-oracle.pgx.api.EdgeProperty-)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-double-double-int-oracle.pgx.api.EdgeProperty-oracle.pgx.api.VertexProperty-)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, VertexSet<ID> vertices, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-oracle.pgx.api.EdgeProperty-)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, VertexSet<ID> vertices, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-oracle.pgx.api.EdgeProperty-oracle.pgx.api.VertexProperty-)

The Personalized Weighted Pagerank combines elements from the weighted and the personalized versions in order to make the personalization of the results more unique, since both: the selection of a subset of vertices and the inclusion of specific weights in the edges, will help to set the importance of the ranking scores when these are being computed.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `source` | nodeSet | the set of chosen vertices from the graph for personalization. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `damp` | double | damping factor. |
| `max_iter` | int | maximum number of iterations that will be performed. |
| `norm` | boolean | boolean flag to determine whether the algorithm will take into account dangling vertices for the ranking scores. |
| `weight` | edgeProp<double> | edge property holding the weight of each edge in the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `rank` | vertexProp<double> | vertex property holding the (normalized) PageRank value for each vertex (a value between 0 and 1). |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.abs;

@GraphAlgorithm
public class PagerankPersonalizedWeightedSet {
  public void pagerankPersonalizedWeightedSet(PgxGraph g, VertexSet source, double tol, double damp, int maxIter,
      boolean norm, EdgeProperty<Double> weight, @Out VertexProperty<Double> rank) {
    double numVertices = g.getNumVertices();
    double m = source.size();

    VertexProperty<Double> weightSum = VertexProperty.create();
    g.getVertices().forEach(n -> weightSum.set(n, n.getOutEdges().sum(weight)));

    VertexProperty<Boolean> isStart = VertexProperty.create();
    rank.setAll(0d);
    isStart.setAll(false);

    source.forEach(n -> {
      rank.set(n, 1.0 / m);
      isStart.set(n, true);
    });

    Scalar<Double> diff = Scalar.create();
    int cnt = 0;

    do {
      diff.set(0.0);
      Scalar<Double> danglingFactor = Scalar.create(0d);

      if (norm) {
        danglingFactor.set(damp / numVertices * g.getVertices().filter(n -> n.getOutDegree() == 0).sum(rank));
      }

      g.getVertices().forEach(t -> {
        double val1 = isStart.get(t) ? (1 - damp) : 0;
        Scalar<Double> val2 = Scalar.create(0d);
        t.getInNeighbors().forEach(src -> {
          PgxEdge in = src.edge();
          val2.reduceAdd(rank.get(src) * weight.get(in) / weightSum.get(src));
        });
        double val = val1 + damp * val2.get() + danglingFactor.get();
        diff.reduceAdd(abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);

    g.getVertices().forEach(n -> rank.set(n, rank.get(n) / m));
  }
}
```
