# Matrix Factorization (Gradient Descent)

- **Category:** matrix factorization
- **Algorithm ID:** pgx_builtin_r2a_matrix_factorization_training
- **Time Complexity:** O(E * k * s) with E = number of edges, k = maximum number of iteration, s = size of the feature vectors
- **Space Requirement:** O(2 * V * s) with V = number of vertices, s = size of the feature vectors
- **Javadoc:** 
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#matrixFactorizationGradientDescent(BipartiteGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#matrixFactorizationGradientDescent-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.EdgeProperty-)

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
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVect;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Length;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.sqrt;
import static oracle.pgx.algorithm.Random.uniformVector;

@GraphAlgorithm
public class MatrixFactorizationGradientDescent {
  public double matrixFactorizationGradientDescent(PgxGraph g, VertexProperty<Boolean> isLeft,
      EdgeProperty<Double> weight, double learningRate, double changePerStep, double lambda, int maxStep,
      int vectorLength, @Out VertexProperty<@Length("vectorLength") PgxVect<Double>> destProperty) {
    VertexProperty<@Length("vectorLength") PgxVect<Double>> destPropertyNext = VertexProperty.create();
    g.getVertices().forSequential(n -> {
      destProperty.set(n, uniformVector());
    });

    destPropertyNext.setAll(destProperty::get);
    double maxValue = 5.0;
    double minValue = 1.0;
    Scalar<Double> rate = Scalar.create(learningRate);
    int counter = 0;

    Scalar<Double> rootMeanSquareError = Scalar.create(0.0);
    while (counter < maxStep) {
      g.getVertices().forEach(currNode -> {
        if (isLeft.get(currNode)) {
          @Length("vectorLength")
          PgxVect<Double> z = PgxVect.create(0.0);

          currNode.getOutEdges().forSequential(currEdge -> {
            PgxVertex oppositeNode = currEdge.destinationVertex();

            double weight1 = weight.get(currEdge);
            double weight2 = destProperty.get(currNode).multiply(destProperty.get(oppositeNode));

            if (weight2 > maxValue) {
              weight2 = maxValue;
            } else if (weight2 < minValue) {
              weight2 = minValue;
            }

            z.reduceAdd(destProperty.get(oppositeNode).multiply(weight1 - weight2)
                .subtract(destProperty.get(currNode).multiply(lambda)));

            rootMeanSquareError.reduceAdd((weight1 - weight2) * (weight1 - weight2));

            // use this local procedure instead once GM-12295 get resolved
            //rootMeanSquareError = updateVector(g, currNode, oppositeNode, currEdge, lambda, maxValue,
            //    minValue, rootMeanSquareError, weight, vectorLength, z, destProperty);
          });
          destPropertyNext.set(currNode, destProperty.get(currNode).add(z.multiply(rate.get())));
        } else {
          @Length("vectorLength")
          PgxVect<Double> z = PgxVect.create(0.0);

          currNode.getInEdges().forSequential(currEdge -> {
            PgxVertex oppositeNode = currEdge.sourceVertex();

            double weight1 = weight.get(currEdge);
            double weight2 = destProperty.get(currNode).multiply(destProperty.get(oppositeNode));

            if (weight2 > maxValue) {
              weight2 = maxValue;
            } else if (weight2 < minValue) {
              weight2 = minValue;
            }

            z.reduceAdd((destProperty.get(oppositeNode).multiply(weight1 - weight2)
                .subtract(destProperty.get(currNode).multiply(lambda))));

            rootMeanSquareError.reduceAdd((weight1 - weight2) * (weight1 - weight2));

            // use this local procedure instead once GM-12295 get resolved
            //rootMeanSquareError = updateVector(g, currNode, oppositeNode, currEdge, lambda, maxValue,
            //    minValue, rootMeanSquareError, weight, vectorLength, Z, destProperty);
          });

          destPropertyNext.set(currNode, destProperty.get(currNode).add(z.multiply(rate.get())));
        }
      });

      destProperty.setAll(destPropertyNext::get);
      rootMeanSquareError.set(sqrt(rootMeanSquareError.get() / (g.getNumEdges() * 2.0)));
      rate.reduceMul(changePerStep);
      counter++;
    }
    return rootMeanSquareError.get();
  }

  private double updateVector(PgxGraph g, PgxVertex currNode, PgxVertex oppositeNode, PgxEdge currEdge, double lambda,
      double maxValue, double minValue, double rootMeanSquareError, EdgeProperty<Double> weight, int vectorLength,
      @Out @Length("vectorLength") PgxVect<Double> z,
      VertexProperty<@Length("vectorLength") PgxVect<Double>> destProperty) {

    double weight1 = weight.get(currEdge);
    double weight2 = destProperty.get(currNode).multiply(destProperty.get(oppositeNode));

    if (weight2 > maxValue) {
      weight2 = maxValue;
    } else if (weight2 < minValue) {
      weight2 = minValue;
    }

    z.reduceAdd(destProperty.get(oppositeNode).multiply(weight1 - weight2)
        .subtract(destProperty.get(currNode).multiply(lambda)));

    return rootMeanSquareError + (weight1 - weight2) * (weight1 - weight2);
  }
}
```
