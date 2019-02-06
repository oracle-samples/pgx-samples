/*
 * Copyright (C) 2013 - 2019 Oracle and/or its affiliates. All rights reserved.
 */
import oracle.pgx.api.beta.annotations.GraphAlgorithm;
import oracle.pgx.api.beta.PgxGraph;
import oracle.pgx.api.beta.Scalar;
import oracle.pgx.api.beta.VertexProperty;
import oracle.pgx.api.beta.annotations.Out;

import static java.lang.Math.abs;

@GraphAlgorithm
public class Pagerank {
  public void pagerank(PgxGraph G, double e, double d, int max_iter_count, boolean norm, @Out VertexProperty<Double> rank) {
    Scalar<Double> diff = Scalar.create();
    int cnt = 0;
    double N = G.getNumVertices();

    rank.setAll(1 / N);

    do {
      diff.set(0.0);
      Scalar<Double> dangling_factor = Scalar.create(0d);

      if (norm) {
        dangling_factor.set(d / N * G.getVertices().filter(v -> v.getOutDegree() == 0).sum(rank));
      }

      G.getVertices().forEach(t -> {
        double in_sum = t.getInNeighbors().sum(w -> rank.get(w) / w.getOutDegree());
        double val = (1 - d) / N + d * in_sum + dangling_factor.get();
        diff.reduceAdd(abs(val - rank.get(t)));
        rank.setDeferred(t, val);
      });
      cnt++;
    } while ((diff.get() > e) && (cnt < max_iter_count));
  }
}
