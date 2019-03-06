/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** This software is licensed to you under the Universal Permissive License (UPL).
 ** See below for license terms.
 **  ____________________________
 ** The Universal Permissive License (UPL), Version 1.0

 ** Subject to the condition set forth below, permission is hereby granted to any person
 ** obtaining a copy of this software, associated documentation and/or data (collectively the "Software"),
 ** free of charge and under any and all copyright rights in the Software, and any and all patent rights
 ** owned or freely licensable by each licensor hereunder covering either (i) the unmodified Software as
 ** contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both

 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the
 ** Software (each a "Larger Work" to which the Software is contributed by such licensors),

 ** without restriction, including without limitation the rights to copy, create derivative works of,
 ** display, perform, and distribute the Software and make, use, sell, offer for sale, import, export,
 ** have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights
 ** on either these or other terms.

 ** This license is subject to the following condition:

 ** The above copyright notice and either this complete permission notice or at a minimum a reference
 ** to the UPL must be included in all copies or substantial portions of the Software.

 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 ** NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 ** IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 ** WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 ** SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
