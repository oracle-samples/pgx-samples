# Personalized PageRank (for a set of vertices)

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k2b_personalized_pagerank_from_set
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_boolean_)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, boolean norm, VertexProperty<ID,​java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_boolean_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_double_double_int_)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_double_double_int_boolean_)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, boolean norm, VertexProperty<ID,​java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_double_double_int_boolean_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, VertexProperty<ID,​java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_double_double_int_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, VertexProperty<ID,​java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_oracle_pgx_api_VertexProperty_)

The Personalized Pagerank allows to select a particular vertex or a set of vertices from the given graph in order to give them a greater importance when computing the ranking score, which will have as result a personalized Pagerank score and reveal relevant (or similar) vertices to the ones chosen at the beginning.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `source` | nodeSet | the set of chosen vertices from the graph for personalization. |
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
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.ControlFlow;

import static java.lang.Math.abs;

@GraphAlgorithm
public class PagerankPersonalizedSet {
  public void personalizedPagerank(PgxGraph g, VertexSet source, double tol, double damp, int maxIter, boolean norm,
      @Out VertexProperty<Double> rank) {
    double numVertices = g.getNumVertices();
    long m = source.size();

    long numberOfStepsEstimatedForCompletion = g.getNumVertices() * (maxIter * 2 + 3) + maxIter + m;
    if (norm) {
      numberOfStepsEstimatedForCompletion += g.getNumVertices() * maxIter;
    }
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    Scalar<Double> diff = Scalar.create();
    int cnt = 0;

    VertexProperty<Boolean> isStart = VertexProperty.create(false);
    rank.setAll(0d);

    source.forEach(n -> {
      rank.set(n, 1.0 / m);
      isStart.set(n, true);
    });

    do {
      diff.set(0.0);
      Scalar<Double> danglingFactor = Scalar.create(0d);

      if (norm) {
        danglingFactor.set(damp / numVertices * g.getVertices().filter(n -> n.getOutDegree() == 0).sum(rank));
      }

      g.getVertices().forEach(t -> {
        double val1 = (isStart.get(t)) ? (1 - damp) : 0;
        double val2 = damp * t.getInNeighbors().sum(w -> rank.get(w) / w.getOutDegree());
        double val = val1 + val2 + danglingFactor.get();
        diff.reduceAdd(abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);

    g.getVertices().forEach(n -> rank.set(n, rank.get(n) / m));
  }
}
```
