# Fast Path Finding

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_o8_limited_path_finding_undirected
- **Time Complexity:** O(E) with E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#limitedShortestPathHopDist(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, int maxHops, PgxMap<Integer, PgxVertex<ID>> highDegreeVertexMapping, VertexSet<ID> highDegreeVertices, VertexProperty<ID, PgxVect<Integer>> index)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#limitedShortestPathHopDist-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.PgxVertex-int-oracle.pgx.api.PgxMap-oracle.pgx.api.VertexSet-oracle.pgx.api.VertexProperty)
  - [Analyst#limitedShortestPathHopDist(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, int maxHops, PgxMap<Integer, PgxVertex<ID>> highDegreeVertexMapping, VertexSet<ID> highDegreeVertices, VertexProperty<ID, PgxVect<Integer>> index, VertexSequence<ID> pathVertices, EdgeSequence pathEdges)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#limitedShortestPathHopDist-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.PgxVertex-int-oracle.pgx.api.PgxMap-oracle.pgx.api.VertexSet-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexSequence-oracle.pgx.api.EdgeSequence)

Computes the shortest path between the source and destination vertex. The algorithm only considers paths up to a length of k.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `src` | node | the source vertex. |
| `dst` | node | the destination vertex. |
| `k` | int | the dimension of the distances property; i.e. number of high-degree vertices. |
| `maxHops` | int | the maximum number of edges to follow when trying to find a path. |
| `superNodes` | nodeSet | the high-degree vertices. |
| `superNodeMapping` | map<int, node> | the high-degree vertices. |
| `distances` | vertexProp<vect<int>[k]> | index containing distances to high-degree vertices. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `path` | nodeSeq | will contain the vertices on the found path or will be empty if there is none. |
| `pathEdges` | edgeSeq | will contain the vertices on the found path or will be empty if there is none. |

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
import oracle.pgx.algorithm.PgxVect;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSequence;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Length;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.ControlFlow.exit;
import static oracle.pgx.algorithm.PgxVertex.NONE;

@GraphAlgorithm
public class HopDistPathFindingUndirected {
  public int fastPathFinding(PgxGraph g, PgxVertex src, PgxVertex dst, int k, int maxHopsArg, VertexSet superNodes,
      PgxMap<Integer, PgxVertex> superNodeMapping, VertexProperty<@Length("k") PgxVect<Integer>> distances,
      @Out VertexSequence path) {
    int maxHops = maxHopsArg;

    if (src == dst) {
      path.push(src);
      return 0;
    } else if (maxHops == 0) {
      return -1;
    }
    if (src.hasEdgeTo(dst) || dst.hasEdgeTo(src)) {
      path.push(src);
      path.push(dst);
      return 1;
    } else if (maxHops == 1) {
      return -1;
    }

    // 2. check whether src and dst are connected via super-nodes
    int superNodeIndex = -1;
    {
      @Length("k")
      PgxVect<Integer> srcVec = distances.get(src);
      @Length("k")
      PgxVect<Integer> dstVec = distances.get(src);
      int current = 0;
      int minHops = maxHops + 1;

      while (current < k) {
        int hops = srcVec.get(current) + dstVec.get(current);
        if (hops < minHops) {
          minHops = hops;
          superNodeIndex = current;
        }
        current++;
      }

      if (superNodeIndex != -1) {
        maxHops = minHops; // can reduce the maxHops since no shortest path can be longer than this one
      }
    }

    // 3. find path from src to dst ignoring super-nodes
    int hops = bidirectionalHopDist(g, src, dst, maxHops, superNodes, path);
    if (hops != -1) {
      // found a path without super-nodes
      return hops;
    }

    if (superNodeIndex == -1) {
      // no path with and without super-nodes; i.e. there is no path at all
      return -1;
    }

    // 4. compute path using superNode computed in 1.
    return computePathIncludingSuperNode(g, src, dst, superNodeIndex, k, superNodeMapping, distances, path);
  }

  int computePathIncludingSuperNode(PgxGraph g, PgxVertex src, PgxVertex dst, int superNodeIndex, int k,
      PgxMap<Integer, PgxVertex> superNodeMapping, VertexProperty<@Length("k") PgxVect<Integer>> distances,
      @Out VertexSequence path) {
    PgxVertex superNode = superNodeMapping.get(superNodeIndex);
    int forwardHops = getDistanceToSuperNode(g, src, superNodeIndex, k, distances);
    int reverseHops = getDistanceToSuperNode(g, dst, superNodeIndex, k, distances);

    VertexSequence forwardPath = VertexSequence.create();
    hopDist(g, src, superNode, forwardHops, forwardPath);
    hopDist(g, dst, superNode, reverseHops, path);

    if (path.size() == 0 && forwardPath.size() == 0) {
      return -1;
    }

    path.pushFront(superNode);
    forwardPath.forSequential(path::pushFront);
    return path.size() - 1;
  }

  int getDistanceToSuperNode(PgxGraph g, PgxVertex n, int superNodeIndex, int k,
      VertexProperty<@Length("k") PgxVect<Integer>> distances) {
    @Length("k")
    PgxVect<Integer> entry = distances.get(n);
    int result = entry.get(superNodeIndex);

    return result;
  }

  int buildPathBidirectional(PgxGraph g, PgxVertex src, PgxVertex dst, PgxVertex middle,
      PgxMap<PgxVertex, PgxVertex> parentForward, PgxMap<PgxVertex, PgxVertex> parentReverse,
      @Out VertexSequence path) {
    VertexSequence forwardPath = VertexSequence.create();
    buildPath(g, middle, src, parentForward, forwardPath);
    buildPath(g, middle, dst, parentReverse, path);

    path.pushFront(middle);
    forwardPath.forSequential(path::pushFront);
    return path.size() - 1;
  }

  int buildPath(PgxGraph g, PgxVertex start, PgxVertex end, PgxMap<PgxVertex, PgxVertex> parents,
      @Out VertexSequence path) {
    PgxVertex current = start;
    while (current != end) {
      current = parents.get(current);
      path.pushBack(current);
    }
    return path.size() - 1;
  }

  int bidirectionalHopDist(PgxGraph g, PgxVertex src, PgxVertex dst, int maxHops, VertexSet superNodes,
      @Out VertexSequence path) {
    VertexSequence frontierForward = VertexSequence.create();
    VertexSequence frontierReverse = VertexSequence.create();
    frontierForward.pushBack(src);
    frontierReverse.pushBack(dst);

    PgxMap<PgxVertex, PgxVertex> parentForward = PgxMap.create();
    PgxMap<PgxVertex, PgxVertex> parentReverse = PgxMap.create();
    parentForward.set(src, src);
    parentReverse.set(dst, dst);

    int current = 0;

    while (current < maxHops && frontierForward.size() > 0 && frontierReverse.size() > 0) {
      int c = frontierForward.size();
      while (c > 0) {
        c--;

        PgxVertex n = frontierForward.popFront();

        n.getNeighbors().filter(v -> parentForward.get(v) == NONE && !superNodes.contains(v)).forSequential(v -> {
          parentForward.set(v, n);
          if (parentReverse.get(v) != NONE) {
            exit(buildPathBidirectional(g, src, dst, v, parentForward, parentReverse, path));
          } else {
            frontierForward.pushBack(v);
          }
        });
      }

      current++;
      if (current == maxHops) {
        return -1;
      }
      c = frontierReverse.size();
      while (c > 0) {
        c--;

        PgxVertex n = frontierReverse.popFront();

        n.getNeighbors().filter(v -> parentReverse.get(v) == NONE && !superNodes.contains(v)).forSequential(v -> {
          parentReverse.set(v, n);
          if (parentForward.get(v) != NONE) {
            exit(buildPathBidirectional(g, src, dst, v, parentForward, parentReverse, path));
          } else {
            frontierReverse.pushBack(v);
          }
        });
      }
      current++;
    }
    return -1;
  }

  int hopDist(PgxGraph g, PgxVertex src, PgxVertex dst, int maxHops, @Out VertexSequence path) {
    VertexSequence frontierForward = VertexSequence.create();
    frontierForward.pushBack(src);

    PgxMap<PgxVertex, PgxVertex> parentForward = PgxMap.create();
    parentForward.set(src, src);

    int current = 0;

    while (current < maxHops && frontierForward.size() > 0) {
      current++;

      int c = frontierForward.size();
      while (c > 0) {
        c--;

        PgxVertex n = frontierForward.popFront();

        n.getNeighbors().filter(v -> parentForward.get(v) == NONE).forSequential(v -> {
          parentForward.set(v, n);
          if (v == dst) {
            exit(buildPath(g, v, src, parentForward, path));
          } else {
            frontierForward.pushBack(v);
          }
        });
      }
    }
    return -1;
  }
}

/*
// use graph instead of dGraph until GM-16698 is fixed
proc fastPathFinding(graph g, node src, node dst, int k, int maxHopsArg, nodeSet superNodes, map<int, node>
superNodeMapping, nodeProp<vect<int>[k]> distances;
    nodeSeq path) : int {

  int maxHops = maxHopsArg;

  // 1. trivial cases
  if (src == dst) {
    path.push(src);
    return 0;
  } else if (maxHops == 0) {
    return -1;
  }
  if (src.hasEdgeTo(dst) || dst.hasEdgeTo(src)) {
    path.push(src);
    path.push(dst);
    return 1;
  } else if (maxHops == 1) {
    return -1;
  }

  // 2. check whether src and dst are connected via super-nodes
  int superNodeIndex = -1;
  {
    iVect[k] srcVec = src.distances;
    iVect[k] dstVec = dst.distances;
    int current = 0;
    int minHops = maxHops + 1;

    while (current < k) {
      int hops = srcVec[current] + dstVec[current];
      if (hops < minHops) {
        minHops = hops;
        superNodeIndex = current;
      }
      current++;
    }

    if (superNodeIndex != -1) {
      maxHops = minHops; // can reduce the maxHops since no shortest path can be longer than this one
    }
  }

  // 3. find path from src to dst ignoring super-nodes
  int hops = bidirectionalHopDist(g, src, dst, maxHops, superNodes, path);
  if (hops != -1) {
    // found a path without super-nodes
    return hops;
  }

  if (superNodeIndex == -1) {
    // no path with and without super-nodes; i.e. there is no path at all
    return -1;
  }

  // 4. compute path using superNode computed in 1.
  return computePathIncludingSuperNode(g, src, dst, superNodeIndex, k, superNodeMapping, distances, path);
}

local computePathIncludingSuperNode(graph g, node src, node dst, int superNodeIndex, int k, map<int, node>
superNodeMapping, nodeProp<vect<int>[k]> distances; nodeSeq path) : int {
  node superNode = superNodeMapping[superNodeIndex];
  int forwardHops = getDistanceToSuperNode(g, src, superNodeIndex, k, distances);
  int reverseHops = getDistanceToSuperNode(g, dst, superNodeIndex, k, distances);

  nodeSeq forwardPath;
  hopDist(g, src, superNode, forwardHops, forwardPath);
  hopDist(g, dst, superNode, reverseHops, path);

  if (path.size() == 0 && forwardPath.size() == 0) {
    return -1;
  }

  path.pushFront(superNode);
  for (n: forwardPath.items) {
    path.pushFront(n);
  }
  return path.size() - 1;
}

local getDistanceToSuperNode(graph g, node n, int superNodeIndex, int k, nodeProp<vect<int>[k]> distances) : int {
  iVect[k] entry = n.distances;
  int result = entry[superNodeIndex];
  return result;
}

local buildPathBidirectional(graph g, node src, node dst, node middle, map<node, node> parentForward, map<node, node>
 parentReverse; nodeSeq path) : int {
  nodeSeq forwardPath;
  buildPath(g, middle, src, parentForward, forwardPath);
  buildPath(g, middle, dst, parentReverse, path);

  path.pushFront(middle);
  for (n: forwardPath.items) {
    path.pushFront(n);
  }
  return path.size() - 1;
}

local buildPath(graph g, node start, node end, map<node, node> parents; nodeSeq path) : int {
  node current = start;
  while (current != end) {
    current = parents[current];
    path.pushBack(current);
  }
  return path.size() - 1;
}

local bidirectionalHopDist(graph g, node src, node dst, int maxHops, nodeSet superNodes; nodeSeq path) : int {
  nodeSeq frontier_forward;
  nodeSeq frontier_reverse;
  frontier_forward.pushBack(src);
  frontier_reverse.pushBack(dst);

  map<node, node> parentForward;
  map<node, node> parentReverse;
  parentForward[src] = src;
  parentReverse[dst] = dst;

  int current = 0;

  while (current < maxHops && frontier_forward.size() > 0 && frontier_reverse.size() > 0) {
    int c = frontier_forward.size();
    while (c > 0) {
      c--;

      node n = frontier_forward.popFront();

      for(v: n.inOutNbrs) (parentForward[v] == NIL && superNodes.has(v) == false) {
        parentForward[v] = n;
        if (parentReverse[v] != NIL) {
          return buildPathBidirectional(g, src, dst, v, parentForward, parentReverse, path);
        } else {
          frontier_forward.pushBack(v);
        }
      }
    }

    current++;
    if (current == maxHops) {
      return -1;
    }
    c = frontier_reverse.size();
    while (c > 0) {
      c--;

      node n = frontier_reverse.popFront();

      for(v: n.inOutNbrs) (parentReverse[v] == NIL && superNodes.has(v) == false) {
        parentReverse[v] = n;
        if (parentForward[v] != NIL) {
          return buildPathBidirectional(g, src, dst, v, parentForward, parentReverse, path);
        } else {
          frontier_reverse.pushBack(v);
        }
      }
    }
    current++;
  }
  return -1;
}

local hopDist(graph g, node src, node dst, int maxHops; nodeSeq path) : int {

  nodeSeq frontier_forward;
  frontier_forward.pushBack(src);

  map<node, node> parentForward;
  parentForward[src] = src;

  int current = 0;

  while (current < maxHops && frontier_forward.size() > 0) {
    current++;

    int c = frontier_forward.size();
    while (c > 0) {
      c--;

      node n = frontier_forward.popFront();

      for(v: n.inOutNbrs) (parentForward[v] == NIL) {
        parentForward[v] = n;
        if (v == dst) {
          return buildPath(g, v, src, parentForward, path);
        } else {
          frontier_forward.pushBack(v);
        }
      }
    }
  }
  return -1;
}
*/
```
