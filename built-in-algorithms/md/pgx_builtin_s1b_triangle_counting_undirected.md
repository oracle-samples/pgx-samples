# Triangle Counting (undirected)

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s1b_triangle_counting_undirected
- **Time Complexity:** O(E ^ 1.5) with E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#countTriangles(PgxGraph graph, boolean sortVerticesByDegree)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#countTriangles-oracle.pgx.api.PgxGraph-boolean-)

This algorithm is intended for undirected graphs and will count all the existing triangles on it. If the graph is a directed one, the algorithm will not count correctly the triangles in it.


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
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;

@GraphAlgorithm
public class TriangleCountingUndirected {
  public long triangleCounting(PgxGraph g) {
    Scalar<Long> t = Scalar.create(0L);

    g.getVertices().forEach(u -> u.getNeighbors().filter(v -> v.greaterThan(u)).forEach(v -> {
      u.getNeighbors().filter(w -> w.greaterThan(v)).forEach(w -> {
        if (v.hasEdgeTo(w)) {
          t.increment();
        }
      });
    }));

    return t.get();
  }
}
```
