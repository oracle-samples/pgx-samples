# Filtered DFS (Depth-First Search)

- **Category:** other
- **Algorithm ID:** pgx_builtin_o2_filtered_dfs
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#filteredDfs(PgxGraph graph, PgxVertex<ID> root)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#filteredDfs-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-)
  - [Analyst#filteredDfs(PgxGraph graph, PgxVertex<ID> root, int maxDepth)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#filteredDfs-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-int-)
  - [Analyst#filteredDfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter filter, VertexFilter navigator)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#filteredDfs-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.filter.VertexFilter-oracle.pgx.api.filter.VertexFilter-)
  - [Analyst#filteredDfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter filter, VertexFilter navigator, boolean initWithInf)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#filteredDfs-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.filter.VertexFilter-oracle.pgx.api.filter.VertexFilter-boolean-)
  - [Analyst#filteredDfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter filter, VertexFilter navigator, boolean initWithInf, int maxDepth)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#filteredDfs-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.filter.VertexFilter-oracle.pgx.api.filter.VertexFilter-boolean-int-)
  - [Analyst#filteredDfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter filter, VertexFilter navigator, boolean initWithInf, int maxDepth, VertexProperty<ID,java.lang.Integer> distance, VertexProperty<ID,PgxVertex<ID>> parent)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#filteredDfs-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.filter.VertexFilter-oracle.pgx.api.filter.VertexFilter-boolean-int-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexProperty-)
  - [Analyst#filteredDfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter filter, VertexFilter navigator, boolean initWithInf, VertexProperty<ID,java.lang.Integer> distance, VertexProperty<ID,PgxVertex<ID>> parent)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#filteredDfs-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.filter.VertexFilter-oracle.pgx.api.filter.VertexFilter-boolean-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexProperty-)
  - [Analyst#filteredDfs-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.filter.VertexFilter-oracle.pgx.api.filter.VertexFilter-int-](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#filteredDfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter filter, VertexFilter navigator, int maxDepth))

This filtered version of the DFS algorithm allows to use a filter and a navigator expression to be evaluated over the vertices during the traversal and discriminate them according to the desired criteria. It will return the distance to the source vertex and the corresponding parent vertex for all the filtered vertices.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `root` | node | the source vertex from the graph for the path. |
| `init_with_inf` | bool | boolean flag to set the initial distance values of the vertices. If set to true, it will initialize the distances as INF, and -1 otherwise. |
| `filter` | vertexFilter | (deprecated) filter expression to be evaluated on the vertices during the graph traversal. The filter expression has no effect; use the navigator instead to filter out vertices. |
| `navigator` | vertexFilter | navigator expression to be evaluated on the vertices during the graph traversal. |
| `max_depth` | int | maximum depth limit for the BFS traversal. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `dist` | vertexProp<int> | vertex property holding the hop distance for each reachable vertex in the graph. |
| `parent` | vertexProp<node> | vertex property holding the parent vertex of the each reachable vertex in the path. |

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
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.filter.VertexFilter;

import static oracle.pgx.algorithm.Traversal.inDFS;
import static java.lang.Integer.MAX_VALUE;

@GraphAlgorithm
public class FilteredDfs {
  public void filteredDfs(PgxGraph g, PgxVertex root, boolean initWithInf, VertexFilter filter, VertexFilter navigator,
      int maxDepth, @Out VertexProperty<Integer> dist, @Out VertexProperty<PgxVertex> parent) {
    if (g.getNumVertices() == 0) {
      return;
    }

    VertexProperty<Boolean> visited = VertexProperty.create(false);

    dist.setAll(initWithInf ? MAX_VALUE : -1);
    parent.setAll(PgxVertex.NONE);

    dist.set(root, 0);
    Scalar<Integer> depth = Scalar.create(0);

    inDFS(g, root).navigator(n -> navigator.evaluate(n) && depth.get() <= maxDepth)
        .filter(n -> navigator.evaluate(n) || filter.evaluate(n)).forward(n -> {
          if (n != root) {
            n.getInNeighbors().forSequential(m -> {
              if (visited.get(m) && dist.get(m) < maxDepth && dist.get(m) >= depth.get() - 1) {
                parent.set(n, m);
              }
            });
          }
          if (parent.get(n) != PgxVertex.NONE) {
            PgxVertex p = parent.get(n);
            dist.set(n, dist.get(p) + 1);
          }

          depth.increment();
          visited.set(n, true);
        }).backward(n -> depth.decrement());
  }
}
```
