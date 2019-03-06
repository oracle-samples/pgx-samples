/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
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
      EdgeProperty<Double> weight, double learning_rate, double change_per_step, int max_step,
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

            double rating = weight.get(curr_edge);
            double rating_hat = dest_property.get(curr_node).multiply(dest_property.get(opposite_node));

            if (rating_hat > max_value) {
              rating_hat = max_value;
            } else if (rating_hat < min_value) {
              rating_hat = min_value;
            }

            Z.reduceAdd(dest_property.get(opposite_node).multiply(rating - rating_hat).multiply(2));

            root_mean_square_error.reduceAdd((rating - rating_hat) * (rating - rating_hat));

          });
          dest_property_next.set(curr_node, dest_property.get(curr_node).add(Z.multiply(rate.get())));
        } else {
          @Length("vector_length") PgxVect<Double> Z = PgxVect.create(0.0);

          curr_node.getInEdges().forSequential(curr_edge -> {
            PgxVertex opposite_node = curr_edge.sourceVertex();

            double rating = weight.get(curr_edge);
            double rating_hat = dest_property.get(curr_node).multiply(dest_property.get(opposite_node));

            if (rating_hat > max_value) {
              rating_hat = max_value;
            } else if (rating_hat < min_value) {
              rating_hat = min_value;
            }

            Z.reduceAdd(dest_property.get(opposite_node).multiply(rating - rating_hat).multiply(2));

            root_mean_square_error.reduceAdd((rating - rating_hat) * (rating - rating_hat));

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
}
