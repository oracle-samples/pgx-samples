# All Vertices and Edges on Filtered Path

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_o10_all_reachable_vertices_edges
- **Time Complexity:** O(E) with E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#allReachableVerticesEdges(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, int k)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#allReachableVerticesEdges-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.PgxVertex-int)

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
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class AllReachableVerticesEdges {

  public void undirectedAllOnPath(PgxGraph g, PgxVertex src, PgxVertex dst, int k, @Out VertexSet verticesOnPath,
      @Out EdgeSet edgesOnPath, @Out PgxMap<PgxVertex, Integer> fDist) {

    int parallelThreshold = 1000;

    VertexSet fFrontier = VertexSet.create();
    fFrontier.add(src);
    VertexSet fReachables = fFrontier;

    VertexSet rFrontier = VertexSet.create();
    rFrontier.add(dst);
    VertexSet rReachables = rFrontier;

    dist.put(src, 0);
    PgxMap<PgxVertex, Integer> rDist = PgxMap.create();
    rDist.put(dst, 0);

    long nextFReachableSize = src.getOutDegree() + src.getInDegree();
    long nextRReachableSize = dst.getOutDegree() + dst.getInDegree();
    int hop = 0;
    int leftHop = 0;
    int rightHop = 0;
    while ((hop < k) && (fFrontier.size() != 0) && (rFrontier.size() != 0)) {
      if (nextFReachableSize <= nextRReachableSize) {
        leftHop++;

        nextFReachableSize = 0;
        VertexSet nextFFrontier = VertexSet.create();

        fFrontier.forSequential(n -> {
          n.getInOutNeighbors().forSequential(m -> {
            boolean rReachable = rReachables.contains(m);
            if (rReachable) {
              PgxEdge e = m.edge();
              edgesOnPath.add(e);
              verticesOnPath.add(m);
            }
            boolean fReachable = fReachables.contains(m);
            if (fReachable == false) {
              nextFFrontier.add(m);
              fReachables.add(m);
              fDist.put(m, leftHop);
              nextFReachableSize += m.getOutDegree() + m.getInDegree();
            }
          });
        });
        fFrontier = nextFFrontier;
      } else {
        rightHop++;

        nextRReachableSize = 0;
        VertexSet nextRFrontier = VertexSet.create();

        rFrontier.forSequential(n -> {
          n.getInOutNeighbors().forSequential(m -> {
            boolean fReachable = fReachables.contains(m);
            if (fReachable) {
              PgxEdge e = m.edge();
              edgesOnPath.add(e);

              verticesOnPath.add(m);
            }
            boolean rReachable = rReachables.contains(m);
            if (rReachable == false) {
              nextRFrontier.add(m);
              rReachables.add(m);
              rDist.put(m, rightHop);
              nextRReachableSize += m.getOutDegree() + m.getInDegree();
            }
          });
        });
        rFrontier = nextRFrontier;
      }
      hop++;
    }

    while ((leftHop < k) && (fFrontier.size() != 0)) {
      leftHop++;

      VertexSet nextFFrontier = VertexSet.create();

      fFrontier.forSequential(n -> {
        n.inOutNeighbors().forSequential(m -> {
          boolean rReachable = rReachables.contains(m);
          if (rReachable && rDist.get(m) <= (k - leftHop)) {
            PgxEdge e = m.edge();
            edgesOnPath.add(e);

            verticesOnPath.add(m);

            boolean fReachable = fReachables.contains(m);
            if (fReachable == false) {
              nextFFrontier.add(m);
              fReachables.add(m);
              fDist.put(m, leftHop);
            }
          }
        });
      });
      fFrontier = nextFFrontier;
      hop++;
    }

    while ((rightHop < k) && (rFrontier.size() != 0)) {
      rightHop++;

      VertexSet nextRFrontier = VertexSet.create();
      rFrontier.forSequential(n -> {
        n.inOutNeighbors().forSequential(m -> {
          boolean fReachable = fReachables.contains(m);
          if (fReachable && fDist.get(m) <= (k - rightHop)) {
            PgxEdge e = m.edge();
            edgesOnPath.add(e);

            verticesOnPath.add(m);

            boolean rReachable = rReachables.contains(m);
            if (rReachable == false) {
              nextRFrontier.add(m);
              rReachables.add(m);
              rDist.put(m, rightHop);
            }
          }
        });
      });
      rFrontier = nextRFrontier;
      hop++;
    }
    return verticesOnPath.size() > 0;
  }
}
/*
procedure undirected_all_on_path(graph G, node src, node dst, int k;
    nodeSet verticesOnPath, edgeSet edgesOnPath, map<node, int> f_dist) : bool {

  int parallel_threshold = 1000;

  nodeSet f_frontier; // at any given iteration, the set of left nodes
  f_frontier.add(src);
  nodeSet f_reachables = f_frontier; // the vertices that have been reached from the left

  nodeSet r_frontier; // at any given iteration, the set of right nodes
  r_frontier.add(dst);
  nodeSet r_reachables = r_frontier; // the vertices that have been reached from the right

  f_dist[src] = 0;
  map<node, int> r_dist; // r_dist stores the distance of the vertices reached from the right to dst
  r_dist[dst] = 0;

  long next_f_reachable_size = (src.outDegree() + src.inDegree());
  long next_r_reachable_size = (dst.outDegree() + dst.inDegree());
  int hop = 0;
  int left_hop = 0;
  int right_hop = 0;
  while ((hop < k) && (f_frontier.size() != 0) && (r_frontier.size() != 0) ) {

    if (next_f_reachable_size <= next_r_reachable_size) {
      left_hop++;

      // scanning neighbors of left frontier is cheaper
      next_f_reachable_size = 0;
      nodeSet next_f_frontier;

        for (n : f_frontier.items) {
          for (m : n.inOutNbrs) {
            // if m is in the right set, we have a path, we can record it for later
            bool r_reachable = r_reachables.has(m);
            if (r_reachable) {
              edge e = m.edge();
              edgesOnPath.add(e);

              verticesOnPath.add(m);
            }
            bool f_reachable = f_reachables.has(m);
            if (f_reachable == false) {
              next_f_frontier.add(m);
              f_reachables.add(m);
              f_dist[m] = left_hop;
              next_f_reachable_size += (m.outDegree() + m.inDegree());
            }
          }
        }
      f_frontier = next_f_frontier;
    } else {
      right_hop++;

      // scanning neighbors of right frontier is cheaper
      next_r_reachable_size = 0;
      nodeSet next_r_frontier;

        for (n : r_frontier.items) {
          for (m : n.inOutNbrs) {
            // if m is in the left set, we have a path, we can record it for later
            bool f_reachable = f_reachables.has(m);
            if (f_reachable) {
              // the edge is on a contributing path
              edge e = m.edge();
              edgesOnPath.add(e);

              // the vertex is on a contributing path
              verticesOnPath.add(m);
            }
            bool r_reachable = r_reachables.has(m);
            if (r_reachable == false) {
              next_r_frontier.add(m);
              r_reachables.add(m);
              r_dist[m] = right_hop;
              next_r_reachable_size += (m.outDegree() + m.inDegree());
            }
          }
        }

      r_frontier = next_r_frontier;
    }

    hop++;
  }

  while ((left_hop < k) && (f_frontier.size() != 0)) {
    left_hop++;

    nodeSet next_f_frontier;


      for (n : f_frontier.items) {
        for (m : n.inOutNbrs) {
          bool r_reachable = r_reachables.has(m);
          if (r_reachable && r_dist[m] <= (k - left_hop)) {
            edge e = m.edge();
            edgesOnPath.add(e);

            verticesOnPath.add(m);

            bool f_reachable = f_reachables.has(m);
            if (f_reachable == false) {
              next_f_frontier.add(m);
              f_reachables.add(m);
              f_dist[m] = left_hop;
            }
          }
        }
      }
    f_frontier = next_f_frontier;

    hop++;
  }

  while ((right_hop < k) && (r_frontier.size() != 0) ) {
    right_hop++;

    nodeSet next_r_frontier;
      for (n : r_frontier.items) {
        for (m : n.inOutNbrs) {
          // if m is in the left set, at a distance less than k - left_hops, we have a path, we can record it for later
          bool f_reachable = f_reachables.has(m);
          if (f_reachable && f_dist[m] <= (k - right_hop)) {
            // the edge is on a contributing path
            edge e = m.edge();
            edgesOnPath.add(e);

            // the vertex is on a contributing path
            verticesOnPath.add(m);

            bool r_reachable = r_reachables.has(m);
            if (r_reachable == false) {
              next_r_frontier.add(m);
              r_reachables.add(m);
              r_dist[m] = right_hop;
            }
          }
        }
      }
    r_frontier = next_r_frontier;

    hop++;
  }
  return verticesOnPath.size() > 0;
}

*/
```
