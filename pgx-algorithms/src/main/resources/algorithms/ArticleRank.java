/*
 * Copyright (C) 2019 Oracle and/or its affiliates. All rights reserved.
 */
import oracle.pgx.api.beta.PgxVertex;
import oracle.pgx.api.beta.annotations.GraphAlgorithm;
import oracle.pgx.api.beta.PgxGraph;
import oracle.pgx.api.beta.Scalar;
import oracle.pgx.api.beta.VertexProperty;
import oracle.pgx.api.beta.annotations.Out;

import static java.lang.Math.abs;

@GraphAlgorithm
public class ArticleRank {
  public void articleRank(PgxGraph G, double e, double d, int max_iter_count, @Out VertexProperty<Double> rank) {
    Scalar<Double> diff = Scalar.create();
    int cnt = 0;
    double N = G.getNumVertices();

    rank.setAll(1 / N);

    double avgOutDegree = G.getVertices().avg(PgxVertex::getOutDegree);

    do {
      diff.set(0.0);

      G.getVertices().forEach(t -> {
        /*
        // Pagerank:
        double in_sum = t.getInNeighbors().sum(w -> rank.get(w) / w.getOutDegree());
        double val = (1 - d) / N + d * in_sum + dangling_factor.get();
        diff.reduceAdd(abs(val - rank.get(t)));
        rank.setDeferred(t, val);
        */

        // ArticleRank:
        double in_sum = t.getInNeighbors().sum(inNeighbor -> rank.get(inNeighbor) / (avgOutDegree + inNeighbor.getOutDegree()));
        double val = (1 - d) + d * avgOutDegree * in_sum;
        diff.reduceAdd(abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while ((diff.get() > e) && (cnt < max_iter_count));
  }
}