# Personalized PageRank

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k2_personalized_pagerank
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#personalizedPagerank(PgxGraph graph, PgxVertex<ID> v)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_)
  - [Analyst#personalizedPagerank(PgxGraph graph, PgxVertex<ID> v, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_boolean_)
  - [Analyst#personalizedPagerank(PgxGraph graph, PgxVertex<ID> v, boolean norm, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_boolean_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedPagerank(PgxGraph graph, PgxVertex<ID> v, double e, double d, int max)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_double_double_int_)
  - [Analyst#personalizedPagerank(PgxGraph graph, PgxVertex<ID> v, double e, double d, int max, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_double_double_int_boolean_)
  - [Analyst#personalizedPagerank(PgxGraph graph, PgxVertex<ID> v, double e, double d, int max, boolean norm, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_double_double_int_boolean_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedPagerank(PgxGraph graph, PgxVertex<ID> v, double e, double d, int max, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_double_double_int_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html)

The Personalized Pagerank allows to select a particular vertex or a set of vertices from the given graph in order to give them a greater importance when computing the ranking score, which will have as result a personalized Pagerank score and reveal relevant (or similar) vertices to the ones chosen at the beginning.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `v` | node | the chosen vertex from the graph for personalization. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `damp` | double | damping factor. |
| `max_iter` | int | maximum number of iterations that will be performed. |
| `norm` | boolean | boolean flag to determine whether the algorithm will take into account dangling vertices for the ranking scores. |

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

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.ControlFlow;

import static java.lang.Math.abs;

@GraphAlgorithm
public class PagerankPersonalized {
  public void personalizedPagerank(PgxGraph g, PgxVertex v, double tol, double damp, int maxIter, boolean norm,
      @Out VertexProperty<Double> rank) {
    double numVertices = g.getNumVertices();
    Scalar<Double> diff = Scalar.create();
    int cnt = 0;
    long numberOfStepsEstimatedForCompletion = g.getNumVertices() * (maxIter * 2 + 1) + maxIter;
    if (norm) {
      numberOfStepsEstimatedForCompletion += g.getNumVertices() * maxIter;
    }
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);
    rank.setAll(0d);
    rank.set(v, 1.0);

    do {
      diff.set(0.0);
      Scalar<Double> danglingFactor = Scalar.create(0d);

      if (norm) {
        danglingFactor.set(damp / numVertices * g.getVertices().filter(n -> n.getOutDegree() == 0).sum(rank));
      }

      g.getVertices().forEach(t -> {
        double val1 = (t == v) ? (1 - damp) : 0;
        double val2 = damp * t.getInNeighbors().sum(w -> rank.get(w) / w.getOutDegree());
        double val = val1 + val2 + danglingFactor.get();
        diff.reduceAdd(abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);
  }
}
```
