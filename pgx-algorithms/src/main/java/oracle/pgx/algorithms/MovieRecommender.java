/*
 * Copyright (C) 2019 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import oracle.pgx.api.CompiledProgram;
import oracle.pgx.api.EdgeProperty;
import oracle.pgx.api.Pgx;
import oracle.pgx.api.PgxEdge;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxSession;
import oracle.pgx.api.VertexProperty;
import oracle.pgx.common.types.PropertyType;
import oracle.pgx.common.util.vector.Vect;
import oracle.pgx.config.FileGraphConfig;
import oracle.pgx.config.FileGraphConfigBuilder;
import oracle.pgx.config.Format;
import oracle.pgx.config.GraphConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Double.min;
import static java.lang.Math.max;
import static oracle.pgx.algorithms.Utils.atIndex;
import static oracle.pgx.algorithms.Utils.createOutputFile;
import static oracle.pgx.algorithms.Utils.getResource;
import static oracle.pgx.algorithms.Utils.writeln;
import static oracle.pgx.algorithms.Utils.writer;
import static oracle.pgx.common.types.PropertyType.DOUBLE;

public class MovieRecommender {
  public static Logger logger = LoggerFactory.getLogger(MovieRecommender.class);
  public static final int VECTOR_LENGTH = 10;
  public static final double LEARNING_RATE = 0.00008;
  public static final double CHANGE_PER_STEP = 1.0;
  public static final int MAX_STEP = 300;

  public static void main(String[] args) throws Exception {
    Path inputDir = Paths.get(args[0]);

    if (!Files.exists(inputDir)) {
      throw new IllegalArgumentException("The argument path '" + inputDir + "' does not exist.");
    }

    try (PgxSession session = Pgx.createSession("pgx-algorithm-session")) {
      String code = getResource("algorithms/MatrixFactorizationGradientDescent.java");
      CompiledProgram program = session.compileProgram(code);
      logger.info("Compiled program {}", program);

      Path data = prepare(inputDir);

      FileGraphConfig trainingConfig = getGraphConfigBuilder()
          .addEdgeUri(data.resolve("ratings-training.csv").toString())
          .addVertexUri(data.resolve("movies-training.csv").toString())
          .addVertexUri(data.resolve("users-training.csv").toString())
          .build();
      PgxGraph trainingGraph = session.readGraphWithProperties(trainingConfig);
      logger.info("Loaded training graph {}", trainingGraph);

      FileGraphConfig testConfig = getGraphConfigBuilder()
          .addEdgeUri(data.resolve("ratings-test.csv").toString())
          .addVertexUri(data.resolve("movies-test.csv").toString())
          .addVertexUri(data.resolve("users-test.csv").toString())
          .build();
      PgxGraph testGraph = session.readGraphWithProperties(testConfig);
      logger.info("Test graph edges size = {}", testGraph.getEdges().size());
      Stream<PgxEdge> testEdgesStream = filterExisting(trainingGraph, testGraph);
      List<PgxEdge> testEdges = testEdgesStream.collect(Collectors.toList());
      logger.info("Test graph filtered edges size = {}", testEdges.size());
      EdgeProperty<Double> predictions = testGraph.createEdgeProperty(DOUBLE);
      logger.info("Loaded test graph {}", testGraph);
      VertexProperty<Object, Boolean> is_left = trainingGraph.getVertexProperty("is_left");
      EdgeProperty<Double> rating = trainingGraph.getEdgeProperty("rating");
      VertexProperty<Object, Vect<Double>> features = trainingGraph.createVertexProperty(DOUBLE, VECTOR_LENGTH, "features", false);
      program.run(trainingGraph, is_left, rating, LEARNING_RATE, CHANGE_PER_STEP, MAX_STEP, VECTOR_LENGTH, features);
      logger.info("Finished running Matrix Factorization Gradient Descent");

      // Predict rating for edges in the test set
      testEdges.forEach(edge -> {
        Vect<Double> userFeature = features.get(edge.getSource());
        Vect<Double> movieFeature = features.get(edge.getDestination());

        double predictedRating = Math.round(dotProduct(userFeature, movieFeature));
        double normalizedRating = max(min(predictedRating, 5), 1);
        predictions.set(edge, normalizedRating);
      });

      // Compute root mean squared error
      double sumSquaredError = 0;
      double length = 0;

      EdgeProperty<Double> testGraphRating = testGraph.getEdgeProperty("rating");

      for (PgxEdge testEdge : testEdges) {
        double actualRating = testGraphRating.get(testEdge);
        double predictedRating = predictions.get(testEdge);

        double error = predictedRating - actualRating;
        double squaredError = Math.pow(error, 2);

        sumSquaredError += squaredError;
        length++;
      }

      logger.info("sumSquaredError = {}", sumSquaredError);
      logger.info("length = {}", length);

      double meanSquaredError = sumSquaredError / length;
      double rootMeanSquaredError = Math.sqrt(meanSquaredError);

      System.out.println("RMSE = " + rootMeanSquaredError);
    }
  }

  private static Stream<PgxEdge> filterExisting(PgxGraph trainingGraph, PgxGraph testGraph) {
    return testGraph.getEdges().stream().filter(e -> {
      Object sourceId = e.getSource().getId();
      Object targetId = e.getDestination().getId();

      return trainingGraph.hasVertex(sourceId) && trainingGraph.hasVertex(targetId);
    });
  }

  private static double dotProduct(Vect<Double> v1, Vect<Double> v2) {
    Stream<Double> s1 = Arrays.stream(v1.toArray());
    Stream<Double> s2 = Arrays.stream(v2.toArray());

    return Streams
        .zip(s1, s2, (i1, i2) -> i1 * i2)
        .reduce(0.0, Double::sum);
  }

  private static FileGraphConfigBuilder getGraphConfigBuilder() {
    return GraphConfigBuilder
            .forFileFormat(Format.CSV)
            .addVertexProperty("is_left", PropertyType.BOOLEAN)
            .addEdgeProperty("rating", DOUBLE)
            .addEdgeProperty("timestamp", PropertyType.INTEGER);
  }

  private static Path prepare(Path inputDir) {
    try {
      Path tempDir = Files.createTempDirectory("pgx-algorithm-sample");
      logger.info("Using temporary directory {}", tempDir);

      // 0 = training, 1 = test
      Path[] ratings = splitRatings(inputDir, tempDir);
      createUsers(ratings[0], tempDir.resolve("users-training.csv"));
      createUsers(ratings[1], tempDir.resolve("users-test.csv"));
      createMovies(ratings[0], tempDir.resolve("movies-training.csv"));
      createMovies(ratings[1], tempDir.resolve("movies-test.csv"));

      return tempDir;
    } catch (IOException e) {
      throw new RuntimeException("Cannot create a temporary directory.", e);
    }
  }

  private static Path[] splitRatings(Path inputDir, Path tempDir) {
    Path path = inputDir.resolve("ratings.csv");

    try {
      List<String> lines = Files
          .lines(path)
          .skip(1)
          .collect(Collectors.toList());

      // Shuffle the lines to avoid bias in partitioning
      Collections.shuffle(lines);

      // Partition on 80%/20% basis
      long rows = lines.size();
      long testSize = rows / 5;
      long trainingSize = rows - testSize;
      logger.info("Total ratings = {}, test ratings = {}, training ratings = {}", rows, testSize, trainingSize);

      Stream<String> training = lines.stream().limit(trainingSize);
      Stream<String> test = lines.stream().skip(trainingSize);

      // Create the training- and test files
      Path ratingsTraining = createRatings(training, tempDir.resolve("ratings-training.csv"));
      Path ratingsTest = createRatings(test, tempDir.resolve("ratings-test.csv"));

      return new Path[] {
          ratingsTraining,
          ratingsTest
      };
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the ratings.", e);
    }
  }

  private static Path createRatings(Stream<String> lines, Path outputPath) {
    Path output = createOutputFile(outputPath);

    try (Writer writer = writer(output)) {
      lines.map(Splitter.comma).forEach(columns ->
        writeln(writer, "1" + columns[0] + ",2" + columns[1] + "," + columns[2] + "," + columns[3])
      );

      return output;
    } catch (IOException e) {
      throw new RuntimeException("Unable to create output file.", e);
    }
  }

  private static void createUsers(Path inputFile, Path outputFile) {
    Path output = createOutputFile(outputFile);

    try (Stream<String> lines = Files.lines(inputFile); Writer writer = writer(output)) {
      lines.map(Splitter.comma).map(atIndex(0)).distinct().forEach(user ->
          writeln(writer, user + ",true")
      );
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the users.", e);
    }
  }

  private static void createMovies(Path inputFile, Path outputFile) {
    Path output = createOutputFile(outputFile);

    try (Stream<String> lines = Files.lines(inputFile); Writer writer = writer(output)) {
      lines.map(Splitter.comma).map(atIndex(1)).distinct().forEach(movie ->
          writeln(writer, movie + ",false")
      );
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the movies.", e);
    }
  }
}
