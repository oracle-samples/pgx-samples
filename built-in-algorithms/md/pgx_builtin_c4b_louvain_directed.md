# LouvainDirected

- **Category:** community detection
- **Algorithm ID:** pgx_builtin_c4b_louvain_directed
- **Time Complexity:** O(E * k) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(8 * V + E) with V = number of vertices
- **Javadoc:**
  - [Analyst#louvain(PgxGraph graph, EdgeProperty weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#louvain-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#louvain(PgxGraph graph, EdgeProperty weight, int maxIter)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#louvain-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-int-)
  - [Analyst#louvain(PgxGraph graph, EdgeProperty weight, int maxIter, int nbrPass, double tol, VertexProperty community)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#louvain-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-int-int-double-oracle.pgx.api.VertexProperty-)

Louvain is an algorithm for community detection in large graphs which uses the graph's modularity. Initially it assigns a different community to each node of the graph. It then iterates over the nodes and evaluates for each node the modularity gain obtained by removing the node from its community and placing it in the community of one of its neigbours. The node is placed in the community for which the modularity gain is maximum. This process is repeated for all nodes until no improvement is possible, i.e until no new assignement of a node to a different community can improve the graph's modularity.

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
public class LouvainDirected {
  public void louvainDirected(PgxGraph g, EdgeProperty<Double> weight, int maxIter, int nbrPass, double tol,
      @Out VertexProperty<Long> communityId) {

    PgxMap<Long, VertexSet> superNodes = PgxMap.create();
    PgxMap<Long, VertexSet> superNbrs = PgxMap.create();
    PgxMap<Long, Double> superEdges = PgxMap.create();
    PgxMap<Long, Long> superCommunityId = PgxMap.create();
    PgxMap<Long, Double> sumIn = PgxMap.create();
    PgxMap<Long, Double> sumTotalIn = PgxMap.create();
    PgxMap<Long, Double> sumTotalOut = PgxMap.create();
    PgxMap<Long, Double> edgeWeightSumIn = PgxMap.create();
    PgxMap<Long, Double> edgeWeightSumOut = PgxMap.create();
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

      sumIn.set(c, n.getOutEdges().filter(e -> e.destinationVertex() == n).sum(weight));
      sumTotalOut.set(c, n.getOutEdges().sum(weight));
      sumTotalIn.set(c, n.getInEdges().sum(weight));
      allEdgesWeight += sumTotalOut.get(c);
      c++;
    });

    long numberOfStepsEstimatedForCompletion = (g.getNumVertices() + maxIter + 1) * nbrPass;
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    double newMod = 0;
    double curMod = 0;
    int itrCounter = 0;
    int passCounter = 0;
    boolean changed;
    long numVertices = g.getNumVertices();
    long currComm;
    double kInIn = 0;
    double kInOut = 0;
    double kInOldIn = 0;
    double kInOldOut = 0;
    double maxKInIn = 0;
    double maxKInOut = 0;
    double maxGain = 0;
    double modularityGain = 0;
    double selfEdgeWeightVar;
    double edgeWeightSumOutVar;
    double edgeWeightSumInVar;
    long superVerSuperCommunityId;
    VertexSet sNbrs;
    long targetComm;
    VertexSet nodes;

    newMod = modularity(g, superNodes, sumIn, sumTotalIn, sumTotalOut, allEdgesWeight);
    do {
      curMod = newMod;

      g.getVertices().forSequential(n -> {
        long nComm = communityId.get(n);
        n.getOutNeighbors().forSequential(nNbr -> {
          PgxEdge e = nNbr.edge();
          long nNbrComm = communityId.get(nNbr);
          double eWeight = weight.get(e);
          long idx = (numVertices * nComm) + nNbrComm;
          edgeWeightSumOut.reduceAdd(nComm, eWeight);
          edgeWeightSumIn.reduceAdd(nNbrComm, eWeight);
          if (!superEdges.containsKey(idx) && n != nNbr) {
            superNbrs.get(nComm).add(nNbr);
          }
          superEdges.reduceAdd(idx, eWeight);
          idx = (numVertices * nNbrComm) + nComm;
          if (!superEdges.containsKey(idx) && n != nNbr) {
            superNbrs.get(nNbrComm).add(n);
          }
          superEdges.reduceAdd(idx, 0.0);

          if (nComm == nNbrComm) {
            selfEdgeWeight.reduceAdd(nComm, eWeight);
          }
        });
      });

      do {
        changed = false;
        superNodes.keys().forSequential(superVer -> {
          currComm = superCommunityId.get(superVer);
          kInIn = 0;
          kInOut = 0;
          kInOldIn = 0;
          kInOldOut = 0;
          maxKInIn = 0;
          maxKInOut = 0;
          maxGain = 0;
          modularityGain = 0;
          selfEdgeWeightVar = selfEdgeWeight.get(superVer);
          edgeWeightSumOutVar = edgeWeightSumOut.get(superVer);
          edgeWeightSumInVar = edgeWeightSumIn.get(superVer);
          superVerSuperCommunityId = currComm;
          sNbrs = superNbrs.get(superVer).clone();
          sNbrs.forSequential(j -> {
            long jSuperNodeProp = superNodeProp.get(j);
            if (superCommunityId.get(jSuperNodeProp) == currComm) {
              kInOldOut += superEdges.get((numVertices * superVer) + jSuperNodeProp);
              kInOldIn += superEdges.get((numVertices * jSuperNodeProp) + superVer);
            }
          });

          sNbrs.forSequential(j -> {
            kInIn = 0;
            kInOut = 0;
            targetComm = superCommunityId.get(superNodeProp.get(j));
            if (currComm != targetComm) {
              sNbrs.forSequential(m -> {
                long mSuperNodeProp = superNodeProp.get(m);
                if (superCommunityId.get(mSuperNodeProp) == targetComm) {
                  kInOut += superEdges.get((numVertices * superVer) + mSuperNodeProp);
                  kInIn += superEdges.get((numVertices * mSuperNodeProp) + superVer);
                }
              });

              modularityGain = (kInIn + kInOut + selfEdgeWeightVar) / allEdgesWeight
                  - (sumTotalIn.get(targetComm) * edgeWeightSumOutVar
                  + sumTotalOut.get(targetComm) * edgeWeightSumInVar) / pow(allEdgesWeight, 2);
              modularityGain += (-kInOldIn - kInOldOut - selfEdgeWeightVar) / allEdgesWeight
                  + (sumTotalIn.get(currComm) * edgeWeightSumOutVar
                  + sumTotalOut.get(currComm) * edgeWeightSumInVar) / pow(allEdgesWeight, 2)
                  - 2 * edgeWeightSumInVar * edgeWeightSumOutVar / pow(allEdgesWeight, 2);

              if (modularityGain > maxGain) {
                maxGain = modularityGain;
                superVerSuperCommunityId = targetComm;
                maxKInIn = kInIn;
                maxKInOut = kInOut;
              }
            }
          });

          if (superVerSuperCommunityId != currComm) {
            changed = true;
            superCommunityId.set(superVer, superVerSuperCommunityId);
            sumIn.reduceAdd(currComm, -kInOldIn - kInOldOut - selfEdgeWeightVar);
            sumTotalIn.reduceAdd(currComm, -edgeWeightSumInVar);
            sumTotalOut.reduceAdd(currComm, -edgeWeightSumOutVar);

            sumIn.reduceAdd(superVerSuperCommunityId, maxKInIn + maxKInOut + selfEdgeWeightVar);
            sumTotalIn.reduceAdd(superVerSuperCommunityId, edgeWeightSumInVar);
            sumTotalOut.reduceAdd(superVerSuperCommunityId, edgeWeightSumOutVar);
            nodes = superNodes.get(superVer).clone();
            nodes.forSequential(n -> {
              communityId.set(n, superVerSuperCommunityId);
            });
          }
        });
        itrCounter++;
      } while (changed && itrCounter < maxIter);

      superNbrs.clear();
      superNodes.clear();
      superCommunityId.clear();
      edgeWeightSumIn.clear();
      edgeWeightSumOut.clear();
      selfEdgeWeight.clear();
      superEdges.clear();

      g.getVertices().forSequential(n -> {
        long nComm = communityId.get(n);
        superNodes.get(nComm).add(n);
        superCommunityId.set(nComm, nComm);
        superNodeProp.set(n, nComm);
      });
      newMod = modularity(g, superNodes, sumIn, sumTotalIn, sumTotalOut, allEdgesWeight);
      passCounter++;
    } while (passCounter < nbrPass && (newMod - curMod > tol));
  }

  double modularity(PgxGraph g, PgxMap<Long, VertexSet> superNodes,
      PgxMap<Long, Double> sumIn, PgxMap<Long, Double> sumTotalIn, PgxMap<Long, Double> sumTotalOut,
      double allEdgesWeight) {

    double inEdgesWeight = 0;
    double totalEdgesWeight = 0;
    superNodes.keys().forEach(superVerCommId -> {
      inEdgesWeight += sumIn.get(superVerCommId);
      totalEdgesWeight += sumTotalIn.get(superVerCommId) * sumTotalOut.get(superVerCommId);
    });
    return inEdgesWeight / allEdgesWeight - totalEdgesWeight / pow(allEdgesWeight, 2);
  }

}
```
