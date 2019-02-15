/*
 * Copyright (C) 2019 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.api.beta.EdgeProperty;
import oracle.pgx.api.beta.PgxEdge;
import oracle.pgx.api.beta.PgxGraph;
import oracle.pgx.api.beta.PgxVect;
import oracle.pgx.api.beta.PgxVertex;
import oracle.pgx.api.beta.Scalar;
import oracle.pgx.api.beta.VertexProperty;
import oracle.pgx.api.beta.annotations.GraphAlgorithm;
import oracle.pgx.api.beta.annotations.Length;
import oracle.pgx.api.beta.annotations.Out;

import static java.lang.Math.sqrt;
import static oracle.pgx.api.beta.Random.uniformVector;

@GraphAlgorithm
public class MatrixFactorizationGradientDescent {
  public double matrix_factorization_gradient_descent(PgxGraph G, VertexProperty<Boolean> is_left,
      EdgeProperty<Double> weight, double learning_rate, double change_per_step, double lambda, int max_step,
      int vector_length, @Out VertexProperty<@Length("vector_length") PgxVect<Double>> dest_property) {
    VertexProperty<@Length("vector_length") PgxVect<Double>> dest_property_next = VertexProperty.create();
    G.getVertices().forSequential(n -> {
      dest_property.set(n, uniformVector());
    });

    dest_property_next.setAll(dest_property::get);
    double max_value = 5.0;
    double min_value = 1.0;
    Scalar<Double> rate = Scalar.create(learning_rate);
    int counter = 0;

    Scalar<Double> root_mean_square_error = Scalar.create(0.0);
    while (counter < max_step) {
      G.getVertices().forEach(curr_node -> {
        if (is_left.get(curr_node)) {
          @Length("vector_length") PgxVect<Double> Z = PgxVect.create(0.0);

          curr_node.getOutEdges().forSequential(curr_edge -> {
            PgxVertex opposite_node = curr_edge.destinationVertex();

            double weight_1 = weight.get(curr_edge);
            double weight_2 = dest_property.get(curr_node).multiply(dest_property.get(opposite_node));

            if (weight_2 > max_value) {
              weight_2 = max_value;
            } else if (weight_2 < min_value) {
              weight_2 = min_value;
            }

            Z.reduceAdd(dest_property.get(opposite_node).multiply(weight_1 - weight_2)
                .subtract(dest_property.get(curr_node).multiply(lambda)));

            root_mean_square_error.reduceAdd((weight_1 - weight_2) * (weight_1 - weight_2));

            // use this local procedure instead once GM-12295 get resolved
            //root_mean_square_error = update_vector(G, curr_node, opposite_node, curr_edge, lambda, max_value,
            //    min_value, root_mean_square_error, weight, vector_length, Z, dest_property);
          });
          dest_property_next.set(curr_node, dest_property.get(curr_node).add(Z.multiply(rate.get())));
        } else {
          @Length("vector_length") PgxVect<Double> Z = PgxVect.create(0.0);

          curr_node.getInEdges().forSequential(curr_edge -> {
            PgxVertex opposite_node = curr_edge.sourceVertex();

            double weight_1 = weight.get(curr_edge);
            double weight_2 = dest_property.get(curr_node).multiply(dest_property.get(opposite_node));

            if (weight_2 > max_value) {
              weight_2 = max_value;
            } else if (weight_2 < min_value) {
              weight_2 = min_value;
            }

            Z.reduceAdd((dest_property.get(opposite_node).multiply(weight_1 - weight_2)
                .subtract(dest_property.get(curr_node).multiply(lambda))));

            root_mean_square_error.reduceAdd((weight_1 - weight_2) * (weight_1 - weight_2));

            // use this local procedure instead once GM-12295 get resolved
            //root_mean_square_error = update_vector(G, curr_node, opposite_node, curr_edge, lambda, max_value,
            //    min_value, root_mean_square_error, weight, vector_length, Z, dest_property);
          });

          dest_property_next.set(curr_node, dest_property.get(curr_node).add(Z.multiply(rate.get())));
        }
      });

      dest_property.setAll(dest_property_next::get);
      root_mean_square_error.set(sqrt(root_mean_square_error.get() / (G.getNumEdges() * 2.0)));
      rate.reduceMul(change_per_step);
      counter++;
    }
    return root_mean_square_error.get();
  }

  private double update_vector(PgxGraph G, PgxVertex curr_node, PgxVertex opposite_node, PgxEdge curr_edge,
      double lambda, double max_value, double min_value, double root_mean_square_error, EdgeProperty<Double> weight,
      int vector_length, @Out @Length("vector_length") PgxVect<Double> Z,
      VertexProperty<@Length("vector_length") PgxVect<Double>> dest_property) {

    double weight_1 = weight.get(curr_edge);
    double weight_2 = dest_property.get(curr_node).multiply(dest_property.get(opposite_node));

    if (weight_2 > max_value) {
      weight_2 = max_value;
    } else if (weight_2 < min_value) {
      weight_2 = min_value;
    }

    Z.reduceAdd(dest_property.get(opposite_node).multiply(weight_1 - weight_2)
        .subtract(dest_property.get(curr_node).multiply(lambda)));

    return root_mean_square_error + (weight_1 - weight_2) * (weight_1 - weight_2);
  }
}
