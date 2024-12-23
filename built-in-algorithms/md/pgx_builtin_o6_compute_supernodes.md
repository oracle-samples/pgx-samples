# Compute High-Degree Vertices

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_o6_compute_supernodes
- **Time Complexity:** O(N log N) with N = number of vertices
- **Space Requirement:** O(k) with V = number of vertices
- **Javadoc:**
  - [Analyst#computeHighDegreeVertices(PgxGraph graph, int k)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#computeHighDegreeVertices_oracle_pgx_api_PgxGraph_int_)
  - [Analyst#computeHighDegreeVertices(PgxGraph graph, int k, PgxMap<java.lang.Integer,​PgxVertex<ID>> highDegreeVertexMapping, VertexSet<ID> highDegreeVertices)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#computeHighDegreeVertices_oracle_pgx_api_PgxGraph_int_oracle_pgx_api_PgxMap_oracle_pgx_api_VertexSet_)

Computes the k vertices with the highest degrees in the graph. The resulting map will contain a mapping with the sorted index to the high-degree vertex with the index.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `k` | int | number of high-degree vertices to be computed. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `superNodes` | nodeSet | the high-degree vertices. |
| `superNodeMapping` | map<int, node> | the high-degree vertices. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class HopDistPathComputeSupernodes {
  public void computeSuperNodes(PgxGraph g, int k, @Out PgxMap<Integer, PgxVertex> superNodeMapping,
      VertexSet superNodes) {
    PgxMap<PgxVertex, Long> vertexDegrees = PgxMap.create();
    g.getVertices().forSequential(n -> {
      long deg = n.getInDegree() + n.getOutDegree();
      if (vertexDegrees.size() < k) {
        vertexDegrees.set(n, deg);
      } else {
        PgxVertex other = vertexDegrees.getKeyForMinValue();
        if (deg > vertexDegrees.get(other)) {
          vertexDegrees.remove(other);
          vertexDegrees.set(n, deg);
        }
      }
    });

    // assert vertexDegrees.size() <= k
    Scalar<Integer> counter = Scalar.create(0);
    vertexDegrees.keys().forSequential(superNode -> {
      long deg = vertexDegrees.get(superNode);
      superNodes.add(superNode);
      superNodeMapping.set(counter.get(), superNode);
      counter.increment();
    });
  }
}
```
