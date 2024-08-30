# Hyperlink-Induced Topic Search (HITS)

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k5_hits
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#hits(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#hits_oracle_pgx_api_PgxGraph_)
  - [Analyst#hits(PgxGraph graph, int max)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#hits_oracle_pgx_api_PgxGraph_int_)
  - [Analyst#hits(PgxGraph graph, int max, VertexProperty<ID,java.lang.Double> auth, VertexProperty<ID,java.lang.Double> hubs)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#hits_oracle_pgx_api_PgxGraph_int_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#hits(PgxGraph graph, VertexProperty<ID,java.lang.Double> auth, VertexProperty<ID,java.lang.Double> hubs)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#hits_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_)

HITS is an algorithm that computes two ranking scores (authority and hub)for each vertex in the graph. The idea of hubs and authorities comes from the web pages: a hub is regarded as a page that is not authoritative in a specific topic, but it has instead links to authority pages, which are regarded as meaningful sources for a particular topic by many hubs. Thus a good hub will point to many authorities, while a good authority will be pointed by many hubs. The authority score of a vertex V is computed by adding all the hub scores of its incoming neighbors (i.e. vertices with edges pointing to V). The hub score is computed in a similar way, using the authority scores instead.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `max_iter` | int | number of iterations that will be performed. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `auth` | vertexProp<double> | vertex property holding the authority score for each vertex. |
| `hub` | vertexProp<double> | vertex property holding the hub score for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.sqrt;

@GraphAlgorithm
public class Hits {
  public void hits(PgxGraph g, int maxIter, @Out VertexProperty<Double> auth, @Out VertexProperty<Double> hub) {
    auth.setAll(1.0);
    hub.setAll(1.0);

    int k = 0;
    while (k < maxIter) {
      Scalar<Double> norm = Scalar.create();

      // phase 1. update auth from hub
      norm.set(0d);

      g.getVertices().forEach(p -> {
        double v = p.getInNeighbors().sum(hub);
        norm.reduceAdd(v * v);
        auth.set(p, v);
      });
      norm.set(sqrt(norm.get()));
      auth.setAll(v -> auth.get(v) / norm.get());

      // phase 2. hub from auth
      norm.set(0d);

      g.getVertices().forEach(p -> {
        double v = p.getOutNeighbors().sum(auth);
        norm.reduceAdd(v * v);
        hub.set(p, v);
      });
      norm.set(sqrt(norm.get()));
      hub.setAll(v -> hub.get(v) / norm.get());

      k++;
    }
  }
}
```
