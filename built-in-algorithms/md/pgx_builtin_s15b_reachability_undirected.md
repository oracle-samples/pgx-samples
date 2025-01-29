# Reachability (undirected)

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s15b_reachability_undirected
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(1)
- **Javadoc:**
  - [Analyst#reachabilityAsync(PgxGraph graph, PgxVertex<ID> source, PgxVertex<ID> dest, int maxHops, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#reachabilityAsync_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_PgxVertex_int_boolean_)

This algorithm tries to find if the destination vertex is reachable given the source vertex and the maximum hop distance set by the user. The search can be performed in a directed or undirected way. These options may lead to different hop distances, since an undirected search has less restrictions on the possible paths connecting vertices than the directed option. Hence hop distances from an undirected search can be smaller than the ones from the directed cases.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `source` | node | source vertex for the search. |
| `dest` | node | destination vertex for the search. |
| `maxHops` | int | maximum hop distance between the source and destination vertices. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | int | the number of hops between the vertices. It will return -1 if the vertices are not connected or are not reachable given the condition of the maximum hop distance allowed. |

## Code

```java
/*
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.ControlFlow;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;

import static oracle.pgx.algorithm.Traversal.Direction.IN_OUT_EDGES;
import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;
import static oracle.pgx.algorithm.Traversal.stopTraversal;

@GraphAlgorithm
public class ReachabilityUndirected {
  public int reachabilityUndirected(PgxGraph g, PgxVertex source, PgxVertex dest, int maxHops) {
    int threshold = 10000000;
    Scalar<PgxVertex> src = Scalar.create(source);
    Scalar<PgxVertex> dst = Scalar.create(dest);

    if (src.get().getInDegree() + src.get().getOutDegree() > dst.get().getInDegree() + dst.get().getOutDegree()) {
      PgxVertex temp = src.get();
      src.set(dst.get());
      dst.set(temp);
    }

    // Specialized for fast up-to-3-hop finding
    // Not for general cases

    // 0 hop
    if (src == dst) {
      return 0;
    } else if (maxHops == 0) {
      return -1;
    }

    // 1 hop
    if (src.get().hasEdgeTo(dst.get()) || src.get().hasEdgeFrom(dst.get())) {
      return 1;
    } else if (maxHops == 1) {
      return -1;
    }

    Scalar<Integer> s = Scalar.create(0);

    // 2 hop
    {
      Scalar<PgxVertex> last1 = Scalar.create(src.get());
      src.get().getOutNeighbors().filter(l1 -> (l1 != last1.get() && l1 != src.get())).forSequential(l1 -> {
        last1.set(l1);
        s.increment();
        if (l1.hasEdgeTo(dst.get()) || l1.hasEdgeFrom(dst.get())) {
          ControlFlow.exit(2);
        }
      });

      last1.set(src.get());
      src.get().getInNeighbors().filter(l1 -> l1 != last1.get() && l1 != src.get()).forSequential(l1 -> {
        last1.set(l1);
        s.increment();
        if (l1.hasEdgeTo(dst.get()) || l1.hasEdgeFrom(dst.get())) {
          ControlFlow.exit(2);
        }
      });
    }

    if (maxHops == 2) {
      return -1;
    }

    // 3 hop (L1-OUT)
    if (s.get() < threshold) {
      Scalar<PgxVertex> last1 = Scalar.create(src.get());
      src.get().getOutNeighbors().filter(l1 -> s.get() < threshold && l1 != last1.get() && l1 != src.get())
          .forSequential(l1 -> {
            last1.set(l1);
            s.increment();
            if (s.get() < threshold) {
              Scalar<PgxVertex> last2 = Scalar.create(l1);
              l1.getOutNeighbors().filter(l2 -> s.get() < threshold && l2 != last2.get() && l2 != l1 && l2 != src.get())
                  .forSequential(l2 -> {
                    last2.set(l2);
                    s.increment();
                    if (l2.hasEdgeTo(dst.get()) || l2.hasEdgeFrom(dst.get())) {
                      ControlFlow.exit(3);
                    }
                  });
            }

            if (s.get() < threshold) {
              Scalar<PgxVertex> last2 = Scalar.create(l1);
              l1.getInNeighbors().filter(l2 -> s.get() < threshold && l2 != last2.get() && l2 != l1 && l2 != src.get())
                  .forSequential(l2 -> {
                    last2.set(l2);
                    s.increment();
                    if (l2.hasEdgeTo(dst.get()) || l2.hasEdgeFrom(dst.get())) {
                      ControlFlow.exit(3);
                    }
                  });
            }
          });
    }

    // 3 hop (L1-IN)
    if (s.get() < threshold) {
      Scalar<PgxVertex> last1 = Scalar.create(src.get());
      src.get().getInNeighbors().filter(l1 -> s.get() < threshold && l1 != last1.get() && l1 != src.get())
          .forSequential(l1 -> {
            last1.set(l1);
            s.increment();

            if (s.get() < threshold) {
              Scalar<PgxVertex> last2 = Scalar.create(l1);
              l1.getOutNeighbors().filter(l2 -> s.get() < threshold && l2 != last2.get() && l2 != l1 && l2 != src.get())
                  .forSequential(l2 -> {
                    last2.set(l2);
                    s.increment();
                    if (l2.hasEdgeTo(dst.get()) || l2.hasEdgeFrom(dst.get())) {
                      ControlFlow.exit(3);
                    }
                  });
            }

            if (s.get() < threshold) {
              Scalar<PgxVertex> last2 = Scalar.create(l1);
              l1.getInNeighbors().filter(l2 -> s.get() < threshold && l2 != last2.get() && l2 != l1 && l2 != src.get())
                  .forSequential(l2 -> {
                    last2.set(l2);
                    s.increment();
                    if (l2.hasEdgeTo(dst.get()) || l2.hasEdgeFrom(dst.get())) {
                      ControlFlow.exit(3);
                    }
                  });
            }
          });
    }

    Scalar<Integer> found = Scalar.create(-1);
    if (!(s.get() < threshold && maxHops == 3)) {
      Scalar<PgxVertex> dst2 = Scalar.create(dst.get());  // a hack to get away with creating accessor in above code
      // failed fast path
      // return correct answer with Big-BFS
      inBFS(g, src.get()).direction(IN_OUT_EDGES).filter(n -> found.get() == -1)
          .navigator(n -> found.get() == -1 && currentLevel() < maxHops).forward(n -> {
            if (n == dst2.get()) {
              found.set(currentLevel());
              stopTraversal();
            }
          });
    }

    return found.get();
  }
}
```
