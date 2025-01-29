# Strongly Connected Components (SCC) via Kosaraju's algorithm

- **Category:** connected components
- **Algorithm ID:** pgx_builtin_g2a_strongly_connected_components_kosaraju
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#sccKosaraju(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#sccKosaraju_oracle_pgx_api_PgxGraph_)
  - [Analyst#sccKosaraju(PgxGraph graph, VertexProperty<ID,java.lang.Long> partitionDistribution)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#sccKosaraju_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

Kosaraju's algorithm works on directed graphs for finding strongly connected components (SCC). A SCC is a maximal subset of vertices of the graph with the particular characteristic that every vertex in the SCC can be reachable from any other other vertex in the SCC.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `scc` | vertexProp<long> | vertex property holding the label of the SCC assigned to each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | long | the total number of SCC found in the graph. |

## Code

```java
/*
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSequence;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.Direction.IN_EDGES;
import static oracle.pgx.algorithm.Traversal.inBFS;
import static oracle.pgx.algorithm.Traversal.inDFS;

@GraphAlgorithm
public class Kosaraju {
  public long kosaraju(PgxGraph g, @Out VertexProperty<Long> scc) {
    scc.setAll(-1L);

    VertexProperty<Boolean> checked = VertexProperty.create();
    checked.setAll(false);

    // [Phase 1]
    // Obtain reverse-post-DFS-order of node sequence.
    // nodeOrder can be also used here but nodeSeq is faster
    VertexSequence queue = VertexSequence.create();
    g.getVertices().filter(t -> !checked.get(t)).forSequential(t ->
        inDFS(g, t)
            .navigator(n -> !checked.get(n))
            .backward(n -> {
              checked.set(n, true);
              queue.pushFront(n);
            })
    );

    // [Phase 2]
    // Starting from each vertex in the sequence
    // do BFS on the transposed graph g^.
    // and every vertices that are (newly) visited compose one SCC.
    Scalar<Long> compId = Scalar.create(0L);
    queue.filter(t -> scc.get(t) == -1).forSequential(t -> {
      inBFS(g, t)
          .navigator(n -> scc.get(n) == -1)
          .forward(n -> scc.set(n, compId.get()))
          .direction(IN_EDGES);

      compId.increment();
    });

    return compId.get();
  }
}
```
