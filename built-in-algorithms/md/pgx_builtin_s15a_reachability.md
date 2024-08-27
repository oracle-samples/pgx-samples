# Reachability

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s15a_reachability
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(1)
- **Javadoc:** 
  - [Analyst#reachability(PgxGraph graph, PgxVertex<ID> source, PgxVertex<ID> dest, int maxHops, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#reachability-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.PgxVertex-int-boolean-)

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
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;

import static oracle.pgx.algorithm.ControlFlow.exit;
import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;
import static oracle.pgx.algorithm.Traversal.stopTraversal;

@GraphAlgorithm
public class Reachability {
  public int reachability(PgxGraph g, PgxVertex source, PgxVertex dest, int maxHops) {
    Scalar<Boolean> giveup = Scalar.create(false);
    int threshold = 10000000;

    // 0 hop
    if (source == dest) {
      return 0;
    } else if (maxHops == 0) {
      return -1;
    }

    // 1 hop
    if (source.hasEdgeTo(dest)) {
      return 1;
    } else if (maxHops == 1) {
      return -1;
    }

    Scalar<Integer> s = Scalar.create(0);
    // 2, 3, 4 hop
    source.getOutNeighbors().filter(l1 -> !giveup.get()).forSequential(l1 -> {
      s.increment();
      if (s.get() >= threshold) {
        giveup.set(true);
      }
      l1.getOutNeighbors().filter(l2 -> !giveup.get()).forSequential(l2 -> {
        if (l2 == dest) {
          exit(2);
        }
        s.increment();
        if (s.get() >= threshold) {
          giveup.set(true);
        }
        if (!giveup.get() && maxHops >= 3) {
          l2.getOutNeighbors().filter(l3 -> !giveup.get()).forSequential(l3 -> {
            if (l3 == dest) {
              exit(3);
            }
            s.increment();
            if (s.get() >= threshold) {
              giveup.set(true);
            }
            if (!giveup.get() && maxHops >= 4) {
              l3.getOutNeighbors().filter(l4 -> !giveup.get()).forSequential(l4 -> {
                if (l4 == dest) {
                  exit(4);
                }
                s.increment();
                if (s.get() >= threshold) {
                  giveup.set(true);
                }
              });
            }
          });
        }
      });
    });

    if (!giveup.get() && maxHops <= 4) {
      return -1;
    }

    Scalar<Integer> found = Scalar.create(-1);

    inBFS(g, source) //
        .filter(n -> found.get() == -1) //
        .navigator(n -> (found.get() == -1) && currentLevel() < maxHops) //
        .forward(n -> {
          if (n == dest) {
            found.set(currentLevel());
            stopTraversal();
          }
        });

    return found.get();
  }
}
```
