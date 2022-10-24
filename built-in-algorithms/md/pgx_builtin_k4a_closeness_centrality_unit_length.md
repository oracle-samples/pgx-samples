# Closeness Centrality (Unit Length)

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k4a_closeness_centrality_unit_length
- **Time Complexity:** O(V * E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#closenessCentralityUnitLength(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#closenessCentralityUnitLength-oracle.pgx.api.PgxGraph-)
  - [Analyst#closenessCentralityUnitLength(PgxGraph graph, VertexProperty<ID,java.lang.Double> cc)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#closenessCentralityUnitLength-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

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
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.Direction.IN_EDGES;
import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;
import static oracle.pgx.algorithm.Traversal.inDFS;

@GraphAlgorithm
public class ClosenessCentralityUnitLength {
  public boolean closenessCentralityUnitLength(PgxGraph g, @Out VertexProperty<Double> closenessCentrality) {
    if (g.getNumVertices() == 0) {
      return true;
    } else if (!isStronglyConnected(g)) {
      return false;
    }

    g.getVertices().forEach(n -> {
      Scalar<Long> levelSum = Scalar.create(0L);
      inBFS(g, n).forward(v ->
          levelSum.reduceAdd((long) currentLevel())
      );
      closenessCentrality.set(n, 1.0 / levelSum.get());
    });

    return true;
  }

  private boolean isStronglyConnected(PgxGraph g) {
    VertexProperty<Boolean> checked = VertexProperty.create();
    //Pick random node and execute simplified version of Kosaraju's algorithm
    PgxVertex t = g.getRandomVertex();
    checked.setAll(false);
    inDFS(g, t).forward(n ->
        checked.set(n, true)
    );
    if (g.getVertices().anyMatch(v -> !checked.get(v))) {
      return false;
    }
    checked.setAll(false);
    inDFS(g, t).direction(IN_EDGES).forward(n ->
        checked.set(n, true)
    );
    return g.getVertices().allMatch(checked::get);
  }
}
```
