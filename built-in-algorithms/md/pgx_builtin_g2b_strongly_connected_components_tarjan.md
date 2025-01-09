# Strongly Connected Components (SCC) via Tarjan's algorithm

- **Category:** connected components
- **Algorithm ID:** pgx_builtin_g2b_strongly_connected_components_tarjan
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(5 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#sccTarjan(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#sccTarjan_oracle_pgx_api_PgxGraph_)
  - [Analyst#sccTarjan(PgxGraph graph, VertexProperty<ID,java.lang.Long> partitionDistribution)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#sccTarjan_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

Tarjan's algorithm works on directed graphs for finding strongly connected components (SCC). A SCC is a maximal subset of vertices of the graph with the particular characteristic that every vertex in the SCC can be reachable from any other other vertex in the SCC.

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
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSequence;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.inDFS;

@GraphAlgorithm
public class Tarjan {
  public long tarjan(PgxGraph g, @Out VertexProperty<Long> scc) {
    VertexProperty<Boolean> inStack = VertexProperty.create();
    VertexProperty<Long> lowLink = VertexProperty.create();
    VertexProperty<Long> index = VertexProperty.create();
    VertexSequence stack = VertexSequence.create();

    // sequentially initialize, otherwise compiler flags this algorithm as parallel in nature
    g.getVertices().forSequential(n -> {
      scc.set(n, -1L);
      inStack.set(n, false);
      index.set(n, -1L);
    });

    Scalar<Long> numScc = Scalar.create(0L);

    // DFS
    g.getVertices().filter(n -> scc.get(n) == -1).forSequential(n -> {
      Scalar<Long> dfsIndex = Scalar.create(0L);

      inDFS(g, n)
          .navigator(t -> index.get(t) == -1)
          .forward(t -> {
            stack.pushBack(t);
            inStack.set(t, true);
            lowLink.set(t, dfsIndex.get());
            index.set(t, dfsIndex.get());
            dfsIndex.increment();
          })
          .backward(t -> {
            t.getOutNeighbors().filter(k -> scc.get(k) == -1).forSequential(k -> {
              lowLink.reduceMin(t, lowLink.get(k));
            });

            if (lowLink.get(t) == index.get(t)) {
              PgxVertex w = stack.popBack();
              while (w != t) {
                inStack.set(w, false);
                scc.set(w, numScc.get());
                w = stack.popBack();
              }
              inStack.set(w, false);
              scc.set(w, numScc.get());
              numScc.increment();
            }
          });
    });

    return numScc.get();
  }
}
```
