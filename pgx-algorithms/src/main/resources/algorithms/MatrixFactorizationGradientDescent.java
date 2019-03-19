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
      EdgeProperty<Double> weight, double learning_rate, double change_per_step, double lambda, int max_step,
      int vector_length, @Out VertexProperty<@Length("vector_length") PgxVect<Double>> features) {

    G.getVertices().forSequential(n -> {
      features.set(n, uniformVector());
    });

    Scalar<Double> rate = Scalar.create(learning_rate);
    int counter = 0;
    Scalar<Double> root_mean_square_error = Scalar.create(0.0);

    while (counter < max_step) {

      root_mean_square_error.set(0.0);
      G.getEdges().forEach(e -> {

        PgxVertex src = e.sourceVertex();
        PgxVertex dst = e.destinationVertex();

        double rating = weight.get(e);
        double estimate = features.get(src).multiply(features.get(dst));

        features.set(src, features.get(src).add(features.get(dst).multiply((rating - estimate) * rate.get())
            .subtract(features.get(src).multiply(lambda * rate.get()))));
        features.set(dst, features.get(dst).add(features.get(src).multiply((rating - estimate) * rate.get())
            .subtract(features.get(dst).multiply(lambda * rate.get()))));

        root_mean_square_error.reduceAdd((rating - estimate) * (rating - estimate));
      });
      rate.reduceMul(change_per_step);
      counter++;
    }
    return sqrt(root_mean_square_error.get() / (G.getNumEdges() * 1.0));
  }
}
