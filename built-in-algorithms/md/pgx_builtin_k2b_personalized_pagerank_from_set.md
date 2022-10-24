# Personalized PageRank (for a set of vertices)

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k2b_personalized_pagerank_from_set
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-boolean-)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-boolean-)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, boolean norm, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-boolean-oracle.pgx.api.VertexProperty-)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-double-double-int-)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, boolean norm)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-double-double-int-boolean-)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, boolean norm, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-double-double-int-boolean-oracle.pgx.api.VertexProperty-)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, double e, double d, int max, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-double-double-int-oracle.pgx.api.VertexProperty-)
  - [Analyst#personalizedPagerank(PgxGraph graph, VertexSet<ID> vertices, VertexProperty<ID,java.lang.Double> rank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#personalizedPagerank-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSet-oracle.pgx.api.VertexProperty-)

The Personalized Pagerank allows to select a particular vertex or a set of vertices from the given graph in order to give them a greater importance when computing the ranking score, which will have as result a personalized Pagerank score and reveal relevant (or similar) vertices to the ones chosen at the begining.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `source` | nodeSet | the set of chosen vertices from the graph for personalization. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `damp` | double | damping factor. |
| `max_iter` | int | maximum number of iterations that will be performed. |
| `norm` | boolean | boolean flag to determine whether the algorithm will take into account dangling vertices for the ranking scores. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `rank` | vertexProp<double> | vertex property holding the (normalized) PageRank value for each vertex (a value between 0 and 1). |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

