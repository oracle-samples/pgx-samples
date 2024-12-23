# In-Degree Centrality

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k7b_indegree_centrality
- **Time Complexity:** O(V) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#inDegreeCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#inDegreeCentrality_oracle_pgx_api_PgxGraph_)
  - [Analyst#inDegreeCentrality(PgxGraph graph, VertexProperty<ID,java.lang.Integer> dc)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#inDegreeCentrality_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

In-Degree centrality returns the sum of the number of incoming edges for each vertex in the graph.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `ic` | vertexProp<int> | vertex property holding the degree centrality value for each vertex in the graph. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void |  |

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
public class IndegreeCentrality {
  public void indegreeCentrality(PgxGraph g, @Out VertexProperty<Integer> indegreeCentrality) {
    long numberOfStepsEstimatedForCompletion = g.getNumVertices();
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);
    g.getVertices().forEach(n ->
        indegreeCentrality.set(n, (int) n.getInDegree())
    );
  }
}
```
