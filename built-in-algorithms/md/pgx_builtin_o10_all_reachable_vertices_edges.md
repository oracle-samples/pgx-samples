# All Vertices and Edges on Filtered Path

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_o10_all_reachable_vertices_edges
- **Time Complexity:** O(E) with E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#allReachableVerticesEdges(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, int k)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#allReachableVerticesEdges_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_PgxVertex_int_)

Finds all the vertices and edges on a path between the src and target of length smaller or equal to k.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `src` | node | the source vertex. |
| `dst` | node | the destination vertex. |
| `k` | int | the dimension of the distances property; i.e. number of high-degree vertices. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `verticesOnPath` | nodeSet | the vertices on the path. |
| `edgesOnPath` | edgeSet | the edges on the path. |
| `f_dist` | map<node, int> | map containing the distances from the source vertex for each vertex on the path. |

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
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.EdgeSet;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class AllReachableVerticesEdges {

  public boolean undirectedAllOnPath(PgxGraph g, PgxVertex src, PgxVertex dst, int k,
      @Out VertexSet verticesOnPath, @Out EdgeSet edgesOnPath, @Out PgxMap<PgxVertex, Integer> distances) {

    // frontiers
    VertexSet srcFrontier = VertexSet.create();
    VertexSet dstFrontier = VertexSet.create();

    srcFrontier.add(src);
    dstFrontier.add(dst);

    // distances
    PgxMap<PgxVertex, Integer> dstDistances = PgxMap.create();

    distances.clear();
    distances.set(src, 0);
    dstDistances.set(dst, 0);

    // hop counters
    int hop = 0;
    int srcHop = 0;
    int dstHop = 0;

    // frontier sizes for next round
    long nextSrcFrontierSize = src.getOutDegree() + src.getInDegree();
    long nextDstFrontierSize = dst.getOutDegree() + dst.getInDegree();

    while ((hop < k) && (srcFrontier.size() != 0) && (dstFrontier.size() != 0)) {
      // explore smaller frontier
      if (nextSrcFrontierSize <= nextDstFrontierSize) {
        // explore source frontier
        srcHop++;

        // next frontier
        VertexSet nextFrontier = VertexSet.create();
        nextSrcFrontierSize = 0;

        // explore frontier
        srcFrontier.forSequential(n -> {
          n.getNeighbors().forSequential(m -> {
            if (dstDistances.containsKey(m)) {
              // neighbor is reachable from dst
              PgxEdge e = m.edge();
              edgesOnPath.add(e);
              verticesOnPath.add(m);
            }
            if (!distances.containsKey(m)) {
              // neighbor was not reached yet
              distances.set(m, srcHop);
              nextFrontier.add(m);
              nextSrcFrontierSize += m.getOutDegree() + m.getInDegree();
            }
          });
        });

        // update source frontier
        srcFrontier = nextFrontier.clone();
      } else {
        // explore destination frontier
        dstHop++;

        // next frontier
        VertexSet nextFrontier = VertexSet.create();
        nextDstFrontierSize = 0;

        // explore frontier
        dstFrontier.forSequential(n -> {
          n.getNeighbors().forSequential(m -> {
            if (distances.containsKey(m)) {
              // neighbor is reachable from src
              PgxEdge e = m.edge();
              edgesOnPath.add(e);
              verticesOnPath.add(m);
            }
            if (!dstDistances.containsKey(m)) {
              // neighbor was not reached yet
              dstDistances.set(m, dstHop);
              nextFrontier.add(m);
              nextDstFrontierSize += m.getOutDegree() + m.getInDegree();
            }
          });
        });
        dstFrontier = nextFrontier.clone();
      }
      hop++;
    }

    // finish source hops
    while (srcHop < k && srcFrontier.size() != 0) {

      srcHop++;

      // next frontier
      VertexSet nextFrontier = VertexSet.create();

      // explore frontier
      srcFrontier.forSequential(n -> {
        n.getNeighbors().forSequential(m -> {
          if (dstDistances.containsKey(m) && dstDistances.get(m) <= (k - srcHop)) {
            // neighbor is reachable from dst so the overall distance is <= k
            PgxEdge e = m.edge();
            edgesOnPath.add(e);
            verticesOnPath.add(m);

            if (!distances.containsKey(m)) {
              // neighbor was not reached yet
              distances.set(m, srcHop);
              nextFrontier.add(m);
            }
          }
        });
      });
      srcFrontier = nextFrontier.clone();
      hop++;
    }

    // finish destination hops
    while ((dstHop < k) && (dstFrontier.size() != 0)) {

      dstHop++;

      // next frontier
      VertexSet nextFrontier = VertexSet.create();

      // explore frontier
      dstFrontier.forSequential(n -> {
        n.getNeighbors().forSequential(m -> {
          if (distances.containsKey(m) && distances.get(m) <= (k - dstHop)) {
            // neighbor is reachable from dst so the overall distance is <= k
            PgxEdge e = m.edge();
            edgesOnPath.add(e);
            verticesOnPath.add(m);

            if (!dstDistances.containsKey(m)) {
              // neighbor was not reached yet
              dstDistances.set(m, dstHop);
              nextFrontier.add(m);
            }
          }
        });
      });
      dstFrontier = nextFrontier.clone();
      hop++;
    }

    return verticesOnPath.size() > 0;
  }
}
```
