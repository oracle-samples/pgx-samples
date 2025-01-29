# Weighted PageRank

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k1c_pagerank_weighted
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#weightedPagerank(PgxGraph graph, boolean norm, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#weightedPagerank_oracle_pgx_api_PgxGraph_boolean_oracle_pgx_api_EdgeProperty_)
  - [Analyst#weightedPagerank(PgxGraph graph, boolean norm, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#weightedPagerank_oracle_pgx_api_PgxGraph_boolean_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#weightedPagerank(PgxGraph graph, double e, double d, int max, boolean norm, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#weightedPagerank_oracle_pgx_api_PgxGraph_double_double_int_boolean_oracle_pgx_api_EdgeProperty_)
  - [Analyst#weightedPagerank(PgxGraph graph, double e, double d, int max, boolean norm, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#weightedPagerank_oracle_pgx_api_PgxGraph_double_double_int_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#weightedPagerank(PgxGraph graph, double e, double d, int max, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#weightedPagerank_oracle_pgx_api_PgxGraph_double_double_int_oracle_pgx_api_EdgeProperty_)
  - [Analyst#weightedPagerank(PgxGraph graph, double e, double d, int max, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#weightedPagerank_oracle_pgx_api_PgxGraph_double_double_int_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#weightedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#weightedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_EdgeProperty_)
  - [Analyst#weightedPagerank(PgxGraph graph, double e, double d, int max, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#weightedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)

The Weighted PageRank works like the original PageRank algorithm, except that it allows for a weight value assigned to each edge. This weight determines the fraction of the PageRank score that will flow from the source vertex through the current edge to its destination vertex.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
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
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.ControlFlow;

import static java.lang.Math.abs;

@GraphAlgorithm
public class PagerankWeighted {
  public void pagerankWeighted(PgxGraph g, double tol, double damp, int maxIter, boolean norm,
      EdgeProperty<Double> weight, @Out VertexProperty<Double> rank) {
    double numVertices = g.getNumVertices();
    long numberOfStepsEstimatedForCompletion = g.getNumVertices() * (maxIter * 2 + 2) + maxIter;
    if (norm) {
      numberOfStepsEstimatedForCompletion += g.getNumVertices() * maxIter;
    }
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    rank.setAll(1.0 / numVertices);

    VertexProperty<Double> weightSum = VertexProperty.create();
    g.getVertices().forEach(n -> {
      weightSum.set(n, n.getOutEdges().sum(weight));
    });

    Scalar<Double> diff = Scalar.create();
    int cnt = 0;

    do {
      diff.set(0d);
      Scalar<Double> danglingFactor = Scalar.create();

      if (norm) {
        danglingFactor.set(damp / numVertices * g.getVertices().filter(v -> v.getOutDegree() == 0).sum(rank));
      }

      g.getVertices().forEach(t -> {
        Scalar<Double> s = Scalar.create(0d);
        t.getInNeighbors().forEach(src -> {
          PgxEdge in = src.edge();
          s.reduceAdd(rank.get(src) * weight.get(in) / weightSum.get(src));
        });
        double val = (1 - damp) / numVertices + damp * s.get() + danglingFactor.get();
        diff.reduceAdd(abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);
  }
}
```
