# Speaker Listener Label Propagation

- **Category:** community detection
- **Algorithm ID:** pgx_builtin_c6_weighted_speaker_listener_label_propagation
- **Time Complexity:** O(max_iter * N) with N = number of neighbors of vertex1, max_iter = number of iterations
- **Space Requirement:** O(N + V * max_iter) with N = number of neighbors of vertex1, max_iter = number of iterations
- **Javadoc:**
  - [Analyst#weightedSpeakerListenerLabelPropagation(PgxGraph graph, String labelPropName, EdgeProperty weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#weightedSpeakerListenerLabelPropagation_oracle_pgx_api_PgxGraph_java_lang_String_oracle_pgx_api_EdgeProperty_)
  - [Analyst#weightedSpeakerListenerLabelPropagation(PgxGraph graph, java.lang.String labelsPropName, int maxIter, double threshold, java.lang.String delimiter, EdgeProperty weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#weightedSpeakerListenerLabelPropagation_oracle_pgx_api_PgxGraph_java_lang_String_int_double_java_lang_String_oracle_pgx_api_EdgeProperty_)
  - [Analyst#weightedSpeakerListenerLabelPropagation(PgxGraph graph, VertexProperty labels, int maxIter, double threshold, java.lang.String delimiter, EdgeProperty weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#weightedSpeakerListenerLabelPropagation_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_int_double_java_lang_String_oracle_pgx_api_EdgeProperty_)

The speaker listener label propagation algorithm is an extension of the label propagation algorithm, which is able to detect overlapping communities. This variant uses the edges weight to find those communities.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `g` | graph | the graph. |
| `max_iter` | int | number of iterations. |
| `threshold` | double | minimum frequency for a label to retain a vertex. |
| `delimiter` | string | string delimiter for labels. |
| `weight` | edgeProp | edge property holding the weight of each edge in the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `labels` | vertexProp<string> | distinct vertex labels which meet the threshold parameter. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

Not available in PGX Algorithm API.
