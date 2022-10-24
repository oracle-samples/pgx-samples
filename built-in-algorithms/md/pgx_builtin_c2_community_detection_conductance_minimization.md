# Conductance Minimization (Soman and Narang Algorithm)

- **Category:** community detection
- **Algorithm ID:** pgx_builtin_c2_community_detection_conductance_minimization
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(5 * V + 2 * E) with V = number of vertices, E = number of edges
- **Javadoc:** 
  - [Analyst#communitiesConductanceMinimization(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#communitiesConductanceMinimization-oracle.pgx.api.PgxGraph-)
  - [Analyst#communitiesConductanceMinimization(PgxGraph graph, int max)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#communitiesConductanceMinimization-oracle.pgx.api.PgxGraph-int-)
  - [Analyst#communitiesConductanceMinimization(PgxGraph graph, int max, VertexProperty<ID,java.lang.Long> partitonDistribution)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#communitiesConductanceMinimization-oracle.pgx.api.PgxGraph-int-oracle.pgx.api.VertexProperty-)
  - [Analyst#communitiesConductanceMinimization(PgxGraph graph, VertexProperty<ID,java.lang.Long> partitonDistribution)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#communitiesConductanceMinimization-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

The algorithm proposed by Soman and Narang to find community structures in a graph can be regarded as a variant of the label propagation algorithm, since it takes into account weights over the edges when looking for the community assignments. This implementation generates the weight of the edges by using the triangles in the graph, and just like label propagation, it assigns a unique community label to each vertex in the graph at the beginning, which is then updated on each iteration by looking and choosing the most frequent label from the labels of their neighbors. Convergence is achieved once the label of each vertex is the same as the most frequent one amongst its neighbors, i.e. when there are no changes in the communities assigned to the vertices in one iteration.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `max_iterations` | int | maximum number of iterations that will be performed. For most graphs, a maximum of 100 iterations should be enough. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `label` | vertexProp<long> | vertex property holding the label of the community assigned to each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | long | returns the total number of communities found. |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Reduction.updateMaxValue;
import static oracle.pgx.algorithm.Traversal.inBFS;
import static java.lang.Double.NEGATIVE_INFINITY;

@GraphAlgorithm
public class SomanNarang {
  public long communityDetection(PgxGraph g, int maxIterations, @Out VertexProperty<Long> communityId) {
    VertexProperty<PgxVertex> community = VertexProperty.create();

    //count triangles for edge_weights
    EdgeProperty<Integer> triangles = EdgeProperty.create();
    g.getEdges().forEach(e -> {
      PgxVertex src = e.sourceVertex();
      PgxVertex dest = e.destinationVertex();

      Scalar<Integer> numTriangles = Scalar.create(0);
      src.getNeighbors().forEach(v -> {
        if (dest.hasEdgeTo(v)) {
          numTriangles.increment();
        }
      });
      triangles.set(e, numTriangles.get());
    });

    EdgeProperty<Double> edgeWeight = EdgeProperty.create();
    edgeWeight.setAll(0.0);
    //compute edge_weights
    g.getVertices().forEach(n -> {
      int s = n.getOutEdges().sum(triangles);
      if (s != 0) {
        n.getOutEdges().forEach(e -> edgeWeight.set(e, (double) triangles.get(e) / s));
      }
    });

    VertexProperty<PgxVertex> communityAux = VertexProperty.create();
    VertexProperty<Double> weight = VertexProperty.create();
    //compute node weights
    g.getVertices().forEach(n -> {
      community.set(n, n);
      communityAux.set(n, n);
      weight.set(n, n.getOutEdges().max(e -> (double) triangles.get(e)));
    });

    VertexProperty<Long> communityDegree = VertexProperty.create();
    communityDegree.setAll(PgxVertex::getOutDegree);

    //use BFS to give neighboring nodes with equal weights the same community
    g.getVertices().forSequential(n -> {
      if (community.get(n) == n) { //handle only unhandled nodes
        inBFS(g, n).navigator(v -> community.get(v) == v && weight.get(v) == weight.get(n)).forward(v -> {
          PgxVertex old = community.get(v);
          PgxVertex newV = community.get(n);
          community.set(v, newV);
          communityDegree.reduceAdd(old, (-1 * v.getOutDegree()));
          communityDegree.reduceAdd(newV, v.getOutDegree());
        });
      }
    });

    int iteration = 0;
    Scalar<Boolean> stable = Scalar.create(false);
    while (!stable.get() && iteration < maxIterations) {
      stable.set(true);

      g.getVertices().forEach(n -> {
        double maxValue = NEGATIVE_INFINITY;
        PgxVertex c = community.get(n);
        n.getInEdges().forSequential(e -> {
          PgxVertex i = e.sourceVertex();
          PgxVertex comm = community.get(i);
          int sumTriangles = i.getOutEdges().sum(triangles);
          if (sumTriangles != 0) {
            updateMaxValue(maxValue,
                triangles.get(e) * (1 - ((double) communityDegree.get(comm) / (2 * g.getNumEdges()))))
                .andUpdate(c, comm);
          }
        });
        communityAux.set(n, c);
      });

      g.getVertices().forEach(n -> {
        PgxVertex old = community.get(n);
        PgxVertex newV = communityAux.get(n);
        if (old != newV) {
          community.set(n, newV);
          communityDegree.reduceAdd(old, (-1 * n.getOutDegree()));
          communityDegree.reduceAdd(newV, n.getOutDegree());
          stable.set(false);
        }
      });

      iteration++;
    }

    Scalar<Long> id = Scalar.create(0L);
    g.getVertices().forSequential(x -> {
      if (community.get(x) == x) {
        communityId.set(x, id.get());
        id.increment();
      }
    });

    g.getVertices().forSequential(x -> {
      PgxVertex n = community.get(x);
      communityId.set(x, communityId.get(n));
    });

    return id.get();
  }
}
```
