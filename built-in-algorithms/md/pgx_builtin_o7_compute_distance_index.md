# Compute Distance Index

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_o7_compute_distance_index
- **Time Complexity:** O(E * k) with E = number of edges, k <= number of high-degree vertices
- **Space Requirement:** O(V * k) with V = number of vertices
- **Javadoc:** 
  - [Analyst#createDistanceIndex(PgxGraph graph, PgxMap<java.lang.Integer,​PgxVertex<ID>> highDegreeVertexMapping, VertexSet<ID> highDegreeVertices)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#createDistanceIndex_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxMap_oracle_pgx_api_VertexSet_)
  - [Analyst#createDistanceIndex(PgxGraph graph, PgxMap<java.lang.Integer,​PgxVertex<ID>> highDegreeVertexMapping, VertexSet<ID> highDegreeVertices, VertexProperty<ID,​PgxVect<java.lang.Integer>> index)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#createDistanceIndex_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxMap_oracle_pgx_api_VertexSet_oracle_pgx_api_VertexProperty_)

Computes an index which contains the distance to the given high-degree vertices for every node in the graph.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `k` | int | the dimension of the distances property; i.e. number of high-degree vertices. |
| `superNodeMapping` | map<int, node> | map containing the high-degree vertices as values and indices from 0 to k as keys. |
| `superNodes` | nodeSet | a set containing the high-degree vertices. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `distances` | vertexProp<vect<int>[k]> | the index containing the distances to each high-degree vertex for all vertices. |

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
import oracle.pgx.algorithm.PgxVect;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.Traversal;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Length;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;

@GraphAlgorithm
public class HopDistPathIndexCreation {

  public void createDistanceIndex(PgxGraph g, int k, PgxMap<Integer, PgxVertex> superNodeMapping,
      VertexSet superNodes, @Out VertexProperty<@Length("k") PgxVect<Integer>> distances) {
    g.getVertices().forEach(n -> {
      @Length("k")
      PgxVect<Integer> tmp = PgxVect.create(Integer.MAX_VALUE);
      distances.set(n, tmp);
    });

    Scalar<Integer> i = Scalar.create(0);

    while (i.get() < k) {
      PgxVertex superNode = superNodeMapping.get(i.get());

      inBFS(g, superNode).direction(Traversal.Direction.IN_OUT_EDGES).forward(n -> {
        @Length("k")
        PgxVect<Integer> tmp = distances.get(n);
        tmp.set(i.get(), currentLevel());
        distances.set(n, tmp);
      });

      i.increment();
    }
  }
}

/*
// use graph instead of dGraph until GM-16698 is fixed
proc createDistanceIndex(graph g, int k, map<int, node> superNodeMapping, nodeSet superNodes; nodeProp<vect<int>[k]>
distances) {

  foreach(n: g.nodes) {
    iVect[k] tmp = INF;
    n.distances = tmp;
  }

  int i = 0;
  while (i < k) {
    node superNode = superNodeMapping[i];
    superNodes.add(superNode);

    inBFS(n: g.nodes from superNode using inOutEdges) {
      vect<int>[k] tmp = n.distances;
      tmp[i] = currentLevel();
      n.distances = tmp;
    }
    i++;
  }
}
*/
```
