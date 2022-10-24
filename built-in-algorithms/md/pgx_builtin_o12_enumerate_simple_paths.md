# Enumerate Simple Paths

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_o12_enumerate_simple_paths
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#enumerateSimplePaths(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, int k, VertexSet<ID> verticesOnPath, EdgeSet edgesOnPath, PgxMap<PgxVertex<ID>, Integer> dist)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#enumerateSimplePaths-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.PgxVertex-int-oracle.pgx.api.VertexSet-oracle.pgx.api.EdgeSet-oracle.pgx.api.PgxMap)

Enumerate all simple paths between the source and destination vertex


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `src` | node | the source vertex. |
| `dst` | node | the destination vertex. |
| `k` | int | the dimension of the distances property; i.e. number of high-degree vertices. |
| `verticesOnPath` | nodeSet | the vertices on the path. |
| `edgesOnPath` | edgeSet | the edges on the path. |
| `f_dist` | map<node, int> | map containing the distance from the source vertex for each vertex on a path. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `pathLengths` | sequence<int> | sequence containing the path lengths. |
| `pathVertices` | nodeSeq | vertex-sequence containing the vertices on the paths. |
| `pathEdges` | edgeSeq | edge-sequence containing the edges on the paths. |

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
public class EnumerateSimplePaths {

  public void enumerateSimplePaths(PgxGraph g, PgxVertex src, PgxVertex dst, int k, VertexSet verticesOnPath,
      EdgeSet edgesOnPath, PgxMap<PgxVertex, Integer> dist, @Out ScalarSequence<Integer> pathLengths,
      @Out VertexSequence pathVertices, @Out EdgeSequence pathEdges) {

    VertexSequence stack = VertexSequence.create();
    ScalarSequence<Integer> hopCountStack = ScalarSequence.create();

    stack.add(src);
    hopCountStack.add(0);

    PgxMap<PgxVertex, PgxVertex> parentVertices = PgxMap.create();
    parentVertices.put(src, PgxVertex.NONE);
    PgxMap<PgxVertex, PgxEdge> parentEdges = PgxMap.create();
    parentEdges.put(src, PgxEdge.NONE);

    if (k > 0) {
      while (stack.size() > 0) {
        PgxVertex current = stack.pop();
        int hop = hopCountStack.pop();

        if (current == dst) {
          int length = 1;
          PgxVertex pathVertex = current;
          pathVertices.push(pathVertex);

          PgxEdge pathEdge = parentEdges.get(pathVertex);
          pathVertex = parentVertices.get(pathVertex);

          int l = hop;
          while (l > 0) {
            pathVertices.push(pathVertex);
            pathEdges.push(pathEdge);
            length++;

            pathEdge = parentEdges.get(pathVertex);
            pathVertex = pathVertices.get(pathVertex);

            l--;
          }
          pathLengths.push(length);
        }
        if (hop < k) {
          int newHop = hop + 1;
          current.getInOutNeighbors().forEach(m -> {
            PgxEdge e = m.edge();
            if (verticesOnPath.contains(m) && edgesOnPath.contains(e) && dist.get(m) <= k
                && dist.get(current) < dist.get(m)) {
              parentVertices.put(m, current);
              parentEdges.put(m, e);
              stack.push(m);
              hopCountStack.push(newHop);
            }
          });
        }
      }
    }
  }
}
/*
procedure enumerate_simple_paths(graph G, node src, node dst, int k,
    nodeSet verticesOnPath, edgeSet edgesOnPath, map<node, int> f_dist; sequence<int> pathLengths, nodeSeq pathVertices,
    edgeSeq pathEdges) {

  // we generate all the paths by doing a pseudo-DFS using a stack
  // (we don't include paths with loops)

  nodeSeq stack;
  sequence<int> hopCountStack;

  stack.push(src);
  hopCountStack.push(0);

  map<node, node> parentNodes;
  parentNodes[src] = NIL;
  map<node, edge> parentEdges;
  parentEdges[src] = NIL;

  if (k > 0) {
    while (stack.size() > 0) {
      node cur = stack.pop();
      int hop = hopCountStack.pop();

      if (cur == dst) {
        // write down the path (in reverse order)
        int length = 1;
        node pathVertex = cur;
        pathVertices.push(pathVertex);

        edge pathEdge = parentEdges[pathVertex];
        pathVertex = parentNodes[pathVertex];

        int l = hop;
        while (l > 0) {
          pathVertices.push(pathVertex);
          pathEdges.push(pathEdge);
          length++;

          // prepare next iteration
          pathEdge = parentEdges[pathVertex];
          pathVertex = parentNodes[pathVertex];

          l--;
        }

        pathLengths.push(length);
      }
      if (hop < k) {
        // we can do more hops
        int newHop = hop + 1;
        for (m : cur.inOutNbrs) {
          edge e = m.edge();
          // to avoid loops we force increasing distance from the source
          if (verticesOnPath.has(m) && edgesOnPath.has(e) && f_dist[m] <= k && f_dist[cur] < f_dist[m]) {
            parentNodes[m] = cur;
            parentEdges[m] = e;
            stack.push(m);
            hopCountStack.push(newHop);
          }
        }
      }
    }
  }
}
*/
```
