# Bidirectional Filtered Dijkstra Algorithm

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p2b_single_source_single_destination_filtered_bidirectional_dijkstra
- **Time Complexity:** O(E + V log V) with V = number of vertices, E = number of edges
- **Space Requirement:** O(10 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#shortestPathFilteredDijkstraBidirectional(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, EdgeProperty<java.lang.Double> cost, GraphFilter filterExpr)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#shortestPathFilteredDijkstraBidirectional-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.PgxVertex-oracle.pgx.api.EdgeProperty-oracle.pgx.api.filter.GraphFilter-)
  - [Analyst#shortestPathFilteredDijkstraBidirectional(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, EdgeProperty<java.lang.Double> cost, GraphFilter filterExpr, VertexProperty<ID,PgxVertex<ID>> parent, VertexProperty<ID,PgxEdge> parentEdge)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#shortestPathFilteredDijkstraBidirectional-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.PgxVertex-oracle.pgx.api.EdgeProperty-oracle.pgx.api.filter.GraphFilter-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexProperty-)

This variant of the Dijkstra's algorithm searches for shortest path in two ways, it does a forward search from the source vertex and a backwards one from the destination vertex, while also adding the corresponding restrictions on the edges given by the filter expression. If the path between the vertices exists, both searches will meet each other at an intermediate point.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `weight` | edgeProp<double> | edge property holding the (positive) weight of each edge in the graph. |
| `src` | node | the source vertex from the graph for the path. |
| `dst` | node | the destination vertex from the graph for the path. |
| `filter` | edgeFilter | filter expression with conditions to be satisfied by the shortest path. If the expression is targeted to edges, it will be evaluated straight away. If the expression targets vertices, then it will be automatically translated into an equivalent edge expression by using the sources and/or the destinations of the edges from the current evaluated vertex, with exception of the source and destination vertices. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `parent` | vertexProp<node> | vertex property holding the parent vertex of the each vertex in the shortest path. |
| `parent_edge` | vertexProp<edge> | vertex property holding the edge ID linking the current vertex in the path with the previous vertex in the path. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | bool | true if there is a path connecting source and destination vertices, false otherwise |

## Code

