# Degree Centrality

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k7c_degree_centrality
- **Time Complexity:** O(V) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#degreeCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#degreeCentrality-oracle.pgx.api.PgxGraph-)
  - [Analyst#degreeCentrality-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#degreeCentrality(PgxGraph graph, VertexProperty<ID,java.lang.Integer> dc))

Degree centrality counts the number of outgoing and incoming edges for each vertex in the graph.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `dc` | vertexProp<int> | vertex property holding the degree centrality value for each vertex in the graph. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class DegreeCentrality {
  public void degreeCentrality(PgxGraph g, @Out VertexProperty<Integer> degreeCentrality) {
    g.getVertices().forEach(n ->
        degreeCentrality.set(n, (int) (n.getOutDegree() + n.getInDegree()))
    );
  }
}
```
