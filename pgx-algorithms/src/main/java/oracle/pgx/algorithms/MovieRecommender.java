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
import static java.lang.Math.sqrt;
import static oracle.pgx.algorithms.Utils.atIndex;
import static oracle.pgx.algorithms.Utils.createOutputFile;
import static oracle.pgx.algorithms.Utils.getResource;
import static oracle.pgx.algorithms.Utils.writeln;
import static oracle.pgx.algorithms.Utils.writer;
import static oracle.pgx.common.types.PropertyType.DOUBLE;

public class MovieRecommender {
  public static Logger logger = LoggerFactory.getLogger(MovieRecommender.class);
  public static final int VECTOR_LENGTH = 100;
  public static final double LEARNING_RATE = 0.8;
  public static final double CHANGE_PER_STEP = 1.0;
  public static final double LAMBDA = 0.5;
  public static final int MAX_STEP = 100;

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
      FileGraphConfig trainingConfig = getGraphConfigBuilder(data).addEdgeUri(data.resolve("ratings-training.csv").toString()).build();
      PgxGraph trainingGraph = session.readGraphWithProperties(trainingConfig);
      logger.info("Loaded training graph {}", trainingGraph);

      FileGraphConfig testConfig = getGraphConfigBuilder(data).addEdgeUri(data.resolve("ratings-test.csv").toString()).build();
      PgxGraph testGraph = session.readGraphWithProperties(testConfig);
      Stream<PgxEdge> testEdges = filterExisting(trainingGraph, testGraph);
      EdgeProperty<Double> predictions = testGraph.createEdgeProperty(DOUBLE);
      logger.info("Loaded test graph {}", testGraph);

      VertexProperty<Object, Boolean> is_left = trainingGraph.getVertexProperty("is_left");
      EdgeProperty<Double> rating = trainingGraph.getEdgeProperty("rating");
      VertexProperty<Object, Vect<Double>> features = trainingGraph.createVertexProperty(DOUBLE, VECTOR_LENGTH, "features", false);
      program.run(trainingGraph, is_left, rating, LEARNING_RATE, CHANGE_PER_STEP, LAMBDA, MAX_STEP, VECTOR_LENGTH, features);
      logger.info("Finished running Matrix Factorization Gradient Descent");

      // Predict rating for edges in the test set
      testEdges.forEach(edge -> {
        Vect<Double> userFeature = features.get(edge.getSource());
        Vect<Double> movieFeature = features.get(edge.getDestination());

        double predictedRating = dotProduct(userFeature, movieFeature);
        double normalizedRating = max(min(predictedRating, 5), 1);
        predictions.set(edge, normalizedRating);
      });

      // Compute root mean squared error
      double sumSquaredError = 0;
      double length = 0;

      for (PgxEdge e : testGraph.getEdges()) {
        double actualRating = rating.get(e);
        double predictionRating = Math.round(predictions.get(e));

        double error = predictionRating - actualRating;
        double squaredError = Math.pow(error, 2);

        sumSquaredError += squaredError;
        length++;
      }

      double meanSquaredError = sumSquaredError / length;
      double rootMeanSquaredError = sqrt(meanSquaredError);

      System.out.println("RMSE = " + rootMeanSquaredError);
    }
  }

  private static Stream<PgxEdge> filterExisting(PgxGraph trainingGraph, PgxGraph testGraph) {
    return testGraph.getEdges().stream().filter(e -> {
      Object sourceId = e.getSource().getId();
      Object targetId = e.getDestination().getId();

      if (trainingGraph.getVertex(sourceId) == null) {
        return false;
      }

      if (trainingGraph.getVertex(targetId) == null) {
        return false;
      }

      return true;
    });
  }

  private static double dotProduct(Vect<Double> v1, Vect<Double> v2) {
    Stream<Double> s1 = Arrays.stream(v1.toArray());
    Stream<Double> s2 = Arrays.stream(v2.toArray());

    return Streams
        .zip(s1, s2, (i1, i2) -> i1 * i2)
        .reduce(0.0, Double::sum);
  }

  private static FileGraphConfigBuilder getGraphConfigBuilder(Path data) {
    return GraphConfigBuilder
            .forFileFormat(Format.CSV)
            .addVertexUri(data.resolve("movies.csv").toString())
            .addVertexUri(data.resolve("users.csv").toString())
            .addVertexProperty("name", PropertyType.STRING)
            .addVertexProperty("genre", PropertyType.STRING)
            .addVertexProperty("is_left", PropertyType.BOOLEAN)
            .addEdgeProperty("rating", DOUBLE)
            .addEdgeProperty("timestamp", PropertyType.INTEGER);
  }

  private static Path prepare(Path inputDir) {
    try {
      Path tempDir = Files.createTempDirectory("pgx-algorithm-sample");
      logger.info("Using temporary directory {}", tempDir);

      createUsers(inputDir, tempDir);
      createMovies(inputDir, tempDir);
      splitRatings(inputDir, tempDir);

      return tempDir;
    } catch (IOException e) {
      throw new RuntimeException("Cannot create a temporary directory.", e);
    }
  }
  
  private static void createUsers(Path inputDir, Path tempDir) {
    Path path = inputDir.resolve("ratings.csv");
    Path output = createOutputFile(tempDir.resolve("users.csv"));

    try (Stream<String> lines = Files.lines(path).skip(1); Writer writer = writer(output)) {
      lines.map(Splitter.comma).map(atIndex(0)).distinct().forEach(user ->
        writeln(writer, "1" + user + ",,,true")
      );
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the users.", e);
    }
  }

  private static void createMovies(Path inputDir, Path tempDir) {
    Path input = inputDir.resolve("movies.csv");
    Path output = createOutputFile(tempDir.resolve("movies.csv"));

    try (Stream<String> lines = Files.lines(input).skip(1); Writer writer = writer(output)) {
      lines.map(line -> line.split(",", 2)).distinct().forEach(movie ->
        writeln(writer, "2" + movie[0] + "," + movie[1] + ",false")
      );
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the movies.", e);
    }
  }

  private static void splitRatings(Path inputDir, Path tempDir) {
    Path path = inputDir.resolve("ratings.csv");

    try {
      long rows = Files.lines(path).count();
      long testSize = rows / 5;
      long trainingSize = rows - testSize;

      Stream<String> training = Files.lines(path).skip(1).limit(trainingSize);
      Stream<String> test = Files.lines(path).skip(1 + trainingSize);

      createRatings(training, tempDir.resolve("ratings-training.csv"));
      createRatings(test, tempDir.resolve("ratings-test.csv"));
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the ratings.", e);
    }
  }

  private static void createRatings(Stream<String> lines, Path outputPath) {
    Path output = createOutputFile(outputPath);

    try (Writer writer = writer(output)) {
      lines.map(Splitter.comma).forEach(columns ->
        writeln(writer, "1" + columns[0] + ",2" + columns[1] + "," + columns[2] + "," + columns[3])
      );
    } catch (IOException e) {
      throw new RuntimeException("Unable to create output file.", e);
    }
  }
}
