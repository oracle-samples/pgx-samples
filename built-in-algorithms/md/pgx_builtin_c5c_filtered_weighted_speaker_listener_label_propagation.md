# Filtered Weighted Speaker Listener Label Propagation

- **Category:** community detection
- **Algorithm ID:** pgx_builtin_c5c_filtered_weighted_speaker_listener_label_propagation
- **Time Complexity:** O(max_iter * N) with N = number of neighbors of vertex1, max_iter = number of iterations
- **Space Requirement:** O(N + V * max_iter) with N = number of neighbors of vertex1, max_iter = number of iterations
- **Javadoc:**
  - [Analyst#filteredWeightedSpeakerListenerLabelPropagation(PgxGraph graph, String labelPropName, EdgeProperty weight, EdgeFilter filter)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#filteredWeightedSpeakerListenerLabelPropagation-oracle.pgx.api.PgxGraph-java.lang.String-oracle.pgx.api.EdgeProperty-oracle.pgx.api.filter.EdgeFilter-)
  - [Analyst#filteredWeightedSpeakerListenerLabelPropagation(PgxGraph graph, java.lang.String labelsPropName, int maxIter, double threshold, java.lang.String delimiter, EdgeProperty weight, EdgeFilter filter)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#filteredWeightedSpeakerListenerLabelPropagation-oracle.pgx.PgxGraph-java.lang.String-int-double-java.lang.String-oracle.pgx.api.EdgeProperty-oracle.pgx.api.filter.EdgeFilter-)
  - [Analyst#filteredWeightedSpeakerListenerLabelPropagation(PgxGraph graph, VertexProperty labels, int maxIter, double threshold, java.lang.String delimiter, EdgeProperty weight, EdgeFilter filter)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#filteredWeightedSpeakerListenerLabelPropagation-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-int-double-java.lang.String-oracle.pgx.api.EdgeProperty-oracle.pgx.api.filter.EdgeFilter-)

The speaker listener label propagation algorithm is an extension of the label propagation algorithm, which is able to detect overlapping communities. This variant utilizes an edge filter to select which neighbors to listen to and uses the edges' weight to update the frequency of listened labels.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `g` | graph | the graph. |
| `max_iter` | int | number of iterations. |
| `threshold` | double | minimum frequency for a label to retain a vertex. |
| `delimiter` | string | string delimiter for labels. |
| `filter` | edgeFilter | The filter to be used on edges when listening to neighbors. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `labels` | vertexProp<string> | distinct vertex labels which meet the threshold parameter. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

Not available in PGX Algorithm API.
