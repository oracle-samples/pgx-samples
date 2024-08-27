# Closeness Centrality (Unit Length)

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k4a_closeness_centrality_unit_length
- **Time Complexity:** O(V * E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#closenessCentralityUnitLength(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#closenessCentralityUnitLength-oracle.pgx.api.PgxGraph-)
  - [Analyst#closenessCentralityUnitLength(PgxGraph graph, VertexProperty<ID,java.lang.Double> cc)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#closenessCentralityUnitLength-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

The Closeness Centrality of a node V is the reciprocal of the sum of all the distances from the possible shortest paths starting from V. Thus the higher the centrality value of V, the closer it is to all the other vertices in the graph.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `cc` | vertexProp<double> | node property holding the closeness centrality value for each node. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | bool | returns true if the graph is connected, false otherwise. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;

@GraphAlgorithm
public class ClosenessCentrality {
  public boolean closenessCentrality(PgxGraph g, @Out VertexProperty<Double> cc) {
    Scalar<Boolean> connected = Scalar.create(true);

    g.getVertices().forEach(s -> {
      Scalar<Integer> foundNodes = Scalar.create(0);
      Scalar<Integer> levelSum = Scalar.create(0);

      inBFS(g, s).direction(Traversal.Direction.IN_OUT_EDGES).forward(v -> {
        foundNodes.increment();
        levelSum.reduceAdd(currentLevel());
      });

      if (foundNodes.get() != g.getNumVertices() || levelSum.get() == 0) {
        connected.reduceAnd(false);
      } else {
        cc.set(s, 1.0 / levelSum.get());
      }
    });

    if (connected.get()) {
      return true;
    } else {
      cc.setAll(0.0);

      return false;
    }
  }
}
```
