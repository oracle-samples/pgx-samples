# K-Core

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s9_kcore
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#kcore(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#kcore-oracle.pgx.api.PgxGraph-)
  - [Analyst#kcore(PgxGraph graph, int minCore, int maxCore)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#kcore-oracle.pgx.api.PgxGraph-int-int-)
  - [Analyst#kcore(PgxGraph graph, int minCore, int maxCore, Scalar<java.lang.Long> maxKCore, VertexProperty<ID,java.lang.Long> kcore)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#kcore-oracle.pgx.api.PgxGraph-int-int-oracle.pgx.api.Scalar-oracle.pgx.api.VertexProperty-)
  - [Analyst#kcore(PgxGraph graph, Scalar<java.lang.Long> maxKCore, VertexProperty<ID,java.lang.Long> kcore)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#kcore-oracle.pgx.api.PgxGraph-oracle.pgx.api.Scalar-oracle.pgx.api.VertexProperty-)

A k-core is a maximal subgraph in which all of its vertices are connected and have the property that all of them have a degree of at least k. The k-cores can be regarded as layers in a graph, since a (k+1)-core will always be a subgraph of a k-core. This means that the larger k becomes, the smaller its k-core (i.e. its corresponding subgraph) will be. The k-core value (or coreness) assigned to a vertex will correspond to the core with the greatest degree from all the cores where it belongs. This implementation of k-core will look for cores lying within the interval set by the min_core and max_core input variables.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `min_core` | long | minimum k-core value. |
| `max_core` | long | maximum k-core value. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `k_core` | vertexProp<long> | vertex property with the largest k-core value for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | long | the largest k-core value found. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class Kcore {
  public long kcore(PgxGraph g, long minCore, long maxCore, @Out VertexProperty<Long> kcore) {
    VertexProperty<Long> activeNbrs = VertexProperty.create();
    VertexProperty<Boolean> active = VertexProperty.create();
    VertexProperty<Boolean> justDeleted = VertexProperty.create();

    Scalar<Long> currentKcore = Scalar.create();
    currentKcore.set(minCore);
    Scalar<Boolean> nodesJustDeleted = Scalar.create();
    nodesJustDeleted.set(false);
    Scalar<Boolean> activeNodesLeft = Scalar.create();
    activeNodesLeft.set(true);

    kcore.setAll(0L);
    active.setAll(true);
    justDeleted.setAll(false);
    activeNbrs.setAll(v -> v.getOutDegree() + v.getInDegree());

    while (currentKcore.get() <= maxCore && activeNodesLeft.get()) {
      do {
        activeNodesLeft.set(false);
        nodesJustDeleted.set(false);

        // Notify neighbors that vertex is deleted
        g.getVertices().filter(justDeleted).forEach(n -> {
          //n.getOutNeighbors().forEach(v -> activeNbrs.decrement(v));
          n.getOutNeighbors().forEach(activeNbrs::decrement);
          n.getInNeighbors().forEach(activeNbrs::decrement);
        });

        // Consolidate
        g.getVertices().filter(active).forEach(n -> {
          if (justDeleted.get(n)) {
            justDeleted.set(n, false);
            active.set(n, false);
          } else if (activeNbrs.get(n) < currentKcore.get()) {
            justDeleted.set(n, true);
            kcore.set(n, currentKcore.get() - 1);
            activeNodesLeft.reduceOr(true);
            nodesJustDeleted.reduceOr(true);
          } else {
            activeNodesLeft.reduceOr(true);
          }
        });
      } while (nodesJustDeleted.get());
      currentKcore.increment();
    }

    return currentKcore.get() - 2;
  }
}
```
