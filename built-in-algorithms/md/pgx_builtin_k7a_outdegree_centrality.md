# Out-Degree Centrality

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k7a_outdegree_centrality
- **Time Complexity:** O(V) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#outDegreeCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#outDegreeCentrality-oracle.pgx.api.PgxGraph-)
  - [Analyst#outDegreeCentrality(PgxGraph graph, VertexProperty<ID,java.lang.Integer> dc)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#outDegreeCentrality-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

Out-Degree centrality returns the sum of the number of outgoing edges for each vertex in the graph.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `oc` | vertexProp<int> | vertex property holding the degree centrality value for each vertex in the graph. |

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
public class OutdegreeCentrality {
  public void outdegreeCentrality(PgxGraph g, @Out VertexProperty<Integer> outdegreeCentrality) {
    long numberOfStepsEstimatedForCompletion = g.getNumVertices();
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);
    g.getVertices().forEach(n ->
        outdegreeCentrality.set(n, (int) n.getOutDegree())
    );
  }
}
```
