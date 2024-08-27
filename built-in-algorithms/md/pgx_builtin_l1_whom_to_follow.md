# WTF (Whom To Follow) Algorithm

- **Category:** link prediction
- **Algorithm ID:** pgx_builtin_l1_whom_to_follow
- **Time Complexity:** O(E * (p + s)) with E = number of edges, p <= maximum number of iterations for the Pagerank step, s <= maximum number of iterations for the SALSA step
- **Space Requirement:** O(5 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#whomToFollow(PgxGraph graph, PgxVertex<ID> vertex)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#whomToFollow-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-)
  - [Analyst#whomToFollow(PgxGraph graph, PgxVertex<ID> vertex, int topK)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#whomToFollow-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-int-)
  - [Analyst#whomToFollow(PgxGraph graph, PgxVertex<ID> vertex, int topK, int sizeCircleOfTrust)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#whomToFollow-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-int-int-)
  - [Analyst#whomToFollow(PgxGraph graph, PgxVertex<ID> vertex, int topK, int sizeCircleOfTrust, int maxIter, double tol, double dampingFactor, int salsaMaxIter, double salsaTol)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#whomToFollow-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-int-int-int-double-double-int-double-)
  - [Analyst#whomToFollow(PgxGraph graph, PgxVertex<ID> vertex, int topK, int sizeCircleOfTrust, int maxIter, double tol, double dampingFactor, int salsaMaxIter, double salsaTol, VertexSequence<ID> hubs, VertexSequence<ID> authorities)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#whomToFollow-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-int-int-int-double-double-int-double-oracle.pgx.api.VertexSequence-oracle.pgx.api.VertexSequence-)
  - [Analyst#whomToFollow(PgxGraph graph, PgxVertex<ID> vertex, int topK, int sizeCircleOfTrust, VertexSequence<ID> hubs, VertexSequence<ID> authorities)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#whomToFollow-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-int-int-oracle.pgx.api.VertexSequence-oracle.pgx.api.VertexSequence-)
  - [Analyst#whomToFollow(PgxGraph graph, PgxVertex<ID> vertex, int topK, VertexSequence<ID> hubs, VertexSequence<ID> authorities)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#whomToFollow-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-int-oracle.pgx.api.VertexSequence-oracle.pgx.api.VertexSequence-)
  - [Analyst#whomToFollow(PgxGraph graph, PgxVertex<ID> vertex, VertexSequence<ID> hubs, VertexSequence<ID> authorities)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#whomToFollow-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.VertexSequence-oracle.pgx.api.VertexSequence-)

The Whom To Follow algorithm is composed by two main stages: the first one is meant to get the relevant vertices (users) for a given source vertex (particular user), which in this implementation is done with personalized Pagerank for the given source vertex. While the second stage analyzes the relationships between the relevant vertices previously found through the edges linking them with their neighbors. This second stage relies on SALSA algorithm and it assigns a ranking score to all the hubs and authority vertices, so the recommendations can come from this assigned values. Whom To Follow takes the concept of authority and hub vertices, and adapts it to users in social networks. The hub vertices become similar users with respect to the given source vertex (also an user), and the authority vertices are translated into users that might be on the interest of the source vertex, i.e. users to follow.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `source` | node | the chosen vertex from the graph for personalization of the recommendations. |
| `top_k` | int | the maximum number of recommendations that will be returned. This number should be smaller than the size of the circle of trust. |
| `circle_of_trust` | int | the maximum size of the circle of trust. |
| `max_iter` | int | maximum number of iterations that will be performed for the Pagerank stage. |
| `tol` | double | maximum tolerated error value for the Pagerank stage. The stage will stop once the sum of the error values of all vertices becomes smaller than this value. |
| `damping_factor` | double | damping factor for the Pagerank stage. |
| `salsa_max_iter` | int | maximum number of iterations that will be performed for the SALSA stage. |
| `salsa_tol` | double | maximum tolerated error value for the SALSA stage. The stage will stop once the sum of the error values of all vertices becomes smaller than this value. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `hubs` | nodeSeq | vertex sequence holding the top rated hub vertices (similar users) for the recommendations. |
| `auths` | nodeSeq | vertex sequence holding the top rated authority vertices (users to follow) for the recommendations. |

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
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSequence;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.abs;

@GraphAlgorithm
public class Wtf {
  public void whomToFollow(PgxGraph g, PgxVertex source, int rawTopK, int circleOfTrust, int maxIter, double tol,
      double dampingFactor, int salsaMaxIter, double salsaTol, @Out VertexSequence hubs, @Out VertexSequence auths) {
    // adjusting sizes of circle of trust and topK values if needed
    long circle = circleOfTrust > g.getNumVertices() ? g.getNumVertices() - 1 : circleOfTrust;
    long topK = rawTopK > circle ? circle : rawTopK;

    // computing personalized pagerank for getting relevant vertices
    VertexProperty<Double> rank = VertexProperty.create();
    personalizedPagerank(g, source, tol, dampingFactor, maxIter, rank);

    // creating circle of trust with top ranked vertices
    VertexSet circleSet = VertexSet.create();
    VertexSequence copyCircleSet = VertexSequence.create();
    VertexProperty<Integer> isLeft = VertexProperty.create(0);
    Scalar<Integer> k = Scalar.create(0);

    PgxMap<PgxVertex, Double> heap = PgxMap.create();
    g.getVertices().filter(n -> k.get() < circle && n != source).forSequential(n -> {
      isLeft.set(n, 1);
      circleSet.add(n);
      heap.set(n, rank.get(n));
      k.increment();
    });
    while (heap.size() > 0) {
      PgxVertex n = heap.getKeyForMaxValue();
      heap.remove(n);
      copyCircleSet.pushFront(n);
    }

    // creating bipartite graph with vertices from the circle of trust and their neighbors
    copyCircleSet.forSequential(n -> {
      long localAuth = n.getOutNeighbors().filter(nbr -> isLeft.get(nbr) != 1 && nbr != source).size();

      if (localAuth == 0) {
        circleSet.remove(n);
        isLeft.set(n, 0);
      }
    });

    VertexProperty<Double> deg = VertexProperty.create();
    Scalar<Integer> numHubs = Scalar.create(0);
    Scalar<Integer> numAuths = Scalar.create(0);

    circleSet.forEach(n -> {
      Scalar<Double> localAuth = Scalar.create(0d);
      n.getOutNeighbors().filter(nbr -> isLeft.get(nbr) != 1 && nbr != source).forEach(nbr -> {
        if (isLeft.get(nbr) == 0) {
          numAuths.increment();
          isLeft.set(nbr, 2);
          deg.set(nbr, (double) nbr.getInNeighbors().filter(nbr2 -> isLeft.get(nbr2) == 1).size());
        }
        localAuth.increment();
      });
      deg.set(n, localAuth.get());
      numHubs.increment();
    });

    // salsa step for recommendations
    salsa(g, salsaTol, salsaMaxIter, numHubs.get(), numAuths.get(), isLeft, deg, rank);

    // getting topK hubs and auths
    getTopNodes(g, topK, numHubs.get(), numAuths.get(), rank, isLeft, hubs, auths);
  }

  void personalizedPagerank(PgxGraph g, PgxVertex v, double tol, double dampingFactor, int maxIter,
      @Out VertexProperty<Double> rank) {
    double numVertices = g.getNumVertices();
    Scalar<Double> diff = Scalar.create(0.0);
    int cnt = 0;
    rank.setAll(0d);
    rank.set(v, 1.0);

    do {
      diff.set(0.0);
      double danglingFactor =
          dampingFactor / numVertices * g.getVertices().filter(n -> n.getOutDegree() == 0).sum(rank);

      g.getVertices().forEach(t -> {
        double val1 = (t == v) ? (1 - dampingFactor) : 0;
        double val2 = dampingFactor * t.getInNeighbors().sum(w -> rank.get(w) / w.getOutDegree());
        double val = val1 + val2 + danglingFactor;
        diff.reduceAdd(abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while (diff.get() > tol && cnt < maxIter);
  }

  void salsa(PgxGraph g, double salsaTol, int salsaMaxIter, int numHubs, int numAuths, VertexProperty<Integer> isLeft,
      VertexProperty<Double> deg, @Out VertexProperty<Double> rank) {
    Scalar<Double> diff = Scalar.create(0.0);
    VertexProperty<Double> filteredWeightedNeighborRanks = VertexProperty.create();
    int cnt = 0;

    g.getVertices().filter(n -> isLeft.get(n) != 0).forEach(n -> {
      if (isLeft.get(n) == 1) {
        rank.set(n, 1.0 / numHubs);
      } else {
        rank.set(n, 1.0 / numAuths);
      }
    });

    do {
      diff.set(0.0);
      g.getVertices().forEach(u -> {
        Scalar<Double> val = Scalar.create(0.0);
        if (isLeft.get(u) == 2) {
          val.reduceAdd(u.getInNeighbors().filter(w -> isLeft.get(w) == 1).sum(w -> rank.get(w) / deg.get(w)));
          val.set(val.get() / deg.get(u));
        } else if (isLeft.get(u) == 1) {
          val.reduceAdd(u.getOutNeighbors().filter(w -> isLeft.get(w) == 2).sum(w -> rank.get(w) / deg.get(w)));
          val.set(val.get() / deg.get(u));
        }
        filteredWeightedNeighborRanks.setDeferred(u, val.get());
      });

      g.getVertices().filter(n -> isLeft.get(n) != 0).forEach(n -> {
        Scalar<Double> val = Scalar.create(0.0);
        if (isLeft.get(n) == 1) {
          n.getOutNeighbors().filter(u -> isLeft.get(u) == 2).forEach(u ->
              val.reduceAdd(filteredWeightedNeighborRanks.get(u)));
        } else {
          n.getInNeighbors().filter(u -> isLeft.get(u) == 1).forEach(u ->
              val.reduceAdd(filteredWeightedNeighborRanks.get(u)));
        }

        diff.reduceAdd(abs(val.get() - rank.get(n)));
        rank.setDeferred(n, val.get());
      });
      cnt++;
    } while (diff.get() > salsaTol && cnt < salsaMaxIter);
  }

  void getTopNodes(PgxGraph g, long topK, int numHubs, int numAuths, VertexProperty<Double> rank,
      VertexProperty<Integer> isLeft, @Out VertexSequence hubs, @Out VertexSequence auths) {

    PgxMap<PgxVertex, Double> hubsHeap = PgxMap.create();
    PgxMap<PgxVertex, Double> authsHeap = PgxMap.create();
    g.getVertices().filter(n -> isLeft.get(n) == 1 || isLeft.get(n) == 2).forSequential(n -> {
      if (isLeft.get(n) == 1) {
        hubsHeap.set(n, rank.get(n));
      } else {
        authsHeap.set(n, rank.get(n));
      }
    });

    fillOutputFromHeap(g, topK, numHubs, hubsHeap, hubs);
    fillOutputFromHeap(g, topK, numAuths, authsHeap, auths);
  }

  void fillOutputFromHeap(PgxGraph g, long topK, int maxCount, PgxMap<PgxVertex, Double> heap,
      @Out VertexSequence list) {

    Scalar<Long> k = Scalar.create(0L);
    while (k.get() < maxCount && k.get() < topK) {
      PgxVertex n = heap.getKeyForMaxValue();
      heap.remove(n);
      list.pushBack(n);
      k.increment();
      if (k.get() == maxCount) {
        return;
      }
    }
  }
}
```
