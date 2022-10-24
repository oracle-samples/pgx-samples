# Classic SALSA

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_r1b_salsa
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#salsa(BipartiteGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#salsa-oracle.pgx.api.BipartiteGraph-)
  - [Analyst#salsa(BipartiteGraph graph, double maxDiff, int maxIter)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#salsa-oracle.pgx.api.BipartiteGraph-double-int-)
  - [Analyst#salsa(BipartiteGraph graph, double maxDiff, int maxIter, VertexProperty<ID,java.lang.Double> salsaRank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#salsa-oracle.pgx.api.BipartiteGraph-double-int-oracle.pgx.api.VertexProperty-)
  - [Analyst#salsa(BipartiteGraph graph, VertexProperty<ID,java.lang.Double> salsaRank)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#salsa-oracle.pgx.api.BipartiteGraph-oracle.pgx.api.VertexProperty-)

The idea of hubs and authorites comes from the web pages: a hub is regarded as a page that is not authoritative in a specific matter, but it has instead links to authority pages, which are regarded as meaningful sources for a particular topic by many hubs. Thus a good hub will point to many authorities, while a good authority will be pointed by many hubs. SALSA is an algorithm that computes authorities and hubs ranking scores for the vertices using the network created by the edges of the [bipartite](prog-guides/mutation-subgraph/subgraph.html#create-a-bipartite-subgraph-based-on-a-vertex-list) graph and assigning weights to the contributions of their 2nd-degree neighbors. This way of computing the scores creates the independence between the authority and hub scores, which are assigned to the vertices depending on the side of the graph they belong (left:hub / right:aut).


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `is_left` | vertexProp<bool> | boolean vertex property stating the side of the vertices in the [bipartite](prog-guides/mutation-subgraph/subgraph.html#create-a-bipartite-subgraph-based-on-a-vertex-list) graph (left for hubs, right for auths). |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `max_iter` | int | maximum number of iterations that will be performed. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `rank` | vertexProp<double> | vertex property holding the normalized authority/hub ranking score for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.abs;

@GraphAlgorithm
public class Salsa {
  public void salsa(PgxGraph g, VertexProperty<Boolean> isLeft, double tol, int maxIter,
      @Out VertexProperty<Double> rank) {
    long numVertices = g.getNumVertices();

    if (numVertices == 0) {
      return;
    }

    VertexProperty<Double> deg = VertexProperty.create();
    Scalar<Double> diff = Scalar.create();
    int cnt = 0;

    deg.setAll(v -> (double) (isLeft.get(v) ? v.getOutDegree() : v.getInDegree()));
    long numHubs = g.getVertices().filter(isLeft).size();
    long numAuths = numVertices - numHubs;

    rank.setAll(v -> 1.0 / (isLeft.get(v) ? numHubs : numAuths));

    do {
      diff.set(0.0);
      g.getVertices().filter(n -> n.getOutDegree() + n.getInDegree() > 0).forEach(n -> {
        Scalar<Double> val = Scalar.create(0.0);
        if (isLeft.get(n)) {
          n.getOutNeighbors()
              .forEach(v -> val.reduceAdd(v.getInNeighbors().sum(w -> rank.get(w) / (deg.get(v) * deg.get(w)))));
        } else {
          n.getInNeighbors()
              .forEach(v -> val.reduceAdd(v.getOutNeighbors().sum(w -> rank.get(w) / (deg.get(v) * deg.get(w)))));
        }
        diff.reduceAdd(abs(val.get() - rank.get(n)));
        rank.setDeferred(n, val.get());
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);
  }
}
```
