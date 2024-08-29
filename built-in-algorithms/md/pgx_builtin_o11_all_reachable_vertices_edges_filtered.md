# All Vertices and Edges on Filtered Path

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_o11_all_reachable_vertices_edges_filtered
- **Time Complexity:** O(E) with E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#allReachableVerticesEdges(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, int k, EdgeFilter filter)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#allReachableVerticesEdgesFiltered_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_PgxVertex_int_oracle_pgx_api_filter_EdgeFilter_)

Finds all the vertices and edges on a path between the src and target of length smaller or equal to k.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `src` | node | the source vertex. |
| `dst` | node | the destination vertex. |
| `k` | int | the dimension of the distances property; i.e. number of high-degree vertices. |
| `filter` | edgeFilter | the filter to be used on edges when searching for a path. |

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

  public void undirectedAllOnPath(PgxGraph g, PgxVertex src, PgxVertex dst, int k, EdgeFilter filter,
      @Out VertexSet verticesOnPath, @Out EdgeSet edgesOnPath, @Out PgxMap<PgxVertex, Integer> fDist) {

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
            PgxEdge e = m.edge();
            if (filter.evaluate(e)) {
              boolean rReachable = rReachables.contains(m);
              if (rReachable) {
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
            PgxEdge e = m.edge();
            if (filter.evaluate(e)) {
              boolean fReachable = fReachables.contains(m);
              if (fReachable) {
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
          PgxEdge e = m.edge();
          if (filter.evaluate(e)) {
            boolean rReachable = rReachables.contains(m);
            if (rReachable && rDist.get(m) <= (k - leftHop)) {
              edgesOnPath.add(e);
              verticesOnPath.add(m);

              boolean fReachable = fReachables.contains(m);
              if (fReachable == false) {
                nextFFrontier.add(m);
                fReachables.add(m);
                fDist.put(m, leftHop);
              }
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
          PgxEdge e = m.edge();
          if (filter.evaluate(e)) {
            boolean fReachable = fReachables.contains(m);
            if (fReachable && fDist.get(m) <= (k - rightHop)) {
              edgesOnPath.add(e);
              verticesOnPath.add(m);

              boolean rReachable = rReachables.contains(m);
              if (rReachable == false) {
                nextRFrontier.add(m);
                rReachables.add(m);
                rDist.put(m, rightHop);
              }
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
procedure undirected_all_on_path_filtered(graph G, node src, node dst, int k, edgeFilter filter;
    nodeSet verticesOnPath, edgeSet edgesOnPath, map<node, int> f_dist) : bool {

  nodeSet f_frontier; // at any given iteration, the set of left nodes
  f_frontier.add(src);
  nodeSet f_reachables = f_frontier; // the vertices that have been reached from the left

  nodeSet r_frontier; // at any given iteration, the set of right nodes
  r_frontier.add(dst);
  nodeSet r_reachables = r_frontier; // the vertices that have been reached from the right

  // f_dist stores the distance of the vertices reached from the left to src
  f_dist[src] = 0;
  map<node, int> r_dist; // r_dist stores the distance of the vertices reached from the right to dst
  r_dist[dst] = 0;

  // we start by doing k hops in a bidirectional manner so that we can visit all the vertices and edges
  // that are potentially on a path. Thanks to this marking we hopefully can avoid to follow unecessary edges
  // in the loops after that complete the full k-hops from both sides
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

      foreach (n : f_frontier.items) {
        for (m : n.inOutNbrs) {
          edge e = m.edge();
          if (filter.evaluate(e)) {
            // if m is in the right set, we have a path, we can record it for later
            bool r_reachable = r_reachables.has(m);
            if (r_reachable) {
              // the edge is on a contributing path
              edgesOnPath.add(e);

              // the vertex is on a contributing path
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
      }

      // swap frontiers
      f_frontier = next_f_frontier;
    } else {
      right_hop++;

      // scanning neighbors of right frontier is cheaper
      next_r_reachable_size = 0;
      nodeSet next_r_frontier;

      foreach (n : r_frontier.items) {
        for (m : n.inOutNbrs) {
          edge e = m.edge();
          if (filter.evaluate(e)) {
            // if m is in the left set, we have a path, we can record it for later
            bool f_reachable = f_reachables.has(m);
            if (f_reachable) {
              // the edge is on a contributing path
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
      }

      // swap frontiers
      r_frontier = next_r_frontier;
    }

    hop++;
  }

  // from that point on , we are extending from the left and the rights up to k hops
  // as we have already done k hops total from left and right, any vertex that contributes
  // to a path of length <= k should already be in the set of vertices discovered in the other direction
  // therefore we can include in the frontiers only the vertices in the other set of reached vertices

  // finish the left hops until we have reached k of them
  while ((left_hop < k) && (f_frontier.size() != 0)) {
    left_hop++;

    nodeSet next_f_frontier;

     foreach (n : f_frontier.items) {
      for (m : n.inOutNbrs) {
        edge e = m.edge();
        if (filter.evaluate(e)) {
          // if m is in the right set, at a distance less than k - left_hops, we have a path, we can record it for later
          bool r_reachable = r_reachables.has(m);
          if (r_reachable && r_dist[m] <= (k - left_hop)) {
            // the edge is on a contributing path
            edgesOnPath.add(e);

            // the vertex is on a contributing path
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
    }

    // swap frontiers
    f_frontier = next_f_frontier;

    hop++;
  }

  // finish the right hops until we have reached k of them
  while ((right_hop < k) && (r_frontier.size() != 0) ) {
    right_hop++;

    nodeSet next_r_frontier;

    foreach (n : r_frontier.items) {
      for (m : n.inOutNbrs) {
        edge e = m.edge();
        if (filter.evaluate(e)) {
          // if m is in the left set, at a distance less than k - left_hops, we have a path, we can record it for later
          bool f_reachable = f_reachables.has(m);
          if (f_reachable && f_dist[m] <= (k - right_hop)) {
            // the edge is on a contributing path
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
    }

    // swap frontiers
    r_frontier = next_r_frontier;

    hop++;
  }

  // we have discovered all the vertices on a contributing path
  return verticesOnPath.size() > 0;
}
*/
```
