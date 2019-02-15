/*
 * Copyright (C) 2019 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import java.io.File;
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

import static oracle.pgx.algorithms.Utils.atIndex;
import static oracle.pgx.algorithms.Utils.getResource;
import static oracle.pgx.algorithms.Utils.writeln;
import static oracle.pgx.algorithms.Utils.writer;

public class MovieRecommender {
  public static Logger logger = LoggerFactory.getLogger(MovieRecommender.class);
  public static final int VECTOR_LENGTH = 20;
  public static final double LEARNING_RATE = 0.32;
  public static final double CHANGE_PER_STEP = 0.85;
  public static final double LAMBDA = 0.25;
  public static final int MAX_STEP = 100;

  public static void main(String[] args) throws Exception {
    Path rawData = Paths.get(args[0]);

    if (!Files.exists(rawData)) {
      throw new IllegalArgumentException("The argument path '" + rawData + "' does not exist.");
    }

    try (PgxSession session = Pgx.createSession("pgx-algorithm-session")) {
      String code = getResource("algorithms/MatrixFactorizationGradientDescent.java");
      CompiledProgram program = session.compileProgram(code);
      logger.info("Compiled program {}", program);

      // TODO: Improve next block
      // ------------------------------------------------------------------------------------------------

      // Massage raw data
      Path data = prepare(rawData);

      // Load training graph
      FileGraphConfig trainingConfig = getGraphConfigBuilder(data)
          .addEdgeUri(data.resolve("ratings-training.csv").toString())
          .build();

      PgxGraph trainingGraph = session.readGraphWithProperties(trainingConfig);

      // Run matrix vectorization
      VertexProperty<Object, Boolean> is_left = trainingGraph.getVertexProperty("is_left");
      EdgeProperty<Double> rating = trainingGraph.getEdgeProperty("rating");
      VertexProperty<Object, Vect<Double>> features = trainingGraph.createVertexProperty(PropertyType.DOUBLE, VECTOR_LENGTH, "features", false);
      program.run(trainingGraph, is_left, rating, LEARNING_RATE, CHANGE_PER_STEP, LAMBDA, MAX_STEP, VECTOR_LENGTH, features);

      // Load test graph
      FileGraphConfig testConfig = getGraphConfigBuilder(data)
          .addEdgeUri(data.resolve("ratings-test.csv").toString())
          .build();

      PgxGraph testGraph = session.readGraphWithProperties(testConfig);

      // Compute prediction for the test set
      EdgeProperty<Object> prediction = testGraph.createEdgeProperty(PropertyType.DOUBLE);

      for (PgxEdge e : testGraph.getEdges()) {
        if (trainingGraph.getVertex(e.getSource().getId()) == null) {
          continue;
        }

        if (trainingGraph.getVertex(e.getDestination().getId()) == null) {
          continue;
        }

        Vect<Double> userFeature = features.get(trainingGraph.getVertex(e.getSource().getId()));
        Vect<Double> movieFeature = features.get(trainingGraph.getVertex(e.getDestination().getId()));

        prediction.set(e, Double.min(dotProduct(userFeature, movieFeature), 5));
      }

      // Compute root mean squared error
      double sumSquaredError = 0;
      double length = 0;

      for (PgxEdge e : testGraph.getEdges()) {
        double actualRating = rating.get(e.getId());
        double predictionRating = e.getProperty("rating");
        double error = predictionRating - actualRating;
        double squaredError = Math.pow(error, 2);

        sumSquaredError += squaredError;
        length++;
      }

      double meanSquaredError = sumSquaredError / length;
      double rootMeanSquaredError = Math.sqrt(meanSquaredError);
      // ------------------------------------------------------------------------------------------------

      System.out.println("RMSE = " + rootMeanSquaredError);
    }
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
            .addEdgeProperty("rating", PropertyType.DOUBLE)
            .addEdgeProperty("timestamp", PropertyType.INTEGER);
  }

  private static Path prepare(Path rawDataPath) {
    try {
      Path data = Files.createTempDirectory("pgx-algorithm-sample");
      logger.info("Using temporary directory {}", data);

      createUsers(rawDataPath, data);
      createMovies(rawDataPath, data);
      createRatings(rawDataPath, data);

      return data;
    } catch (IOException e) {
      throw new RuntimeException("Cannot create a temporary directory.", e);
    }
  }

  private static void createUsers(Path rawDataPath, Path data) {
    Path path = rawDataPath.resolve("ratings.csv");
    Path output = createOutputFile(data.resolve("users.csv"));

    try (Stream<String> lines = Files.lines(path).skip(1); Writer writer = writer(output)) {
      lines.map(Splitter.comma).map(atIndex(0)).distinct().forEach(user ->
        writeln(writer, "1" + user + ",,,true")
      );
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the users.", e);
    }
  }

  private static void createMovies(Path rawDataPath, Path data) {
    Path input = rawDataPath.resolve("movies.csv");
    Path output = createOutputFile(data.resolve("movies.csv"));

    try (Stream<String> lines = Files.lines(input).skip(1); Writer writer = writer(output)) {
      lines.map(line -> line.split(",", 2)).distinct().forEach(movie ->
        writeln(writer, "2" + movie[0] + "," + movie[1] + ",false")
      );
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the movies.", e);
    }
  }

  private static void createRatings(Path rawDataPath, Path data) {
    Path path = rawDataPath.resolve("ratings.csv");

    try {
      long rows = Files.lines(path).count();
      long testSize = rows / 5;
      long trainingSize = rows - testSize;

      Stream<String> training = Files.lines(path).skip(1).limit(trainingSize);
      Stream<String> test = Files.lines(path).skip(1 + trainingSize);

      createRatings(training, data.resolve("ratings-training.csv"));
      createRatings(test, data.resolve("ratings-test.csv"));
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

  private static Path createOutputFile(Path path) {
    File file = path.toFile();

    if (!file.exists()) {
      try {
        if (!file.createNewFile()) {
          throw new RuntimeException("Unable to create users file.");
        }
      } catch (IOException e) {
        throw new RuntimeException("Unable to create users file.", e);
      }
    }

    return path;
  }
}
