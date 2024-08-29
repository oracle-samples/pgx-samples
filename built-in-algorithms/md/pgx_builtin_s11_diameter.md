# Diameter / Radius

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s11_diameter
- **Time Complexity:** O(V * E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#radius(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#radius_oracle_pgx_api_PgxGraph_)
  - [Analyst#radius(PgxGraph graph, Scalar<java.lang.Integer> radius, VertexProperty<ID,java.lang.Integer> eccentricity](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#radius_oracle_pgx_api_PgxGraph_oracle_pgx_api_Scalar_oracle_pgx_api_VertexProperty_)

The diameter of a graph is the maximal value of eccentricity of all the vertices in the graph, while the radius is the minimum graph eccentricity. The eccentricity of a vertex is the maximum distance via shortest paths to any other vertex in the graph. This algorithm will compute the eccentricity of all the vertices and will also return the diameter or radius value depending on the request. The algorithm will return an INF eccentricity and diameter/radius, for graphs with more than one strongly connected component.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `diameterOn` | boolean | boolean flag to determine whether the algorithm will return the diameter or radius of the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `eccentricity` | vertexProp<int> | vertex property holding the eccentricity value for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | int | value of the diameter or radius, depending the chosen option. |

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
import static java.lang.Integer.MAX_VALUE;

@GraphAlgorithm
public class Diameter {
  public int diameter(PgxGraph g, boolean diameterOn, @Out VertexProperty<Integer> eccentricity) {
    eccentricity.setAll(0);
    long n = g.getNumVertices();
    Scalar<Integer> diameter = Scalar.create(0);
    Scalar<Integer> radius = Scalar.create(MAX_VALUE);
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
      eccentricity.setAll(MAX_VALUE);
      return MAX_VALUE;
    }

    if (diameterOn) {
      return diameter.get();
    } else {
      return radius.get();
    }
  }
}
```
