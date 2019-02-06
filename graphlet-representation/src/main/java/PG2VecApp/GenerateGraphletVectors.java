/*
 * Generate vector representations from graphlets using PGX
 * @author: rpatra
 */

package PG2VecApp;

import oracle.pgx.api.PgxSession;
import oracle.pgx.api.Pgx;
import oracle.pgx.api.Analyst;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.beta.mllib.Pg2vecModel;
import oracle.pgx.api.beta.frames.PgxFrame;
import java.util.Arrays;


public class GenerateGraphletVectors {
  public static void main(String[] args) throws Exception {
    PgxSession session = Pgx.createSession("pg2vec-session");
    Analyst analyst = session.createAnalyst();

    String src_path = args[0];

    // read the graphlets (built using PGX)
    PgxGraph graph = session.readGraphWithProperties(src_path+"/data/graph/NCI109/NCI109.json").undirect();

    // set the hyper-parameters of the PG2Vec model
    Pg2vecModel model = analyst.pg2vecModelBuilder()
        .setGraphLetIdPropertyName("graph_id")
        .setVertexPropertyNames(Arrays.asList("node_label"))
        .setMinWordFrequency(1)
        .setBatchSize(128)
        .setNumEpochs(5)
        .setLayerSize(200)
        .setLearningRate(0.04)
        .setMinLearningRate(0.0001)
        .setWindowSize(4)
        .setWalksPerVertex(5)
        .setWalkLength(8)
        .setUseGraphletSize(true)
        .setValidationFraction(0.05)
        .build();

    // Train the PG2Vec model on the input graph (consist of all the graphlets)
    model.fit(graph);

    double loss = model.getLoss();
    System.out.println("Loss: " + loss);

    // Export the trained graphlet vectors using PgxFrames
    PgxFrame graphletVectors = model.getTrainedGraphletVectors().flattenAll();
    graphletVectors.write()
        .overwrite(true)
        .csv()
        .store(src_path+"/data/graphlet_vectors.csv");

    // Note there are other features also that we did not use in this application but checkout the details
    // at https://docs.oracle.com/cd/E56133_01/latest/tutorials/mllib/pg2vec.html
  }
}