# Periphery / Center

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s12_periphery
- **Time Complexity:** O(V * E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#periphery(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#periphery_oracle_pgx_api_PgxGraph_)
  - [Analyst#periphery(PgxGraph graph, VertexSet<ID> periphery)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#periphery_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexSet_)

The periphery of a graph is the set of vertices that have an eccentricity value equal to the diameter of the graph. Similarly, the center is comprised by the set of vertices with eccentricity equal to the radius of the graph. The diameter of a graph is the maximal value of eccentricity of all the vertices in the graph, while the radius is the minimum graph eccentricity. The eccentricity of a vertex is the maximum distance via shortest paths to any other vertex in the graph. This algorithm will return the set of vertices from the periphery or the center of the graph, depending on the request. The algorithm will return a set with all the vertices for graphs with more than one strongly connected component.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `peripheryOn` | boolean | boolean flag to determine whether the algorithm will return the periphery or center of the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `periphery` | nodeSet | vertex set holding the vertices from the periphery or center of the graph. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;
import static java.lang.Integer.MAX_VALUE;

@GraphAlgorithm
public class Periphery {
  public void periphery(PgxGraph g, boolean peripheryOn, @Out VertexSet periphery) {
    VertexProperty<Integer> eccentricity = VertexProperty.create();

    eccentricity.setAll(0);
    Scalar<Integer> diameter = Scalar.create(0);
    Scalar<Integer> radius = Scalar.create(MAX_VALUE);
    long n = g.getNumVertices();
    Scalar<Boolean> disconnected = Scalar.create(false);

    g.getVertices().filter(s -> !disconnected.get()).forEach(s -> {
      Scalar<Integer> visited = Scalar.create(1);
      Scalar<Integer> maxLevel = Scalar.create(0);

      inBFS(g, s).filter(v -> v != s).forward(v -> {
        maxLevel.reduceMax(currentLevel());
        visited.increment();
      });

      disconnected.set(visited.get() < n);
      diameter.reduceMax(maxLevel.get());
      radius.reduceMin(maxLevel.get());
      eccentricity.set(s, maxLevel.get());
    });

    if (disconnected.get()) {
      g.getVertices().forEach(periphery::add);
    } else {
      int chosen = peripheryOn ? diameter.get() : radius.get();
      g.getVertices().forEach(w -> {
        if (eccentricity.get(w) == chosen) {
          periphery.add(w);
        }
      });
    }
  }
}
```
