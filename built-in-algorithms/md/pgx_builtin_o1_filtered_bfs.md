# Filtered BFS (Breadth-First Search)

- **Category:** other
- **Algorithm ID:** pgx_builtin_o1_filtered_bfs
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#filteredBfs(PgxGraph graph, PgxVertex<ID> root)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#filteredBfs_oracle_pgx_api_PgxGraph_ID_)
  - [Analyst#filteredBfs(PgxGraph graph, PgxVertex<ID> root, int maxDepth)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#filteredBfs_oracle_pgx_api_PgxGraph_ID_int_)
  - [Analyst#filteredBfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter navigator)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#filteredBfs_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_filter_VertexFilter_)
  - [Analyst#filteredBfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter navigator, boolean initWithInf, int maxDepth)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#filteredBfs_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_filter_VertexFilter_boolean_int_)
  - [Analyst#filteredBfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter navigator, boolean initWithInf, int maxDepth, VertexProperty<ID,​java.lang.Integer> distance, VertexProperty<ID,​PgxVertex<ID>> parent)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#filteredBfs_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_filter_VertexFilter_boolean_int_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#filteredBfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter navigator, boolean initWithInf, VertexProperty<ID,​java.lang.Integer> distance, VertexProperty<ID,​PgxVertex<ID>> parent)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#filteredBfs_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_filter_VertexFilter_boolean_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#filteredBfs(PgxGraph graph, PgxVertex<ID> root, VertexFilter navigator, int maxDepth)](https://docs.oracle.com/en/database/oracle/property-graph/24.4spgjv/oracle/pgx/api/Analyst.html#filteredBfs_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_filter_VertexFilter_int_)

This filtered version of the BFS algorithm allows to use a filter and a navigator expression to be evaluated over the vertices during the traversal and discriminate them according to the desired criteria. It will return the distance to the source vertex and the corresponding parent vertex for all the filtered vertices.

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
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.filter.VertexFilter;

import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;
import static java.lang.Integer.MAX_VALUE;

@GraphAlgorithm
public class FilteredBfs {
  public void filteredBfs(PgxGraph g, PgxVertex root, boolean initWithInf, VertexFilter filter, VertexFilter navigator,
      int maxDepth, @Out VertexProperty<Integer> dist, @Out VertexProperty<PgxVertex> parent) {
    if (g.getNumVertices() == 0) {
      return;
    }

    dist.setAll(initWithInf ? MAX_VALUE : -1);
    parent.setAll(PgxVertex.NONE);

    dist.set(root, 0);

    inBFS(g, root).navigator(n -> navigator.evaluate(n) && currentLevel() < maxDepth)
        .filter(n -> navigator.evaluate(n) || filter.evaluate(n)).forward(n -> {
          dist.set(n, currentLevel());
          parent.set(n, n.parentVertex());
        });
  }
}
```
