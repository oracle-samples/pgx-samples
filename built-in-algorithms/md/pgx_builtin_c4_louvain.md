# Louvain

- **Category:** community detection
- **Algorithm ID:** pgx_builtin_c4_louvain
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(8 * V + E) with V = number of vertices
- **Javadoc:**
  - [Analyst#louvain(PgxGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#louvain_oracle_pgx_api_PgxGraph_oracle_pgx_api_EdgeProperty_)
  - [Analyst#louvain(PgxGraph graph, EdgeProperty<java.lang.Double> weight, int maxIter)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#louvain_oracle_pgx_api_PgxGraph_oracle_pgx_api_EdgeProperty_int_)
  - [Analyst#louvain(PgxGraph graph, EdgeProperty<java.lang.Double> weight, int maxIter, int nbrPass, double tol, VertexProperty<ID,java.lang.Long> community)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#louvain_oracle_pgx_api_PgxGraph_oracle_pgx_api_EdgeProperty_int_int_double_oracle_pgx_api_VertexProperty_)

Louvain is an algorithm for community detection in large graphs which uses the graph's modularity. Initially it assigns a different community to each node of the graph. It then iterates over the nodes and evaluates for each node the modularity gain obtained by removing the node from its community and placing it in the community of one of its neighbors. The node is placed in the community for which the modularity gain is maximum. This process is repeated for all nodes until no improvement is possible, i.e until no new assignment of a node to a different community can improve the graph's modularity.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | |
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
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
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
import oracle.pgx.algorithm.ControlFlow;

import static java.lang.Math.pow;


@GraphAlgorithm
public class Louvain {
  public void louvain(PgxGraph g, EdgeProperty<Double> weight, int maxIter, int nbrPass, double tol,
      @Out VertexProperty<Long> communityId) {

    PgxMap<Long, VertexSet> superNodes = PgxMap.create();
    PgxMap<Long, VertexSet> superNbrs = PgxMap.create();
    PgxMap<Long, Double> superEdges = PgxMap.create();
    PgxMap<Long, Long> superCommunityId = PgxMap.create();
    PgxMap<Long, Double> sumIn = PgxMap.create();
    PgxMap<Long, Double> sumTotal = PgxMap.create();
    PgxMap<Long, Double> edgeWeightSum = PgxMap.create();
    PgxMap<Long, Double> selfEdgeWeight = PgxMap.create();
    VertexProperty<Long> superNodeProp = VertexProperty.create();
    double allEdgesWeight = 0;
    //initialize communities
    long c = 0;
    g.getVertices().forSequential(n -> {
      communityId.set(n, c);
      superNodes.get(c).add(n);
      superCommunityId.set(c, c);
      superNodeProp.set(n, c);

      sumIn.set(c, 2 * n.getOutEdges().filter(e -> e.destinationVertex() == n).sum(weight));

      sumTotal.set(c, sumIn.get(c) + n.getOutEdges().filter(e -> e.destinationVertex() != n).sum(weight));
      allEdgesWeight += sumTotal.get(c);
      c++;
    });

    long numberOfStepsEstimatedForCompletion = (g.getNumVertices() + maxIter + 1) * nbrPass;
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    double newMod = 0;
    double curMod = 0;
    int itrCounter = 0;
    int passCounter = 0;
    boolean changed;
    double d = 0;
    long numVertices = g.getNumVertices();

    long currComm;
    double kIn = 0;
    double kInOld = 0;
    double maxKIn = 0;
    double maxGain = 0;
    double modularityGain = 0;
    VertexSet sNbrs;
    long targetComm;
    VertexSet nodes;
    newMod = modularity(g, superNodes, sumIn, sumTotal, allEdgesWeight);
    do {
      curMod = newMod;

      // maps each superNodeProp with its neighbors with the aggregate total weight of the edges
      g.getVertices().forEach(n -> {
        n.getOutNeighbors().forSequential(nNbr -> {
          PgxEdge e = nNbr.edge();
          long idx = (numVertices * communityId.get(n)) + communityId.get(nNbr);
          edgeWeightSum.reduceAdd(communityId.get(n), weight.get(e));
          if (!superEdges.containsKey(idx) && n != nNbr) {
            superNbrs.get(communityId.get(n)).add(nNbr);
          }

          superEdges.reduceAdd(idx, weight.get(e));
          if (communityId.get(n) == communityId.get(nNbr)) {
            selfEdgeWeight.reduceAdd(communityId.get(n), weight.get(e));
          }

          if (n == nNbr) {
            superEdges.reduceAdd(idx, weight.get(e));
            selfEdgeWeight.reduceAdd(communityId.get(n), weight.get(e));
            edgeWeightSum.reduceAdd(communityId.get(n), weight.get(e));
          }
        });
      });

      do {
        changed = false;
        superNodes.keys().forSequential(superVer -> {
          currComm = superCommunityId.get(superVer);
          kIn = 0;
          kInOld = 0;
          maxKIn = 0;
          maxGain = 0;
          modularityGain = 0;
          sNbrs = superNbrs.get(superVer).clone();
          sNbrs.forSequential(j -> {
            if (superCommunityId.get(superNodeProp.get(j)) == currComm) {
              kInOld += superEdges.get((numVertices * superVer) + superNodeProp.get(j));
            }
          });

          sNbrs.forSequential(j -> {
            kIn = 0;
            targetComm = superCommunityId.get(superNodeProp.get(j));
            if (currComm != targetComm) {
              sNbrs.forSequential(m -> {
                if (superCommunityId.get(superNodeProp.get(m)) == targetComm) {
                  kIn += superEdges.get((numVertices * superVer) + superNodeProp.get(m));
                }
              });

              modularityGain = (2 * kIn + selfEdgeWeight.get(superVer)) / allEdgesWeight
                  - sumTotal.get(targetComm) * edgeWeightSum.get(superVer) * 2 / pow(allEdgesWeight, 2)
                  - (2 * kInOld + selfEdgeWeight.get(superVer)) / allEdgesWeight
                  + sumTotal.get(currComm) * edgeWeightSum.get(superVer) * 2 / pow(allEdgesWeight, 2)
                  - 2 * pow(edgeWeightSum.get(superVer) / allEdgesWeight, 2);
              if (modularityGain > maxGain) {
                maxGain = modularityGain;
                superCommunityId.set(superVer, superCommunityId.get(superNodeProp.get(j)));
                maxKIn = kIn;
              }
            }
          });

          if (superCommunityId.get(superVer) != currComm) {
            changed = true;
            sumIn.reduceAdd(currComm, -2 * kInOld - selfEdgeWeight.get(superVer));
            sumTotal.reduceAdd(currComm, -edgeWeightSum.get(superVer) + kInOld);
            sumIn.reduceAdd(superCommunityId.get(superVer), 2 * maxKIn + selfEdgeWeight.get(superVer));
            sumTotal.reduceAdd(superCommunityId.get(superVer), edgeWeightSum.get(superVer));
            nodes = superNodes.get(superVer).clone();
            nodes.forEach(n -> {
              communityId.set(n, superCommunityId.get(superVer));
            });
          }
        });
        itrCounter++;
      } while (changed && itrCounter < maxIter);

      superNbrs.clear();
      superNodes.clear();
      superCommunityId.clear();
      edgeWeightSum.clear();
      selfEdgeWeight.clear();
      superEdges.clear();

      g.getVertices().forSequential(n -> {
        superNodes.get(communityId.get(n)).add(n);
        superCommunityId.set(communityId.get(n), communityId.get(n));
        superNodeProp.set(n, communityId.get(n));
      });
      newMod = modularity(g, superNodes, sumIn, sumTotal, allEdgesWeight);
      passCounter++;
    } while (passCounter < nbrPass && (newMod - curMod > tol));
  }

  double modularity(PgxGraph g, PgxMap<Long, VertexSet> superNodes,
      PgxMap<Long, Double> sumIn, PgxMap<Long, Double> sumTotal, double allEdgesWeight) {

    double inEdgesWeight = 0;
    double totalEdgesWeight = 0;
    superNodes.keys().forEach(superVerCommId -> {
      inEdgesWeight += sumIn.get(superVerCommId);
      totalEdgesWeight += pow(sumTotal.get(superVerCommId), 2);
    });
    return inEdgesWeight / allEdgesWeight - totalEdgesWeight / pow(allEdgesWeight, 2);
  }

}
```
