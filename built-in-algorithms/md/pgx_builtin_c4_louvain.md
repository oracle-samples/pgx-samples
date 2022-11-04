# Louvain

- **Category:** community detection
- **Algorithm ID:** pgx_builtin_c4_louvain
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(8 * V + E) with V = number of vertices
- **Javadoc:** 
  - [Analyst#louvain(PgxGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#louvain-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#louvain(PgxGraph graph, EdgeProperty<java.lang.Double> weight, int maxIter)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#louvain-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-int-)
  - [Analyst#louvain(PgxGraph graph, EdgeProperty<java.lang.Double> weight, int maxIter, int nbrPass, double tol, VertexProperty<ID,java.lang.Long> community)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#louvain-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-int-int-double-oracle.pgx.api.VertexProperty-)

Louvain is an algorithm for community detection in large graphs which uses the graph's modularity. Initially it assigns a different community to each node of the graph. It then iterates over the nodes and evaluates for each node the modularity gain obtained by removing the node from its community and placing it in the community of one of its neighbours. The node is placed in the community for which the modularity gain is maximum. This process is repeated for all nodes until no improvement is possible, i.e until no new assignment of a node to a different community can improve the graph's modularity.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `weight` | edgeProp<double> | weights of the edges of the graph. |
| `max_iter` | int | maximum number of iterations that will be performed during each pass. |
| `nbr_pass` | int | number of passes that will be performed. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the graph's total modularity gain becomes smaller than this value. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `communityId` | vertexProp<long> | the community ID assigned to each node. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.pow;


@GraphAlgorithm
public class Louvain {
  public void louvain(PgxGraph g, EdgeProperty<Double> weight, int maxIter, int nbrPass, double tol,
      @Out VertexProperty<Long> communityId) {
    long c = 0;
    int iter = 0;
    int pass = 0;
    long numVertices = g.getNumVertices();
    PgxMap<Long, Double> sumIn = PgxMap.create();
    PgxMap<Long, Double> sumTotal = PgxMap.create();
    VertexProperty<Double> edgeWeight;

    // initialize communities
    g.getVertices().forSequential(n -> {
      communityId.set(n, c);

      //sum of the weights of the edges incident to node n
      edgeWeight.set(n, n.getOutEdges().sum(weight));

      //sum of the weights of the edges inside n's community
      sumIn.set(c, n.getOutEdges().filter(e -> e.destinationVertex() == n).sum(weight));

      //sum of the weights of the edges incidsent to nodes in n's community
      sumTotal.set(c, edgeWeight.get(n));

      c++;
    });

    double twoM = 2 * g.getEdges().sum(weight);
    double newMod = 0;
    double curMod = 0;

    if (nbrPass > 1) {
      newMod = modularity(g, weight, edgeWeight, communityId, twoM);
    }

    boolean changed;

    do {
      curMod = newMod;

      //aggregate the graph: nodes of the same community constitute a super node
      PgxMap<Long, VertexSet> svertices = PgxMap.create();
      PgxMap<Long, VertexSet> superNbrs = PgxMap.create();
      PgxMap<Long, Double> allSuperEdges = PgxMap.create();
      VertexProperty<Long> svertex = VertexProperty.create();
      PgxMap<Long, Long> svertexCommunity = PgxMap.create();
      PgxMap<Long, Double> edgeWeightSum = PgxMap.create();

      g.getVertices().forSequential(n -> {
        svertices.get(communityId.get(n)).add(n);
        svertexCommunity.set(communityId.get(n), communityId.get(n));
        svertex.set(n, communityId.get(n));
        n.getOutNeighbors().forSequential(nNbr -> {
          PgxEdge e = nNbr.edge();
          long idx = (numVertices * communityId.get(n)) + communityId.get(nNbr);
          if (!allSuperEdges.containsKey(idx)) {
            superNbrs.get(communityId.get(n)).add(nNbr);
          }
          allSuperEdges.reduceAdd(idx, weight.get(e));
          edgeWeightSum.reduceAdd(communityId.get(n), weight.get(e));
        });
      });

      do {
        changed = false;
        svertices.keys().forSequential(n -> {
          c = svertexCommunity.get(n);
          double kIn = 0;
          double gain = 0;
          VertexSet snbrs = superNbrs.get(n).clone();
          double maxGain = 0;
          double modularityGain = 0;
          snbrs.forSequential(o -> {
            Long comm = svertexCommunity.get(svertex.get(o));
            snbrs.forSequential(m -> {
              if (svertexCommunity.get(svertex.get(m)) == comm) {
                kIn += allSuperEdges.get((numVertices * n) + svertex.get(m));
              }
            });

            modularityGain = (sumIn.get(comm) + kIn) / twoM - pow((sumTotal.get(comm) + edgeWeightSum.get(n)) / twoM,
              2) - (sumIn.get(comm) / twoM - pow(sumTotal.get(comm) / twoM, 2) - pow(edgeWeightSum.get(n) / twoM, 2));
            if (modularityGain > maxGain) {
              maxGain = modularityGain;
              svertexCommunity.set(n, svertexCommunity.get(svertex.get(o)));
            }
          });

          if (svertexCommunity.get(n) != c) {
            double kInOld = 0;
            double kInNew = 0;
            changed = true;
            snbrs.forSequential(m -> {
              if (svertexCommunity.get(svertex.get(m)) == c) {
                kInOld += allSuperEdges.get((numVertices * n) + svertex.get(m));
              }
            });
            sumIn.set(c, sumIn.get(c) - kInOld);
            sumTotal.set(c, sumTotal.get(c) - (edgeWeightSum.get(n) - kInOld));
            snbrs.forSequential(m -> {
              if (svertexCommunity.get(svertex.get(m)) == svertexCommunity.get(n)) {
                kInNew += allSuperEdges.get((numVertices * n) + svertex.get(m));
              }
            });
            sumIn.set(svertexCommunity.get(n), sumIn.get(svertexCommunity.get(n)) + kInNew);
            sumTotal.set(svertexCommunity.get(n), sumTotal.get(svertexCommunity.get(n))
                + (edgeWeightSum.get(n) - kInNew));
          }
        });
        iter++;
      } while (changed && iter < maxIter);
      g.getVertices().forEach(n -> {
        communityId.set(n, svertexCommunity.get(svertex.get(n)));
      });
      pass++;
      if (nbrPass > 1) {
        newMod = modularity(g, weight, edgeWeight, communityId, twoM);
      }
    } while (pass < nbrPass && (newMod - curMod > tol));
  }

  double modularity(PgxGraph g, EdgeProperty<Double> weight, VertexProperty<Double> edgeWeight,
      VertexProperty<Long> communityId, double twoM) {
    double q = 0;
    g.getVertices().forEach(i -> {
      g.getVertices().forEach(j -> {
        if (communityId.get(i) == communityId.get(j)) {
          double aij = j.getOutEdges().filter(e -> e.destinationVertex() == i).sum(weight);
          q += aij - (edgeWeight.get(i) * edgeWeight.get(j) / twoM);
        }
      });
    });
    return q / twoM;
  }
}```
