/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package Apps;

import java.util.ArrayList;
import java.util.List;

import oracle.pgx.api.Analyst;
import oracle.pgx.api.Pgx;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxSession;
import oracle.pgx.api.VertexSet;
import oracle.pgx.api.beta.frames.PgxFrame;
import oracle.pgx.api.beta.mllib.SupervisedGraphWiseModel;
import oracle.pgx.api.filter.ResultSetVertexFilter;
import oracle.pgx.common.types.PropertyType;
import oracle.pgx.config.FileGraphConfigBuilder;
import oracle.pgx.config.Format;
import oracle.pgx.config.GraphConfigBuilder;

public class CoraApp {
  public static void main(String[] args) throws Exception {
    PgxSession session = Pgx.createSession("cora-session");
    Analyst analyst = session.createAnalyst();

    System.out.println("Loading and preparing graph...");
    FileGraphConfigBuilder configBuilder = GraphConfigBuilder.forFileFormat(Format.CSV)
        .addVertexUri("data/graph/cora/cora.content")
        .addEdgeUri("data/graph/cora/cora.cites")
        .setSeparator("\t")
        .setVertexIdColumn(1)
        .setEdgeSourceColumn(2)
        .setEdgeDestinationColumn(1)
        .hasHeader(false);

    List<String> featureNames = new ArrayList<>();
    configBuilder.addVertexProperty("ID", PropertyType.INTEGER, 0, 1);
    configBuilder.addVertexProperty("label", PropertyType.STRING, 0, 1435);
    for (int i = 0; i < 1433; i++) {
      featureNames.add("feature" + i);
      configBuilder.addVertexProperty("feature" + i, PropertyType.DOUBLE, 0, i + 2);
    }

    PgxGraph cora = session.readGraphWithProperties(configBuilder.build(), "cora");

    // store the labels
    PgxFrame labels = session.queryPgql("SELECT ID(v) as vertexId, v.label as label FROM cora MATCH (v)").toFrame();

    // create a training graph
    PgxGraph trainGraph = cora.filter(
        new ResultSetVertexFilter(session.queryPgql("SELECT v FROM cora MATCH (v) WHERE ID(v) % 4 != 0"), "v"));

    // the test set - nodes not seen during training
    VertexSet<Integer> testSet = cora.getVertices(
        new ResultSetVertexFilter(session.queryPgql("SELECT v FROM cora MATCH (v) WHERE ID(v) % 4 = 0"), "v"));

    System.out.println("Done loading!");

    // fit the model
    System.out.println("Fitting model...");
    SupervisedGraphWiseModel model = analyst.supervisedGraphWiseModelBuilder()//
        .setVertexTargetPropertyName("label")//
        .setVertexInputPropertyNames(featureNames)//
        .build();

    model.fit(trainGraph);
    System.out.println("Done fitting!");

    // evaluate the model on the unseen nodes
    model.evaluateLabels(cora, testSet).print();

    // infer and store embeddings for all nodes
    PgxFrame embeddings = model.inferEmbeddings(cora, cora.getVertices());

    labels.join(embeddings, "vertexId", "left", "right")
        .select("leftvertexId", "rightembedding", "leftlabel")
        .flattenAll()
        .write()
        .overwrite(true)
        .csv("embeddings.csv");
  }
}
