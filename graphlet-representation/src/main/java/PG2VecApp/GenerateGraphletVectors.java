/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** This software is licensed to you under the Universal Permissive License (UPL).
 ** See below for license terms.
 **  ____________________________
 ** The Universal Permissive License (UPL), Version 1.0

 ** Subject to the condition set forth below, permission is hereby granted to any person
 ** obtaining a copy of this software, associated documentation and/or data (collectively the "Software"),
 ** free of charge and under any and all copyright rights in the Software, and any and all patent rights
 ** owned or freely licensable by each licensor hereunder covering either (i) the unmodified Software as
 ** contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both

 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the
 ** Software (each a "Larger Work" to which the Software is contributed by such licensors),

 ** without restriction, including without limitation the rights to copy, create derivative works of,
 ** display, perform, and distribute the Software and make, use, sell, offer for sale, import, export,
 ** have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights
 ** on either these or other terms.

 ** This license is subject to the following condition:

 ** The above copyright notice and either this complete permission notice or at a minimum a reference
 ** to the UPL must be included in all copies or substantial portions of the Software.

 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 ** NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 ** IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 ** WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 ** SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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