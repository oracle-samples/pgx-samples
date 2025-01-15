# ArticleRank

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k10_article_rank
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#articleRank(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#articleRank_oracle_pgx_api_PgxGraph_)
  - [Analyst#articleRank(PgxGraph graph, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#articleRank_oracle_pgx_api_PgxGraph_boolean_)
  - [Analyst#articleRank(PgxGraph graph, double e, double d, int max)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#articleRank_oracle_pgx_api_PgxGraph_double_double_int_)
  - [Analyst#articleRank(PgxGraph graph, double e, double d, int max, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#articleRank_oracle_pgx_api_PgxGraph_double_double_int_boolean_)
  - [Analyst#articleRank(PgxGraph graph, double e, double d, int max, boolean norm, VertexProperty rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#articleRank_oracle_pgx_api_PgxGraph_double_double_int_boolean_oracle_pgx_api_VertexProperty_)
  - [Analyst#articleRank(PgxGraph graph, double e, double d, int max, VertexProperty rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#articleRank_oracle_pgx_api_PgxGraph_double_double_int_oracle_pgx_api_VertexProperty_)

ArticleRank is a variant of the PageRank algorithm and operates in a similar way. It computes the ranking score for the vertices by analyzing the incoming edges, while reducing the assumption that relationships with nodes that have a low out-degree are of higher importance.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `damp` | double | damping factor. |
| `max_iter` | int | maximum number of iterations that will be performed. |
| `norm` | boolean | boolean flag to determine whether the algorithm will take into account dangling vertices for the ranking scores. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `rank` | vertexProp | vertex property holding the (normalized) ArticleRank value for each vertex (a value between 0 and 1). |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class ArticleRank {
  public void articleRank(PgxGraph g, double tol, double damp, int maxIter, boolean norm,
      @Out VertexProperty<Double> rank) {
    Scalar<Double> diff = Scalar.create();
    int cnt = 0;
    double n = g.getNumVertices();
    double avgOutDegree = g.getVertices().avg(w -> w.getOutDegree());
    rank.setAll(1 / n);

    do {
      diff.set(0.0);
      Scalar<Double> danglingFactor = Scalar.create(0d);
      if (norm) {
        danglingFactor.set(damp / n * g.getVertices().filter(v -> v.getOutDegree() == 0).sum(rank::get));
      }

      g.getVertices().forEach(t -> {
        double inSum = t.getInNeighbors().sum(w -> rank.get(w) / (w.getOutDegree() + avgOutDegree));
        double val = (1 - damp) / n + damp * inSum + danglingFactor.get();
        diff.reduceAdd(Math.abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);
  }
}
```
