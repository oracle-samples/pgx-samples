# Enumerate Simple Paths

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_o12_enumerate_simple_paths
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#enumerateSimplePaths(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, int k, VertexSet<ID> verticesOnPath, EdgeSet edgesOnPath, PgxMap<PgxVertex<ID>, Integer> dist)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#enumerateSimplePaths_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_PgxVertex_int_oracle_pgx_api_VertexSet_oracle_pgx_api_EdgeSet_oracle_pgx_api_PgxMap_)

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

Not available in PGX Algorithm API.
