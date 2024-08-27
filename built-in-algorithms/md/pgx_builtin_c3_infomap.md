# Infomap

- **Category:** community detection
- **Algorithm ID:** pgx_builtin_c3_infomap
- **Time Complexity:** O((k ^ 2) * E) with E = number of edges, k <= maximum number of iterations
- **Space Requirement:** O(10 * V + 2 * E) with V = number of vertices, E = number of edges
- **Javadoc:** 
  - [Analyst#communitiesInfomap(PgxGraph graph, VertexProperty<ID,java.lang.Double> rank, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#communitiesInfomap-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-oracle.pgx.api.EdgeProperty-)
  - [Analyst#communitiesInfomap(PgxGraph graph, VertexProperty<ID,java.lang.Double> rank, EdgeProperty<java.lang.Double> weight, double tau, double tol, int maxIter)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#communitiesInfomap-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-oracle.pgx.api.EdgeProperty-double-double-int-)
  - [Analyst#communitiesInfomap(PgxGraph graph, VertexProperty<ID,java.lang.Double> rank, EdgeProperty<java.lang.Double> weight, double tau, double tol, int maxIter, VertexProperty<ID,java.lang.Long> module)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#communitiesInfomap-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-oracle.pgx.api.EdgeProperty-double-double-int-oracle.pgx.api.VertexProperty-)
  - [Analyst#communitiesInfomap(PgxGraph graph, VertexProperty<ID,java.lang.Double> rank, EdgeProperty<java.lang.Double> weight, VertexProperty<ID,java.lang.Long> module)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#communitiesInfomap-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-oracle.pgx.api.EdgeProperty-oracle.pgx.api.VertexProperty-)

Infomap is a robust algorithm designed to find community structures in a graph that requires some pre-processing steps. This implementation needs a reciprocated or an undirected graph, as well as the ranking score from the normalized weighted version of the Pagerank algorithm. It will assign a unique module (or community) label to each vertex in the graph based on their Pagerank score, edge weights and the labels of their neighbors. It is an iterative algorithm that updates the labels of the vertices in random order on each iteration using the previous factors, converging once there are no further changes in the vertex labels, or once the maximum number of iterations is reached. The algorithm is non-deterministic because of the random order for visiting and updating the vertex labels, thus the communities found might be different each time the algorithm is run.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the undirected graph. |
| `rank` | vertexProp<double> | vertex property holding the normalized (weighted) PageRank value for each vertex (a value between 0 and 1). |
| `weight` | edgeProp<double> | edge property holding the weight of each edge in the graph. |
| `tau` | double | damping factor. |
| `tol` | double | maximum tolerated error value. The algorithm will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `max_iter` | int | maximum number of iterations that will be performed. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `module` | vertexProp<long> | vertex property holding the label of the community assigned to each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | long | returns the total number of communities found. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.ControlFlow;
import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.ControlFlow;

import static java.lang.Math.log;
import static oracle.pgx.algorithm.ControlFlow.exit;

@GraphAlgorithm
public class Infomap {
  public long weightedInfomap(PgxGraph g, VertexProperty<Double> rank, EdgeProperty<Double> weight, double tau,
      double tol, int maxIter, @Out VertexProperty<Long> module) {
    VertexProperty<Double> exitPr = VertexProperty.create();
    EdgeProperty<Double> normWeight = EdgeProperty.create();

    VertexProperty<PgxVertex> order = VertexProperty.create();

    long numberOfStepsEstimatedForCompletion = 2 * g.getNumVertices()
        + (g.getNumVertices() / 2 + 3) * maxIter * maxIter;
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    g.getVertices().forEach(n -> {
      order.set(n, n);
    });

    //initialize modules (each vertex is a module)
    PgxMap<Long, Long> emptyMod = PgxMap.create();
    PgxMap<Long, Long> modSize = PgxMap.create();
    PgxMap<Long, Double> modRank = PgxMap.create();
    PgxMap<Long, Double> qi = PgxMap.create();

    double sigmaQi = 0;
    double codeLength = 0;
    double codeDiff = 0;

    PgxMap<Integer, Double> initMap = PgxMap.create();
    initialization(g, tau, rank, weight, module, exitPr, normWeight, modSize, modRank, qi, initMap);
    sigmaQi = initMap.get(0);
    codeLength = initMap.get(1);

    //outer loop
    int outCnt = 0;
    boolean converged;
    long totalSum = 0;
    long totalPre = 0;
    do {

      double oldCodeLength = codeLength + codeDiff;
      converged = false;

      //fine-grain loop
      PgxMap<Long, Double> auxMap = PgxMap.create();
      fineGrainLoop(g, maxIter, tau, sigmaQi, codeDiff, codeLength, rank, normWeight, module, exitPr, emptyMod, modSize,
          modRank, qi, auxMap, order);
      sigmaQi = auxMap.get(0L);
      codeDiff = auxMap.get(1L);

      //coarse-grain loop
      auxMap.clear();
      coarseGrainLoop(g, maxIter, tau, sigmaQi, codeDiff, codeLength, rank, normWeight, module, exitPr, emptyMod,
          modSize, modRank, qi, auxMap);
      sigmaQi = auxMap.get(0L);
      codeDiff = auxMap.get(1L);

      if (oldCodeLength - (codeLength + codeDiff) < tol) {
        converged = true;
      }

      outCnt++;
    } while (outCnt < maxIter && !converged);

    relabelingModules(g, module);

    return modSize.size();
  }

  long getKey(PgxMap<Long, Long> emptyMod) {
    emptyMod.keys().forSequential(ControlFlow::exit);

    return -1;
  }

  double plogp(double p) {
    if (p > 0) {
      return p * log(p);
    } else {
      return 0.0;
    }
  }

  void relabelingModules(PgxGraph g, @Out VertexProperty<Long> module) {
    Scalar<Long> l = Scalar.create(-1L);
    Scalar<Long> k = Scalar.create(-1L);

    PgxMap<PgxVertex, Long> modules = PgxMap.create();

    g.getVertices().forSequential(nd -> {
      modules.set(nd, module.get(nd));
    });

    while (modules.size() > 0) {
      PgxVertex nd = modules.getKeyForMinValue();
      long ndModule = module.get(nd);
      if (k.get() < ndModule) {
        l.increment();
      }
      if (ndModule >= l.get()) {
        k.set(ndModule);
        module.set(nd, l.get());
      }

      modules.remove(nd);
    }
  }

  void compareNbrModules(PgxGraph g, long oldModule, double tau, double sigmaQi, double locRank, double locExitPr,
      double ndTpWeight, PgxMap<Long, Double> qi, PgxMap<Long, Double> modRank, PgxMap<Long, Long> modSize,
      @Out PgxMap<Long, Double> outFlowToMod, @Out PgxMap<Long, Double> inFlowFromMod,
      @Out PgxMap<Integer, Double> mapOfBests, @Out PgxMap<Long, Long> emptyMod) {

    double oldExitPr1 = qi.get(oldModule);
    double oldSumPr1 = modRank.get(oldModule);
    double oldModTpWeight = modSize.get(oldModule) * 1.0 / g.getNumVertices();

    double additionalTeleportOutFlow = tau * locRank * (oldModTpWeight - ndTpWeight);
    double additionalTeleportInFlow = tau * (oldSumPr1 - locRank) * ndTpWeight;

    outFlowToMod.keys().forSequential(newModule -> {
      if (newModule == oldModule) {
        outFlowToMod.reduceAdd(newModule, additionalTeleportOutFlow);
        inFlowFromMod.reduceAdd(newModule, additionalTeleportInFlow);
      } else {
        outFlowToMod.reduceAdd(newModule, tau * locRank * modSize.get(newModule) / g.getNumVertices());
        inFlowFromMod.reduceAdd(newModule, tau * modRank.get(newModule) * ndTpWeight);
      }
    });

    double outFlowToOldMod = additionalTeleportOutFlow;
    double inFlowFromOldMod = additionalTeleportInFlow;

    if (outFlowToMod.containsKey(oldModule)) {
      outFlowToOldMod = outFlowToMod.get(oldModule);
      inFlowFromOldMod = inFlowFromMod.get(oldModule);
    }

    if (modRank.get(oldModule) > locRank && emptyMod.size() > 0) {
      long emptyTag = getKey(emptyMod);
      outFlowToMod.set(emptyTag, 0d);
      inFlowFromMod.set(emptyTag, 0d);
    }

    double newExitPr1 = oldExitPr1 - locExitPr + outFlowToOldMod + inFlowFromOldMod;

    Scalar<Double> bestDiffCodeLen = Scalar.create(0d);

    Scalar<Double> currentDiffCodeLen = Scalar.create(0d);
    Scalar<Long> currentNewModule = Scalar.create(0L);
    Scalar<Double> currentRankOldModule = Scalar.create(0d);
    Scalar<Double> currentRankNewModule = Scalar.create(0d);
    Scalar<Double> currentExitPrOldModule = Scalar.create(0d);
    Scalar<Double> currentExitPrNewModule = Scalar.create(0d);
    Scalar<Double> currentNewSigmaQI = Scalar.create(0d);

    outFlowToMod.keys().forSequential(newModule -> {
      double outFlowToNewMod = outFlowToMod.get(newModule);
      double inFlowFromNewMod = inFlowFromMod.get(newModule);

      if (oldModule != newModule) {
        double oldExitPr2 = qi.get(newModule);
        double oldSumPr2 = modRank.get(newModule);

        currentNewModule.set(newModule);
        currentRankOldModule.set(oldSumPr1 - locRank);
        currentRankNewModule.set(oldSumPr2 + locRank);
        currentExitPrOldModule.set(newExitPr1);
        currentExitPrNewModule.set(oldExitPr2 + locExitPr - outFlowToNewMod - inFlowFromNewMod);
        currentNewSigmaQI.set(sigmaQi + newExitPr1 + currentExitPrNewModule.get() - oldExitPr1 - oldExitPr2);

        double plogpOld = plogp(sigmaQi);
        double plogpNew = plogp(currentNewSigmaQI.get());
        double plogpExitOld = plogp(oldExitPr1) + plogp(oldExitPr2);
        double plogpExitNew = plogp(currentExitPrOldModule.get()) + plogp(currentExitPrNewModule.get());
        double plogpStayOld = plogp(oldExitPr1 + oldSumPr1) + plogp(oldExitPr2 + oldSumPr2);
        double plogpStayNew = plogp(currentExitPrOldModule.get() + currentRankOldModule.get()) + plogp(
            currentExitPrNewModule.get() + currentRankNewModule.get());

        double deltaAllExitLogAllExit = plogpNew - plogpOld;
        double deltaExitLogExit = plogpExitNew - plogpExitOld;
        double deltaStayLogStay = plogpStayNew - plogpStayOld;

        currentDiffCodeLen.set(deltaAllExitLogAllExit - 2 * deltaExitLogExit + deltaStayLogStay);

        if (currentDiffCodeLen.get() < bestDiffCodeLen.get()) {
          bestDiffCodeLen.set(currentDiffCodeLen.get());
          mapOfBests.set(0, currentDiffCodeLen.get());
          mapOfBests.set(1, (double) currentNewModule.get());
          mapOfBests.set(2, currentRankOldModule.get());
          mapOfBests.set(3, currentRankNewModule.get());
          mapOfBests.set(4, currentExitPrOldModule.get());
          mapOfBests.set(5, currentExitPrNewModule.get());
          mapOfBests.set(6, currentNewSigmaQI.get());
        }
      }
    });
  }

  void initialization(PgxGraph g, double tau, VertexProperty<Double> rank, @Out EdgeProperty<Double> weight,
      @Out VertexProperty<Long> module, @Out VertexProperty<Double> exitPr, @Out EdgeProperty<Double> normWeight,
      @Out PgxMap<Long, Long> modSize, @Out PgxMap<Long, Double> modRank, @Out PgxMap<Long, Double> qi,
      @Out PgxMap<Integer, Double> initMap) {
    double aux1 = 0;
    Scalar<Double> aux2 = Scalar.create(0d);
    Scalar<Double> aux3 = Scalar.create(0d);
    Scalar<Double> aux4 = Scalar.create(0d);
    Scalar<Double> sigmaQI = Scalar.create(0d);
    Scalar<Long> l = Scalar.create(0L);

    //assuming equal tp weights (1/N):
    double initTpWeight = (g.getNumVertices() - 1) / (double) g.getNumVertices();

    g.getVertices().forSequential(n -> {
      long nRank = rank.get(n);
      module.set(n, l.get());
      double sumWeight = n.getOutEdges().sum(weight);

      n.getOutNeighbors().forSequential(nNbr -> {
        PgxEdge e = nNbr.edge();

        normWeight.set(e, (1 - tau) * nRank * weight.get(e) / sumWeight);
      });

      double tmpQI = (tau * initTpWeight * nRank) + ((1 - tau) * nRank);
      qi.set(l.get(), tmpQI);
      exitPr.set(n, tmpQI);
      modRank.set(l.get(), nRank);
      modSize.set(l.get(), 1L);

      sigmaQI.reduceAdd(tmpQI);
      aux2.reduceAdd(plogp(tmpQI));
      aux3.reduceAdd(plogp(tmpQI + nRank));
      aux4.reduceAdd(plogp(nRank));

      l.increment();
    });

    aux1 = plogp(sigmaQI.get());
    initMap.set(0, sigmaQI.get());
    initMap.set(1, (aux1 - 2 * aux2.get() + aux3.get() - aux4.get()) / log(2.0));
  }

  void fineGrainLoop(PgxGraph g, int maxIter, double tau, double sigmaQI, double codeDiff, double codeLength,
      VertexProperty<Double> rank, EdgeProperty<Double> normWeight, @Out VertexProperty<Long> module,
      @Out VertexProperty<Double> exitPr, @Out PgxMap<Long, Long> emptyMod, @Out PgxMap<Long, Long> modSize,
      @Out PgxMap<Long, Double> modRank, @Out PgxMap<Long, Double> qi, @Out PgxMap<Long, Double> auxMap,
      @Out VertexProperty<PgxVertex> order) {

    Scalar<Long> moved = Scalar.create(0L);
    int cnt = 0;
    auxMap.set(0L, sigmaQI);
    auxMap.set(1L, codeDiff);

    Scalar<Long> nn = Scalar.create(g.getNumVertices());

    do {

      moved.set(0L);
      Scalar<Long> oldModule = Scalar.create(0L);

      Scalar<Long> l = Scalar.create(0L);

      do {
        PgxVertex p = g.getRandomVertex();
        PgxVertex q = g.getRandomVertex();
        PgxVertex tmp = order.get(p);
        order.set(p, order.get(q));
        order.set(q, tmp);
        l.increment();
      } while (l.get() < nn.get() / 2);

      g.getVertices().forSequential(n -> {
        PgxVertex nd = order.get(n);

        oldModule.set(module.get(nd));

        double localSigmaQI = auxMap.get(0L);
        PgxMap<Long, Double> outFlowToMod = PgxMap.create();
        PgxMap<Long, Double> inFlowFromMod = PgxMap.create();

        nd.getOutNeighbors().forSequential(ndNbr -> {
          PgxEdge e = ndNbr.edge();
          outFlowToMod.reduceAdd(module.get(ndNbr), normWeight.get(e));
        });

        nd.getInNeighbors().forSequential(ndNbr -> {
          PgxEdge e = ndNbr.edge();
          inFlowFromMod.reduceAdd(module.get(ndNbr), normWeight.get(e));
        });

        PgxMap<Integer, Double> mapOfBests = PgxMap.create();
        double locRank = rank.get(nd);
        double locExitPr = exitPr.get(nd);
        double ndTpWeight = 1.0 / g.getNumVertices();

        compareNbrModules(g, oldModule.get(), tau, localSigmaQI, locRank, locExitPr, ndTpWeight, qi, modRank, modSize,
            outFlowToMod, inFlowFromMod, mapOfBests, emptyMod);

        double bestDiffCodeLen = mapOfBests.get(0);
        long bestNewModule = (long) (double) mapOfBests.get(1);
        double bestRankOldModule = mapOfBests.get(2);
        double bestRankNewModule = mapOfBests.get(3);
        double bestExitPrOldModule = mapOfBests.get(4);
        double bestExitPrNewModule = mapOfBests.get(5);
        double bestNewSigmaQI = mapOfBests.get(6);

        if (bestDiffCodeLen < 0) {

          if (emptyMod.containsKey(bestNewModule)) {
            emptyMod.remove(bestNewModule);
          }

          module.set(nd, bestNewModule);
          modSize.increment(bestNewModule);
          qi.set(bestNewModule, bestExitPrNewModule);
          modRank.set(bestNewModule, bestRankNewModule);

          modSize.decrement(oldModule.get());
          qi.set(oldModule.get(), bestExitPrOldModule);
          modRank.set(oldModule.get(), bestRankOldModule);
          if (modSize.get(oldModule.get()) < 1) {
            modSize.remove(oldModule.get());
            modRank.remove(oldModule.get());
            qi.remove(oldModule.get());
            emptyMod.set(oldModule.get(), oldModule.get());
          }
          moved.increment();
          auxMap.set(0L, bestNewSigmaQI);
          auxMap.reduceAdd(1L, bestDiffCodeLen / log(2.0));
        }
      });
      cnt++;
    } while (cnt < maxIter && moved.get() > 0);
  }

  void coarseGrainLoop(PgxGraph g, int maxIter, double tau, double sigmaQI, double codeDiff, double codeLength,
      VertexProperty<Double> rank, EdgeProperty<Double> normWeight, @Out VertexProperty<Long> module,
      @Out VertexProperty<Double> exitPr, @Out PgxMap<Long, Long> emptyMod, @Out PgxMap<Long, Long> modSize,
      @Out PgxMap<Long, Double> modRank, @Out PgxMap<Long, Double> qi, @Out PgxMap<Long, Double> auxMap) {

    // creating super-vertices and super-edges
    PgxMap<Long, VertexSet> snodes = PgxMap.create();
    PgxMap<Long, VertexSet> superNbrs = PgxMap.create();
    PgxMap<Long, VertexSet> inSuperNbrs = PgxMap.create();
    PgxMap<Long, Double> allSuperEdges = PgxMap.create();

    VertexProperty<Long> snode = VertexProperty.create();
    PgxMap<Long, Long> snodeMod = PgxMap.create();
    PgxMap<Long, Double> snodeRank = PgxMap.create();
    PgxMap<Long, Double> snodeExitPr = PgxMap.create();

    Scalar<Integer> cntE = Scalar.create(1);
    g.getVertices().forSequential(n -> {
      long nModule = module.get(n);
      snodes.get(nModule).add(n);
      snodeMod.set(nModule, nModule);
      snodeRank.reduceAdd(nModule, rank.get(n));
      snodeExitPr.set(nModule, qi.get(nModule));
      snode.set(n, nModule);
      n.getOutNeighbors().forSequential(nNbr -> {
        long nNbrModule = module.get(nNbr);
        PgxEdge e = nNbr.edge();
        if (nModule != nNbrModule) {
          long idx = (g.getNumVertices() * nModule) + nNbrModule;

          if (!allSuperEdges.containsKey(idx)) {
            superNbrs.get(nModule).add(nNbr);
            inSuperNbrs.get(nNbrModule).add(n);
          }
          allSuperEdges.reduceAdd(idx, normWeight.get(e));
          cntE.increment();
        }
      });
    });

    Scalar<Long> moved = Scalar.create(0L);
    int coarseCnt = 0;

    auxMap.set(0L, sigmaQI);
    auxMap.set(1L, codeDiff);

    do {
      moved.set(0L);
      long outSum = 0;
      long preOut = 0;

      snodes.keys().forSequential(sndID -> {
        long oldModule = snodeMod.get(sndID);
        VertexSet tmp = snodes.get(sndID).clone();
        double localSigmaQI = auxMap.get(0L);

        PgxMap<Long, Double> superOutFlowToMod = PgxMap.create();
        PgxMap<Long, Double> superInFlowFromMod = PgxMap.create();

        VertexSet outSn = superNbrs.get(sndID).clone();
        VertexSet inSn = inSuperNbrs.get(sndID).clone();

        outSn.forSequential(sn -> superOutFlowToMod
            .reduceAdd(module.get(sn), allSuperEdges.get((g.getNumVertices() * sndID) + snode.get(sn))));

        inSn.forSequential(sn -> superInFlowFromMod
            .reduceAdd(module.get(sn), allSuperEdges.get((g.getNumVertices() * snode.get(sn)) + sndID)));

        PgxMap<Integer, Double> mapOfBests = PgxMap.create();
        double locRank = snodeRank.get(sndID);
        double locExitPr = snodeExitPr.get(sndID);
        double ndTpWeight = tmp.size() / (double) g.getNumVertices();

        compareNbrModules(g, oldModule, tau, localSigmaQI, locRank, locExitPr, ndTpWeight, qi, modRank, modSize,
            superOutFlowToMod, superInFlowFromMod, mapOfBests, emptyMod);

        double bestDiffCodeLen = mapOfBests.get(0);
        long bestNewModule = (long) (double) mapOfBests.get(1);
        double bestRankOldModule = mapOfBests.get(2);
        double bestRankNewModule = mapOfBests.get(3);
        double bestExitPrOldModule = mapOfBests.get(4);
        double bestExitPrNewModule = mapOfBests.get(5);
        double bestNewSigmaQI = mapOfBests.get(6);

        if (bestDiffCodeLen < 0) {

          if (emptyMod.containsKey(bestNewModule)) {
            emptyMod.remove(bestNewModule);
          }

          tmp.forSequential(n -> module.set(n, bestNewModule));

          moved.reduceAdd((long) tmp.size());

          modSize.set(bestNewModule, modSize.get(bestNewModule) + tmp.size());
          qi.set(bestNewModule, bestExitPrNewModule);
          modRank.set(bestNewModule, bestRankNewModule);
          snodeMod.set(sndID, bestNewModule);

          modSize.set(oldModule, modSize.get(oldModule) - tmp.size());
          qi.set(oldModule, bestExitPrOldModule);
          modRank.set(oldModule, bestRankOldModule);
          if (modSize.get(oldModule) < 1) {
            modSize.remove(oldModule);
            modRank.remove(oldModule);
            qi.remove(oldModule);
            emptyMod.set(oldModule, oldModule);
          }

          auxMap.set(0L, bestNewSigmaQI);
          auxMap.reduceAdd(1L, bestDiffCodeLen / log(2.0));
        }
      });
      coarseCnt++;
    } while (coarseCnt < maxIter && moved.get() > 0);
  }
}
```
