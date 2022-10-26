# Speaker Listener Label Propagation

- **Category:** community detection
- **Algorithm ID:** pgx_builtin_c5_speaker_listener_label_propagation
- **Time Complexity:** O(max_iter * N) with N = number of neighbors of vertex1, max_iter = number of iterations
- **Space Requirement:** O(N + V * max_iter) with N = number of neighbors of vertex1, max_iter = number of iterations
- **Javadoc:** 
  - [Analyst#speakerListenerLabelPropagation(PgxGraph graph, String labelPropName)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#speakerListenerLabelPropagation-oracle.pgx.api.PgxGraph-java.lang.String-)
  - [Analyst#speakerListenerLabelPropagation(PgxGraph graph, int maxIter, double threshold, java.lang.String delimiter, java.lang.String labelsPropName)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#speakerListenerLabelPropagation-oracle.pgx.PgxGraph-int-double-java.lang.String-java.lang.String-)
  - [Analyst#speakerListenerLabelPropagation(PgxGraph graph, int maxIter, double threshold, java.lang.String delimiter, VertexProperty<ID, java.lang.String> labels)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#speakerListenerLabelPropagation-oracle.pgx.api.PgxGraph-int-double-java.lang.String-oracle.pgx.api.VertexProperty-)

The speaker listener label propagation algorithm is an extension of the label propagation algorithm, which is able to detect overlapping communities.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `g` | graph | the graph. |
| `max_iter` | int | number of iterations. |
| `threshold` | double | minimum frequency for a label to retain a vertex. |
| `delimiter` | string | string delimiter for labels. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `labels` | vertexProp<string> | distinct vertex labels which meet the threshold parameter. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

Not available in PGX Alogrithm API.
