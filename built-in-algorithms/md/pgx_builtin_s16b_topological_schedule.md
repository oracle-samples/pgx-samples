# Topological Schedule

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s16b_topological_schedule
- **Time Complexity:** O(k * (V + E)) with V = number of vertices, E = number of edges, k = size of the source set
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#topologicalSchedule(PgxGraph graph, VertexSet<ID> source)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#topologicalSchedule-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-)
  - [Analyst#topologicalSchedule(PgxGraph graph, VertexSet<ID> source, VertexProperty<ID,java.lang.Integer> topoSched)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#topologicalSchedule-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-oracle.pgx.api.VertexProperty-)

Topological schedule sets an order over the vertices in a graph based on the proximity these have to the vertices from the given source. The algorithm does a BFS traversal for each vertex from the source set in order to assign the correct scheduling order to all the reachable, even if the graph is undirected or has cycles. The vertices that are not reachable will be assigned a value of -1.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `source` | nodeSet | set of vertices to be used as the starting points for the scheduling order. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `top_sched` | vertexProp<int> | vertex property holding the scheduled order of each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;

@GraphAlgorithm
public class TopologicalSchedule {
  public void topologicalSchedule(PgxGraph g, VertexSet source, @Out VertexProperty<Integer> schedule) {
    schedule.setAll(-1);

    source.forEach(n -> inBFS(g, n).forward(v -> {
      if (schedule.get(v) > currentLevel() || schedule.get(v) == -1) {
        schedule.set(v, currentLevel());
      }
    }));
  }
}
```
