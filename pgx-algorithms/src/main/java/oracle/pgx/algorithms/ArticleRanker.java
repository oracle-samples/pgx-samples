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

package oracle.pgx.algorithms;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import oracle.pgx.api.CompiledProgram;
import oracle.pgx.api.Pgx;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxSession;
import oracle.pgx.api.PgxVertex;
import oracle.pgx.api.VertexProperty;
import oracle.pgx.common.types.PropertyType;
import oracle.pgx.config.FileGraphConfig;
import oracle.pgx.config.Format;
import oracle.pgx.config.GraphConfig;
import oracle.pgx.config.GraphConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.lines;
import static oracle.pgx.algorithms.Utils.createOutputFile;
import static oracle.pgx.algorithms.Utils.getResource;
import static oracle.pgx.algorithms.Utils.limit;
import static oracle.pgx.algorithms.Utils.writeln;
import static oracle.pgx.algorithms.Utils.writer;

public class ArticleRanker {
  public static Logger logger = LoggerFactory.getLogger(ArticleRanker.class);
  public static final double TOLERANCE = 0.001;
  public static final double DAMPING = 0.85;
  public static final int MAX_ITER_COUNT = 1000;

  public static void main(String[] args) throws Exception {
    Path inputDir = Paths.get(args[0]);

    if (!Files.exists(inputDir)) {
      throw new IllegalArgumentException("The input directory '" + inputDir + "' does not exist.");
    }

    try (PgxSession session = Pgx.createSession("pgx-algorithm-session")) {
      String code = getResource("algorithms/ArticleRank.java");
      CompiledProgram program = session.compileProgram(code);
      logger.info("Compiled program {}", program);

      GraphConfig config = createGraphConfig(inputDir);
      PgxGraph graph = session.readGraphWithProperties(config);
      logger.info("Loaded graph {}", graph);

      VertexProperty<Object, Object> rank = graph.createVertexProperty(PropertyType.DOUBLE);
      program.run(graph, TOLERANCE, DAMPING, MAX_ITER_COUNT, rank);

      for (PgxVertex<Object> vertex : limit(graph.getVertices(), 10)) {
        System.out.println("ArticleRank " + vertex + " = " + rank.get(vertex));
      }
    }
  }

  private static FileGraphConfig createGraphConfig(Path inputDir) {
    try {
      Path tempDir = Files.createTempDirectory("pgx-sample-algorithm-articlerank");
      logger.info("Using temporary directory {}", tempDir);

      String articlesUri = createArticles(inputDir, tempDir);
      String citationsUri = createCitations(inputDir, tempDir);

      return GraphConfigBuilder
          .forFileFormat(Format.CSV)
          .addVertexUri(articlesUri)
          .addEdgeUri(citationsUri)
          .build();
    } catch (IOException e) {
      throw new RuntimeException("Cannot create a temporary directory.", e);
    }
  }

  private static String createArticles(Path inputDir, Path tempDir) {
    Path path = inputDir.resolve("web-Google.txt");
    Path output = createOutputFile(tempDir.resolve("articles.csv"));

    try {
      Set<Integer> seen = new HashSet<>();

      try (Writer writer = writer(output); Stream<String> lines = lines(path).skip(4)) {
        lines.map(Splitter.tab).forEach(columns -> {
          int source = Integer.parseInt(columns[0]);
          int target = Integer.parseInt(columns[1]);

          writeArticle(seen, writer, source);
          writeArticle(seen, writer, target);
        });
      }

      return output.toString();
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the articles.", e);
    }
  }

  private static void writeArticle(Set<Integer> seen, Writer writer, int article) {
    if (seen.contains(article)) {
      return;
    }

    writeln(writer, String.valueOf(article));
    seen.add(article);
  }

  private static String createCitations(Path inputDir, Path tempDir) {
    Path input = inputDir.resolve("web-Google.txt");
    Path output = createOutputFile(tempDir.resolve("citations.csv"));

    try (Writer writer = writer(output); Stream<String> lines = lines(input).skip(4)) {
      lines.map(Splitter.tab).forEach(columns -> {
        writeln(writer, columns[0] + "," + columns[1]);
      });

      return output.toString();
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the citations.", e);
    }
  }
}