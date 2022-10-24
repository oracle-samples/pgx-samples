# Bipartite Check

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s10_bipartite_check
- **Time Complexity:** O(E) with E = number of edges
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#bipartiteCheck(PgxGraph graph, VertexProperty<ID,java.lang.Boolean> isLeft)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#bipartiteCheck-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

This algorithm checks whether the given directed graph is bipartite. It assumes that all the edges are going in the same direction since the method relies on BFS traversals of the graph. If the graph is bipartite the algorithm will return the side of each vertex in the graph with the is_left vertex property.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `is_left` | vertexProp<bool> | vertex property holding the side of each vertex in a bipartite graph (true for left, false for right). |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | bool | true if the graph is bipartite, false otherwise. |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
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
public class BipartiteCheck {
  public boolean bipartiteCheck(PgxGraph g, @Out VertexProperty<Boolean> isLeft) {
    VertexProperty<Boolean> visited = VertexProperty.create(false);
    isLeft.setAll(false);

    Scalar<Boolean> isBipartiteGraph = Scalar.create(true);

    // assumption: edges only go from left to right
    g.getVertices().filter(root_node -> !visited.get(root_node) && root_node.getOutDegree() > 0)
        .forSequential(root_node -> {
          isLeft.set(root_node, true);

          inBFS(g, root_node).forward(n -> {
            boolean levelIsLeft = currentLevel() % 2 == 0;
            visited.set(n, true);
            isLeft.set(n, levelIsLeft);

            if (levelIsLeft && n.getInDegree() > 0) {
              isBipartiteGraph.set(false);
            } else if (!levelIsLeft && n.getOutDegree() > 0) {
              isBipartiteGraph.set(false);
            }
          });
        });

    return isBipartiteGraph.get();
  }
}
```
