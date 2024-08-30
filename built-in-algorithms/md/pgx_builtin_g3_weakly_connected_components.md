# Weakly Connected Components (WCC)

- **Category:** connected components
- **Algorithm ID:** pgx_builtin_g3_weakly_connected_components
- **Time Complexity:** O(E * d) with d = diameter of the graph
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#wcc(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#wcc_oracle_pgx_api_PgxGraph_)
  - [Analyst#wcc(PgxGraph graph, VertexProperty<ID,java.lang.Long> partitionDistribution)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#wcc_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

This algorithm finds weakly connected components (WCC) in a directed graph. A WCC is a maximal subset of vertices of the graph with the particular characteristic that for every pair of vertices U and V in the WCC there must be a path connecting U to V, ignoring the direction of edges. It is a non-deterministic algorithm because of its parallelized implementation.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `comp_id` | vertexProp<long> | vertex property holding the label of the WCC assigned to each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | long | the total number of WCC found in the graph. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import java.util.function.Function;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class Wcc {
  public long wcc(PgxGraph g, @Out VertexProperty<Long> compId) {
    VertexProperty<PgxVertex> wcc = VertexProperty.create();

    // Initialize
    wcc.setAll(Function.identity());

    Scalar<Boolean> changed = Scalar.create();
    do {
      changed.set(false);

      // kernel 1 (get 'maximum' value among neighbors)
      g.getVertices().forEach(u -> {
        u.getInNeighbors().forSequential(v -> {
          if (wcc.get(u).lessThan(wcc.get(v))) {
            changed.reduceOr(true);
            wcc.set(u, wcc.get(v));
          }
        });

        u.getOutNeighbors().forSequential(v -> {
          if (wcc.get(u).lessThan(wcc.get(v))) {
            changed.reduceOr(true);
            wcc.set(u, wcc.get(v));
          }
        });
      });

      // kernel 2
      g.getVertices().forEach(u -> {
        if (wcc.get(u) != u) {
          PgxVertex v = wcc.get(u);
          if (wcc.get(v) != v) {
            wcc.set(u, wcc.get(v));
          }
        }
      });
    } while (changed.get());

    // Create output format
    Scalar<Long> numComp = Scalar.create(0L);
    g.getVertices().forSequential(n -> {
      if (wcc.get(n) == n) {
        compId.set(n, numComp.get());
        numComp.increment();
      }
    });

    g.getVertices().filter(n -> wcc.get(n) != n).forEach(n -> {
      PgxVertex v = wcc.get(n);
      compId.set(n, compId.get(v));
    });

    return numComp.get();
  }
}
```
