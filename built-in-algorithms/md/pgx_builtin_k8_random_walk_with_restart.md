# Random Walk with Restart

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k8_random_walk_with_restart
- **Time Complexity:** O(L) with L = length of the random walk
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#randomWalkWithRestart(PgxGraph graph, ID source, int length, java.math.BigDecimal reset_prob, PgxMap<PgxVertex<ID>,java.lang.Integer> visitCount)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#randomWalkWithRestart-oracle.pgx.api.PgxGraph-ID-int-java.math.BigDecimal-oracle.pgx.api.PgxMap-)

This algorithm performs a random walk over the graph. The walk will start at the given source vertex and will randomly visit neighboring vertices in the graph, with a probability equal to the value of reset_probability of going back to the starting point. The random walk will also go back to the starting point every time it reaches a vertex with no outgoing edges. The algorithm will stop once it reaches the specified walk length.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `source` | node | starting point of the random walk. |
| `length` | int | length (number of steps) of the random walk. |
| `reset_prob` | double | probability value for resetting the random walk. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `visit_count` | map<node, int> | map holding the number of visits during the random walk for each vertex in the graph. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Random.uniform;

@GraphAlgorithm
public class RandomWalkWithRestart {
  public void randomWalkWithRestart(PgxGraph g, PgxVertex source, int length, double resetProb,
      @Out PgxMap<PgxVertex, Integer> visitCount) {
    if (length <= 0) {
      return;
    }

    PgxVertex n = source;
    int current = 0;

    while (current < length) {
      double beta = uniform();

      if (beta < resetProb || n.getDegree() == 0) {
        n = source;
      } else {
        n = n.getRandomOutNeighbor();
      }
      visitCount.increment(n);
      current++;
    }
  }
}
```
