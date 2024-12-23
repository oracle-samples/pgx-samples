# Degree Centrality

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k7c_degree_centrality
- **Time Complexity:** O(V) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#degreeCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#degreeCentrality_oracle_pgx_api_PgxGraph_)
  - [Analyst#degreeCentrality(PgxGraph graph, VertexProperty<ID,​java.lang.Integer> dc)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#degreeCentrality_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

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
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.ControlFlow;

@GraphAlgorithm
public class DegreeCentrality {
  public void degreeCentrality(PgxGraph g, @Out VertexProperty<Integer> degreeCentrality) {
    long numberOfStepsEstimatedForCompletion = g.getNumVertices();
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);
    g.getVertices().forEach(n ->
        degreeCentrality.set(n, (int) (n.getOutDegree() + n.getInDegree()))
    );
  }
}
```
