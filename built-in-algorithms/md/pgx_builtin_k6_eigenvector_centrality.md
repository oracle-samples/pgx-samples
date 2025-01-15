# Eigenvector Centrality

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k6_eigenvector_centrality
- **Time Complexity:** O(V * k) with V = number of vertices, k <= maximum number of iterations
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#eigenvectorCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#eigenvectorCentrality_oracle_pgx_api_PgxGraph_)
  - [Analyst#eigenvectorCentrality(PgxGraph graph, int max, double maxDiff, boolean useL2Norm, boolean useInEdge)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#eigenvectorCentrality_oracle_pgx_api_PgxGraph_int_double_boolean_boolean_)
  - [Analyst#eigenvectorCentrality(PgxGraph graph, int max, double maxDiff, boolean useL2Norm, boolean useInEdge, VertexProperty<ID,java.lang.Double> ec)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#eigenvectorCentrality_oracle_pgx_api_PgxGraph_int_double_boolean_boolean_oracle_pgx_api_VertexProperty_)
  - [Analyst#eigenvectorCentrality(PgxGraph graph, VertexProperty<ID,java.lang.Double> ec)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#eigenvectorCentrality_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

The Eigenvector Centrality determines the centrality of a vertex by adding and weighting the centrality of its neighbors. Using outgoing or incoming edges when computing the eigenvector centrality will be equivalent to do so with the normal or the transpose adjacency matrix, respectively leading to the "right" and "left" eigenvectors.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `max_iter` | int | maximum number of iterations that will be performed. |
| `max_diff` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `use_l2norm` | boolean | boolean flag to determine whether the algorithm will use the l2 norm (Euclidean norm) or the l1 norm (absolute value) to normalize the centrality scores. |
| `use_inEdges` | boolean | boolean flag to determine whether the algorithm will use the incoming or the outgoing edges in the graph for the computations. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `ec` | vertexProp<double> | vertex property holding the normalized centrality value for each vertex. |

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

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

@GraphAlgorithm
public class EigenvectorCentrality {
  public void eigenvectorCentrality(PgxGraph g, int maxIter, double maxDiff, boolean useL2Norm, boolean useInedges,
      @Out VertexProperty<Double> ec) {
    VertexProperty<Double> ecUnnormal = VertexProperty.create();

    // Initialize
    ec.setAll(1.0 / (double) g.getNumVertices());

    int iter = 0;
    Scalar<Double> diff = Scalar.create();
    diff.set(0.0);

    do {
      // compute unnormalized sum
      g.getVertices().forEach(n -> {
        if (useInedges) {
          ecUnnormal.set(n, n.getInNeighbors().sum(ec));
        } else {
          ecUnnormal.set(n, n.getOutNeighbors().sum(ec));
        }
      });

      double s;
      if (useL2Norm) {
        // L2 Norm Normalization
        double l2Sum = g.getVertices().sum(n -> ecUnnormal.get(n) * ecUnnormal.get(n));
        s = (l2Sum == 0) ? 1.0 : 1 / sqrt(l2Sum);
      } else {
        // L1 Norm Normalization
        double l1Sum = g.getVertices().sum(n -> abs(ecUnnormal.get(n)));
        s = (l1Sum == 0) ? 1.0 : 1 / (l1Sum);
      }

      // update for next step
      diff.set(0.0);
      g.getVertices().forEach(n -> {
        double val = ecUnnormal.get(n) * s;
        diff.reduceAdd(abs(ec.get(n) - val));
        ec.set(n, val);
      });
      iter++;
    } while ((iter < maxIter) && (diff.get() > maxDiff));
  }
}
```
