# Classic PageRank

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k1a_pagerank
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#pagerank(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#pagerank-oracle.pgx.api.PgxGraph-)
  - [Analyst#pagerank(PgxGraph graph, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#pagerank-oracle.pgx.api.PgxGraph-boolean-)
  - [Analyst#pagerank(PgxGraph graph, boolean norm, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#pagerank-oracle.pgx.api.PgxGraph-boolean-oracle.pgx.api.VertexProperty-)
  - [Analyst#pagerank(PgxGraph graph, double e, double d, int max)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#pagerank-oracle.pgx.api.PgxGraph-double-double-int-)
  - [Analyst#pagerank(PgxGraph graph, double e, double d, int max, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#pagerank-oracle.pgx.api.PgxGraph-double-double-int-boolean-)
  - [Analyst#pagerank(PgxGraph graph, double e, double d, int max, boolean norm, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#pagerank-oracle.pgx.api.PgxGraph-double-double-int-boolean-oracle.pgx.api.VertexProperty-)
  - [Analyst#pagerank(PgxGraph graph, double e, double d, int max, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#pagerank-oracle.pgx.api.PgxGraph-double-double-int-oracle.pgx.api.VertexProperty-)
  - [Analyst#pagerank(PgxGraph graph, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#pagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

PageRank is an algorithm that computes ranking scores for the vertices using the network created by the incoming edges in the graph. Thus it is intended for directed graphs, although undirected graphs can be treated as well by converting them into directed graphs with reciprocated edges (i.e. keeping the original edge and creating a second one going in the opposite direction). The edges on the graph will define the relevance of each vertex in the graph, reflecting this on the scores, meaning that greater scores will correspond to vertices with greater relevance.


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

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class Pagerank {
  public void pagerank(PgxGraph g, double tol, double damp, int maxIter, boolean norm,
      @Out VertexProperty<Double> rank) {
    Scalar<Double> diff = Scalar.create();
    int cnt = 0;
    double n = g.getNumVertices();

    rank.setAll(1 / n);
    do {
      diff.set(0.0);
      Scalar<Double> danglingFactor = Scalar.create(0d);

      if (norm) {
        danglingFactor.set(damp / n * g.getVertices().filter(v -> v.getOutDegree() == 0).sum(rank::get));
      }

      g.getVertices().forEach(t -> {
        double inSum = t.getInNeighbors().sum(w -> rank.get(w) / w.getOutDegree());
        double val = (1 - damp) / n + damp * inSum + danglingFactor.get();
        diff.reduceAdd(Math.abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);
  }
}
```
