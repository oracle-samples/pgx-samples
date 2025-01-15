# Personalized Weighted PageRank

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k2c_personalized_weighted_pagerank
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, PgxVertex<ID> v, boolean norm, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_boolean_oracle_pgx_api_EdgeProperty_)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, PgxVertex<ID> v, boolean norm, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_boolean_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, PgxVertex v, double e, double d, int max, boolean norm, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank_oracle_pgx_api_PgxGraph_ID_java_math_BigDecimal_java_math_BigDecimal_int_boolean_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, PgxVertex<ID> v, double e, double d, int max, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank_oracle_pgx_api_PgxGraph_ID_java_math_BigDecimal_java_math_BigDecimal_int_oracle_pgx_api_EdgeProperty_)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, PgxVertex<ID> v, double e, double d, int max, boolean norm, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank_oracle_pgx_api_PgxGraph_ID_java_math_BigDecimal_java_math_BigDecimal_int_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, PgxVertex<ID> v, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_oracle_pgx_api_EdgeProperty_)
  - [Analyst#personalizedWeightedPagerank(PgxGraph graph, PgxVertex<ID> v, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#personalizedWeightedPagerank_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)

The Personalized Weighted Pagerank combines elements from the weighted and the personalized versions in order to make the personalization of the results more unique, since both: the selection of a subset of vertices and the inclusion of specific weights in the edges, will help to set the importance of the ranking scores when these are being computed.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `v` | node | the chosen vertex from the graph for personalization. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `damp` | double | damping factor. |
| `max_iter` | int | maximum number of iterations that will be performed. |
| `norm` | boolean | boolean flag to determine whether the algorithm will take into account dangling vertices for the ranking scores. |
| `weight` | edgeProp<double> | edge property holding the weight of each edge in the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `rank` | vertexProp<double> | vertex property holding the (normalized) weighted PageRank value for each vertex (a value between 0 and 1). |

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
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.ControlFlow;

import static java.lang.Math.abs;

@GraphAlgorithm
public class PagerankPersonalizedWeighted {
  public void pagerankPersonalizedWeighted(PgxGraph g, PgxVertex v, double tol, double damp, int maxIter, boolean norm,
      EdgeProperty<Double> weight, @Out VertexProperty<Double> rank) {
    double numVertices = g.getNumVertices();
    long numberOfStepsEstimatedForCompletion = g.getNumVertices() * (maxIter * 2 + 2) + maxIter;
    if (norm) {
      numberOfStepsEstimatedForCompletion += g.getNumVertices() * maxIter;
    }
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);
    rank.setAll(0d);
    rank.set(v, 1.0);

    VertexProperty<Double> weightSum = VertexProperty.create();
    g.getVertices().forEach(n -> {
      weightSum.set(n, n.getOutEdges().sum(weight));
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
        double val1 = (t == v) ? (1 - damp) : 0;
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
  }
}
```
