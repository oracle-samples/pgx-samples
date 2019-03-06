/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
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