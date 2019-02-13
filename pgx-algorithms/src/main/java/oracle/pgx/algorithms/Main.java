/*
 * Copyright (C) 2019 Oracle and/or its affiliates. All rights reserved.
 */

package oracle.pgx.algorithms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import oracle.pgx.api.CompiledProgram;
import oracle.pgx.api.EdgeProperty;
import oracle.pgx.api.Pgx;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxSession;
import oracle.pgx.api.PgxVertex;
import oracle.pgx.api.VertexProperty;
import oracle.pgx.common.types.PropertyType;
import oracle.pgx.common.util.vector.Vect;
import oracle.pgx.config.Format;
import oracle.pgx.config.GraphConfig;
import oracle.pgx.config.GraphConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  public static Logger logger = LoggerFactory.getLogger(Main.class);
  public static final int VECTOR_LENGTH = 20;
  public static final double LEARNING_RATE = 0.32;
  public static final double CHANGE_PER_STEP = 0.85;
  public static final double LAMBDA = 0.25;
  public static final int MAX_STEP = 100;

  public static void main(String[] args) throws Exception {
    Path rawDataPath = Paths.get(args[0]);

    if (!Files.exists(rawDataPath)) {
      throw new IllegalArgumentException("The data path " + rawDataPath + " does not exist.");
    }

    try (PgxSession session = Pgx.createSession("pgx-algorithm-session")) {
      // Compile PGX algorithm
      String code = getResource("MatrixFactorizationGradientDescent.java");
      CompiledProgram program = session.compileProgram(code);

      // Massage raw data
      Path data = prepare(rawDataPath);

      // Load graph
      GraphConfig config = GraphConfigBuilder.forFileFormat(Format.CSV)
          .hasHeader(true)
          .setVertexIdColumn("id")
          .setEdgeSourceColumn("userId")
          .setEdgeDestinationColumn("movieId")
          .addVertexUri(data.resolve("movies.csv").toString())
          .addVertexUri(data.resolve("users.csv").toString())
          .addEdgeUri(data.resolve("ratings.csv").toString())
          .addVertexProperty("name", PropertyType.STRING)
          .addVertexProperty("genre", PropertyType.STRING)
          .addVertexProperty("is_left", PropertyType.BOOLEAN)
          .addEdgeProperty("rating", PropertyType.DOUBLE)
          .addEdgeProperty("timestamp", PropertyType.INTEGER)
          .build();

      PgxGraph graph = session.readGraphWithProperties(config);

      // Run matrix vectorization
      VertexProperty<Object, Boolean> is_left = graph.getVertexProperty("is_left");
      EdgeProperty<Double> weight = graph.getEdgeProperty("rating");
      VertexProperty<Object, Vect<Double>> features = graph.createVertexProperty(PropertyType.DOUBLE, VECTOR_LENGTH, "features", false);
      program.run(graph, is_left, weight, LEARNING_RATE, CHANGE_PER_STEP, LAMBDA, MAX_STEP, VECTOR_LENGTH, features);

      // Show results for first 10.
      int i = 0;
      for (PgxVertex<Object> vertex : graph.getVertices()) {
        System.out.println("Feature vector " + vertex + " = " + Arrays.toString(features.get(vertex).toArray()));
        i++;
        if (i > 10) {
          System.exit(0);
        }
      }

      // TODO: Train model on training set (subgraph?), test model on test set.
    }
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

    try {
      Stream<String> lines = Files.lines(path).skip(1);
      File output = data.resolve("users.csv").toFile();

      if (!output.exists()) {
        if (!output.createNewFile()) {
          throw new RuntimeException("Unable to create users file.");
        }
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
        writer.write("id,name,genre,is_left");
        writer.newLine();

        lines.map(line -> line.split(",")[0]).distinct().forEach(user -> {
          try {
            writer.write("1" + user + ",,,true");
            writer.newLine();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the users.", e);
    }
  }

  private static void createMovies(Path rawDataPath, Path data) {
    Path path = rawDataPath.resolve("movies.csv");

    try {
      Stream<String> lines = Files.lines(path).skip(1);
      File output = data.resolve("movies.csv").toFile();

      if (!output.exists())  {
        if (!output.createNewFile()) {
          throw new RuntimeException("Unable to create movies file.");
        }
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
        writer.write("id,name,genre,is_left");
        writer.newLine();

        lines.map(line -> line.split(",", 2)).distinct().forEach(movie -> {
          try {
            writer.write("2" + movie[0] + "," + movie[1] + ",false");
            writer.newLine();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the movies.", e);
    }
  }

  private static void createRatings(Path rawDataPath, Path data) {
    Path path = rawDataPath.resolve("ratings.csv");

    try {
      Stream<String> lines = Files.lines(path);
      File output = data.resolve("ratings.csv").toFile();

      if (!output.exists()) {
        if (!output.createNewFile()) {
          throw new RuntimeException("Unable to create ratings file.");
        }
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
        writer.write("userId,movieId,rating,timestamp");
        writer.newLine();

        lines.skip(1).forEach(line -> {
          String[] columns = line.split(",");
          try {
            writer.write("1" + columns[0] + ",2" + columns[1] + "," + columns[2] + "," + columns[3]);
            writer.newLine();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the ratings.", e);
    }
  }

  private static String getResource(String name) {
    URL resource = Main.class.getClassLoader().getResource(name);

    if (resource == null) {
      throw new IllegalStateException("Program '" + name + "' not found.");
    }

    return resource.getFile();
  }
}
