# Matrix Factorization (Gradient Descent)

- **Category:** matrix factorization
- **Algorithm ID:** pgx_builtin_r2a_matrix_factorization_training
- **Time Complexity:** O(E * k * s) with E = number of edges, k = maximum number of iteration, s = size of the feature vectors
- **Space Requirement:** O(2 * V * s) with V = number of vertices, s = size of the feature vectors
- **Javadoc:**
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent_oracle_pgx_api_BipartiteGraph_oracle_pgx_api_EdgeProperty_)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight, double learningRate, double changePerStep, double lambda, int maxStep, int vectorLength)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent_oracle_pgx_api_BipartiteGraph_oracle_pgx_api_EdgeProperty_double_double_double_int_int_)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight, double learningRate, double changePerStep, double lambda, int maxStep, int vectorLength, VertexProperty<ID,​PgxVect<java.lang.Double>> features)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent_oracle_pgx_api_BipartiteGraph_oracle_pgx_api_EdgeProperty_double_double_double_int_int_oracle_pgx_api_VertexProperty_)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,​PgxVect<java.lang.Double>> features)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent_oracle_pgx_api_BipartiteGraph_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_)

This algorithm needs a [bipartite](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgdg/graph-mutation-and-subgraphs.html) graph to generate feature vectors that factorize the given set of left vertices (users) and right vertices (items), so that the inner product of such feature vectors can recover the information from the original graph structure, which can be seen as a sparse matrix. The generated feature vectors can be used for making recommendations with the given set of users, where a good recommendation for a given user will be a dot (inner) product between the feature vector of the user and the corresponding feature vector of a vertex from the item set, such that the result of that dot product returns a high score.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `is_left` | vertexProp<node> | boolean vertex property stating the side of the vertices in the [bipartite](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgdg/graph-mutation-and-subgraphs.html) graph (left for users, right for items). |
| `weight` | edgeProp<double> | edge property holding the rating weight of each edge in the graph. The weight needs to be pre-scaled into the range 1-5. If the weight values are not between 1 and 5, the result will become inaccurate. |
| `learning_rate` | double | learning rate for the optimization process. |
| `change_per_step` | double | parameter used to modulate the learning rate during the optimization process. |
| `lambda` | double | penalization parameter to avoid overfitting during optimization process. |
| `max_step` | int | maximum number of iterations that will be performed. |
| `vector_length` | int | size of the feature vectors to be generated for the factorization. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `dest_property` | vertexProp<vect<double>[vector_length]> | vertex property holding the generated feature vectors for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | double | the root mean square error. |

## Code

Not available in PGX Algorithm API.
