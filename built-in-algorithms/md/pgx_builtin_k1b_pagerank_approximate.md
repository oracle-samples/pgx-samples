# Approximate PageRank

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k1b_pagerank_approximate
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#pagerankApproximate(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#pagerankApproximate_oracle_pgx_api_PgxGraph_)
  - [Analyst#pagerankApproximate(PgxGraph graph, double e, double d, int max)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#pagerankApproximate_oracle_pgx_api_PgxGraph_double_double_int_)
  - [Analyst#pagerankApproximate(PgxGraph graph, double e, double d, int max, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#pagerankApproximate_oracle_pgx_api_PgxGraph_double_double_int_oracle_pgx_api_VertexProperty_)
  - [Analyst#pagerankApproximate(PgxGraph graph, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#pagerankApproximate_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

This variant of the PageRank algorithm computes the ranking scores for the vertices in similar way to the classic algorithm without normalization and with a more relaxed convergence criteria, since the tolerated error value is compared against each single vertex in the graph, instead of looking at the cumulative vertex error. Thus this variant will converge faster than the classic algorithm, but the ranking values might not be as accurate as in the classic implementation.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `damp` | double | damping factor. |
| `max_iter` | int | maximum number of iterations that will be performed. |

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
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.ControlFlow;

@GraphAlgorithm
public class PagerankApproximate {
  public void approximatePagerank(PgxGraph g, double tol, double damp, int maxIter, @Out VertexProperty<Double> rank) {
    VertexProperty<Boolean> active = VertexProperty.create();
    VertexProperty<Double> myDelta = VertexProperty.create();
    VertexProperty<Double> newDelta = VertexProperty.create();

    double initialRankValue = 1.0 / g.getNumVertices();
    long numberOfStepsEstimatedForCompletion = g.getNumVertices() * (maxIter * 2 + 4) + maxIter;
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    // initialize
    Scalar<Boolean> nodesActive = Scalar.create(true);
    Scalar<Integer> iteration = Scalar.create(0);
    active.setAll(true);
    myDelta.setAll(initialRankValue);
    newDelta.setAll(0.0);
    rank.setAll(initialRankValue);

    // push
    while (nodesActive.get() && iteration.get() < maxIter) {
      nodesActive.set(false);

      // push
      g.getVertices().filter(active).forEach(n ->
          n.getOutNeighbors().forEach(k -> newDelta.reduceAdd(k, myDelta.get(n) / n.getOutDegree()))
      );

      // consolidate
      g.getVertices().forEach(n -> {
        double newPageRank;
        double normalizedDelta;

        if (iteration.get() == 0) { // first iteration needs special handling
          newPageRank = initialRankValue * (1 - damp) + damp * newDelta.get(n);
          normalizedDelta = newPageRank - initialRankValue;
        } else {
          newPageRank = rank.get(n) + damp * newDelta.get(n);
          normalizedDelta = damp * newDelta.get(n);
        }

        rank.set(n, newPageRank);
        myDelta.set(n, normalizedDelta);

        if (normalizedDelta < 0) {
          normalizedDelta = -1 * normalizedDelta;
        }

        if (normalizedDelta < tol) {
          active.set(n, false);
        } else {
          active.set(n, true);
          nodesActive.reduceOr(true);
        }
        newDelta.set(n, 0d);
      });
      iteration.increment();
    }
  }
}
```
