# Matrix Factorization (Gradient Descent)

- **Category:** matrix factorization
- **Algorithm ID:** pgx_builtin_r2a_matrix_factorization_training
- **Time Complexity:** O(E * k * s) with E = number of edges, k = maximum number of iteration, s = size of the feature vectors
- **Space Requirement:** O(2 * V * s) with V = number of vertices, s = size of the feature vectors
- **Javadoc:**
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.EdgeProperty-)

This algorithm needs a [bipartite](prog-guides/mutation-subgraph/subgraph.html#create-a-bipartite-subgraph-based-on-a-vertex-list) graph to generate feature vectors that factorize the given set of left vertices (users) and right vertices (items), so that the inner product of such feature vectors can recover the information from the original graph structure, which can be seen as a sparse matrix. The generated feature vectors can be used for making recommendations with the given set of users, where a good recommendation for a given user will be a dot (inner) product between the feature vector of the user and the corresponding feature vector of a vertex from the item set, such that the result of that dot product returns a high score.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `is_left` | vertexProp<node> | boolean vertex property stating the side of the vertices in the [bipartite](prog-guides/mutation-subgraph/subgraph.html#create-a-bipartite-subgraph-based-on-a-vertex-list) graph (left for users, right for items). |
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

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */

procedure matrix_factorization_gradient_descent(graph G, vertexProp<bool> is_left, edgeProp<double> weight,
    double learning_rate, double change_per_step, double lambda, int max_step, int vector_length;
    vertexProp<vect<double>[vector_length]> dest_property) : double {

  vertexProp<vect<double>[vector_length]> dest_property_next;
  for (n: G.nodes) {
    n.dest_property = uniform();
  }

  G.dest_property_next = _.dest_property;
  double max_value = 5.0;
  double min_value = 1.0;
  double rate = learning_rate;
  int counter = 0;

  double root_mean_square_error = 0.0;
  while (counter < max_step) {

    foreach (curr_node: G.nodes) {
      if (curr_node.is_left) {
        vect<double>[vector_length] Z = 0.0;

        for (curr_edge: curr_node.outEdges) {
          node opposite_node = curr_edge.toNode();

          double weight_1 = curr_edge.weight;
          double weight_2 = curr_node.dest_property * opposite_node.dest_property;

          if (weight_2 > max_value) {
            weight_2 = max_value;
          } else if (weight_2 < min_value) {
            weight_2 = min_value;
          }

          Z += ((weight_1 - weight_2) * opposite_node.dest_property - lambda * curr_node.dest_property);

          root_mean_square_error += (weight_1 - weight_2) * (weight_1 - weight_2);

          // use this local procedure instead once GM-12295 get resolved
          //root_mean_square_error = update_vector(G, curr_node, opposite_node, curr_edge, lambda, max_value,
          //    min_value, root_mean_square_error, weight, vector_length, Z, dest_property);
        }
        curr_node.dest_property_next = curr_node.dest_property + rate * Z;
      } else {
        vect<double>[vector_length] Z = 0.0;

        for (curr_edge: curr_node.inEdges) {
          node opposite_node = curr_edge.fromNode();

          double weight_1 = curr_edge.weight;
          double weight_2 = curr_node.dest_property * opposite_node.dest_property;

          if (weight_2 > max_value) {
            weight_2 = max_value;
          } else if (weight_2 < min_value) {
            weight_2 = min_value;
          }

          Z += ((weight_1 - weight_2) * opposite_node.dest_property - lambda * curr_node.dest_property);

          root_mean_square_error += (weight_1 - weight_2) * (weight_1 - weight_2);

          // use this local procedure instead once GM-12295 get resolved
          //root_mean_square_error = update_vector(G, curr_node, opposite_node, curr_edge, lambda, max_value,
          //    min_value, root_mean_square_error, weight, vector_length, Z, dest_property);
        }
        curr_node.dest_property_next = curr_node.dest_property + rate * Z;
      }
    }
    G.dest_property = _.dest_property_next;
    root_mean_square_error = sqrt(root_mean_square_error / (G.numEdges() * 2.0));
    rate *= change_per_step;
    counter++;
  }
  return root_mean_square_error;
}

local update_vector(graph G, node curr_node, node opposite_node, edge curr_edge, double lambda, double max_value,
    double min_value, double root_mean_square_error, edgeProp<double> weight, int vector_length;
    vect<double>[vector_length] Z, vertexProp<vect<double>[vector_length]> dest_property) : double {

  double weight_1 = curr_edge.weight;
  double weight_2 = curr_node.dest_property * opposite_node.dest_property;

  if (weight_2 > max_value) {
    weight_2 = max_value;
  } else if (weight_2 < min_value) {
    weight_2 = min_value;
  }

  Z += (weight_1 - weight_2) * opposite_node.dest_property - lambda * curr_node.dest_property;

  return root_mean_square_error + (weight_1 - weight_2) * (weight_1 - weight_2);
}
```
