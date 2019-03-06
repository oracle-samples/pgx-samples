/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package DeepWalkApp;

import oracle.pgx.api.PgxSession;
import oracle.pgx.api.Pgx;
import oracle.pgx.api.Analyst;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.beta.mllib.DeepWalkModel;
import oracle.pgx.api.beta.frames.PgxFrame;
import java.util.Arrays;


public class GenerateNodeVectors {
  public static void main(String[] args) throws Exception {
    PgxSession session = Pgx.createSession("deepwalk-session");
    Analyst analyst = session.createAnalyst();

    String src_path = args[0];

    // read the input graph (built using PGX)
    PgxGraph graph = session.readGraphWithProperties(src_path+"/data/graph/sample/sample.json").undirect();

    // set the hyper-parameters of the DeepWalk model
    DeepWalkModel model = analyst.deepWalkModelBuilder()
        .setMinWordFrequency(1)
        .setBatchSize(32)
        .setNumEpochs(1)
        .setLayerSize(20)
        .setLearningRate(0.05)
        .setMinLearningRate(0.0001)
        .setWindowSize(2)
        .setWalksPerVertex(4)
        .setWalkLength(3)
        .setSampleRate(0.00001)
        .setNegativeSample(1)
        .setValidationFraction(0.01)
        .build();

    // Train the DeepWalk model on the input graph
    model.fit(graph);

    double loss = model.getLoss();
    System.out.println("Loss: " + loss);

    // Export the trained node vectors using PgxFrames
    PgxFrame vertexVectors = model.getTrainedVertexVectors().flattenAll();
    vertexVectors.write()
        .overwrite(true)
        .csv()
        .store(src_path+"/data/node_vectors.csv");

    // Note there are other features also that we did not use in this application but checkout the details
    // at https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/deepwalk.html
  }
}