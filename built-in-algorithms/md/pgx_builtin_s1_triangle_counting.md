# Triangle Counting

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s1_triangle_counting
- **Time Complexity:** O(E ^ 1.5) with E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#countTriangles(PgxGraph graph, boolean sortVerticesByDegree)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#countTriangles_oracle_pgx_api_PgxGraph_boolean_)

This algorithm is intended for directed graphs and will count all the existing triangles on it. To run the algorithm on undirected graphs, use the undirected version.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | long | returns the total number of triangles found. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;

@GraphAlgorithm
public class TriangleCounting {
  public long triangleCounting(PgxGraph g) {
    Scalar<Long> t = Scalar.create(0L);

    g.getVertices().forEach(u -> u.getInOutNeighbors().filter(v -> v.greaterThan(u)).forEach(v -> {
      u.getInOutNeighbors().filter(w -> w.greaterThan(v)).forEach(w -> {
        if (v.hasEdgeTo(w)) {
          t.increment();
        }
        if (v.hasEdgeFrom(w)) {
          t.increment();
        }
      });
    }));

    return t.get();
  }
}
```
