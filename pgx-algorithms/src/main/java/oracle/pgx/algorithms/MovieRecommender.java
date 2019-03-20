/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package oracle.pgx.algorithms;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import oracle.pgx.api.CompiledProgram;
import oracle.pgx.api.EdgeProperty;
import oracle.pgx.api.EdgeSet;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgx.api.Pgx;
import oracle.pgx.api.PgxEdge;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxSession;
import oracle.pgx.api.PgxVertex;
import oracle.pgx.api.VertexProperty;
import oracle.pgx.api.VertexSet;
import oracle.pgx.api.filter.ResultSetEdgeFilter;
import oracle.pgx.api.filter.ResultSetVertexFilter;
import oracle.pgx.api.internal.AnalysisResult;
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
  public static final double LAMBDA = 0.15;
  public static final double LEARNING_RATE = 0.001;
  public static final double CHANGE_PER_STEP = 1.0;
  public static final int MAX_STEP = 100;

  public static void main(String[] args) throws Exception {
    Path inputDir = Paths.get(args[0]);
    boolean generateRecommendations = false;
    int uid = 1;
    int topk = 10;

    if (args.length == 3) {
      uid = Integer.parseInt(args[1]);
      topk = Integer.parseInt(args[2]);
      generateRecommendations = true;
    }

    if (!Files.exists(inputDir)) {
      throw new IllegalArgumentException("The argument path '" + inputDir + "' does not exist.");
    }

    try (PgxSession session = Pgx.createSession("pgx-algorithm-session")) {
      logger.info("SETUP\n");
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
      logger.info("Loaded test graph {}\n", testGraph);
      VertexProperty<Object, Boolean> is_left = trainingGraph.getVertexProperty("is_left");
      EdgeProperty<Double> rating = trainingGraph.getEdgeProperty("rating");
      VertexProperty<Object, Vect<Double>> features = trainingGraph.createVertexProperty(DOUBLE, VECTOR_LENGTH,
          "features", false);
      Map<Integer, String> movieName = createAuxiliarMap(inputDir, 1);
      Map<Integer, String> movieCategory = createAuxiliarMap(inputDir, 2);

      logger.info("TRAINING\n");
      AnalysisResult result = program.run(trainingGraph, is_left, rating, LEARNING_RATE, CHANGE_PER_STEP, LAMBDA,
          MAX_STEP, VECTOR_LENGTH, features);
      logger.info("RMSE on the TRAINING graph = {}", result.getReturnValue());
      logger.info("Time for running Matrix Factorization Gradient Descent: {} secs\n",
          result.getExecutionTimeMs() / 1000.0);
      logger.info("TESTING\n");

      // Predict rating for edges in the test set
      testEdges.forEach(edge -> {
        Vect<Double> userFeatures = features.get(edge.getSource());
        Vect<Double> movieFeatures = features.get(edge.getDestination());

        double normalizedRating = dotProduct(userFeatures, movieFeatures);
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

      double meanSquaredError = sumSquaredError / length;
      double rootMeanSquaredError = Math.sqrt(meanSquaredError);

      logger.info("RMSE on the TESTING graph = {}", rootMeanSquaredError);
      logger.info("sumSquaredError = {}\tlength = {}\n", sumSquaredError, length);

      if (generateRecommendations) {
        logger.info("MOVIE RECOMMENDATIONS!");

        // Movie topK ranked and recommendations
        String uid_str = "1" + Integer.toString(uid);
        uid = Integer.parseInt(uid_str.substring(1));

        PgxVertex userVertex = trainingGraph.getVertex(Integer.parseInt(uid_str));
        int moviesSeen = 0;

        PgqlResultSet resultSet = trainingGraph.queryPgql("SELECT e MATCH (u)-[e]->() WHERE ID(u) = " + uid_str);
        EdgeSet ratingsTrainSet = trainingGraph.getEdges(new ResultSetEdgeFilter(resultSet, "e"));
        moviesSeen += resultSet.getNumResults();

        resultSet = testGraph.queryPgql("SELECT e MATCH (u)-[e]->() WHERE ID(u) =  " + uid_str + "");
        EdgeSet ratingsTestSet = trainingGraph.getEdges(new ResultSetEdgeFilter(resultSet, "e"));
        moviesSeen += resultSet.getNumResults();

        resultSet = trainingGraph.queryPgql("SELECT x MATCH (u)->(x) WHERE ID(u) !=  " + uid_str + "");
        VertexSet trainSet = trainingGraph.getVertices(new ResultSetVertexFilter(resultSet, "x"));

        resultSet = testGraph.queryPgql("SELECT x MATCH (u)->(x) WHERE ID(u) !=  " + uid_str + "");
        VertexSet testSet = trainingGraph.getVertices(new ResultSetVertexFilter(resultSet, "x"));

        int userId = (Integer) userVertex.getId();
        String idx_str2 = Integer.toString(userId);
        userId = Integer.parseInt(idx_str2.substring(1));
        Vect<Double> userIdFeatures = features.get(userVertex);

        Map<Integer, double[]> mapScores = new HashMap<Integer, double[]>();
        mapScores.put(-1, new double[] {-1});
        mapScores = topRankedMovies(topk, userIdFeatures, ratingsTrainSet, features, rating, mapScores);
        mapScores = topRankedMovies(topk, userIdFeatures, ratingsTestSet, features, rating, mapScores);
        mapScores.remove(-1);

        Map<Integer, double[]> predScores = new HashMap<Integer, double[]>();
        predScores.put(-1, new double[] {-1});
        predScores = topRecommendedMovies(topk, userIdFeatures, trainSet, features, predScores, mapScores);
        predScores = topRecommendedMovies(topk, userIdFeatures, testSet, features, predScores, mapScores);
        predScores.remove(-1);

        logger.info("Selected user ID: {}, movies seen: {}\n", userId, moviesSeen);
        logger.info("Top {} rated by the user\n", topk);
        for (int key : mapScores.keySet()) {
          logger.info("{}",movieName.get(key));
          logger.info("ID: {} score: {}\t(computed: {})", key, mapScores.get(key)[0], mapScores.get(key)[1]);
          logger.info("{}\n", movieCategory.get(key));
        }

        logger.info("Top {} recommendations\n", topk);
        for (int key : predScores.keySet()) {
          logger.info("{}", movieName.get(key));
          logger.info("ID: {}, predicted score: {}", key, predScores.get(key)[0]);
          logger.info("{}\n", movieCategory.get(key));
        }
      }
    }
  }

  private static Map<Integer, double[]> topRecommendedMovies(int topk, Vect<Double> userIdFeatures, VertexSet moviesSet,
      VertexProperty<Object, Vect<Double>> features, Map<Integer, double[]> predScores,
      Map<Integer, double[]> mapScores) {

    double minScore = predScores.get(-1)[0];
    predScores.remove(-1);
    Map<Integer, Double> auxScores = new HashMap<Integer, Double> ();
    for(int key : predScores.keySet()) {
      auxScores.put(key, predScores.get(key)[1]);
    }

    Iterator<PgxVertex> iter = moviesSet.iterator();

    while (iter.hasNext()) {

      PgxVertex movieVertex = iter.next();
      Vect<Double> movieFeatures = features.get(movieVertex);

      int idx = (Integer) movieVertex.getId();
      String idx_str = Integer.toString(idx);
      idx = Integer.parseInt(idx_str.substring(1));

      if (auxScores.get(idx) == null) {

        double r = dotProduct(userIdFeatures, movieFeatures);
        double e = 0;
        double roundedScore = 5;
        if (r < 5) {
          roundedScore = 0.5 * Math.round(r / 0.5);
          e = min(Math.abs(r - roundedScore), Math.abs(r - roundedScore - 0.5));
        } else {
          e = Math.abs(r - 5);
        }

        if (mapScores.get(idx) == null && roundedScore - e > minScore) {
          if (auxScores.size() == topk) {
            int minKey = getMinKey(auxScores);
            minScore = auxScores.get(minKey);
            auxScores.remove(minKey);
            predScores.remove(minKey);

            minKey = getMinKey(auxScores);
            minScore = auxScores.get(minKey);
          }
          double[] vScores = new double[] {r, roundedScore - e};
          auxScores.put(idx, roundedScore - e);
          predScores.put(idx, vScores);
        }
      }
    }

    predScores.put(-1, new double[] {minScore});
    return predScores;
  }

  private static Map<Integer, double[]> topRankedMovies(int topk, Vect<Double> userIdFeatures, EdgeSet moviesWatched,
      VertexProperty<Object, Vect<Double>> features, EdgeProperty<Double> rating, Map<Integer, double[]> mapScores) {

    double minScore = mapScores.get(-1)[0];
    mapScores.remove(-1);
    Map<Integer, Double> auxScores = new HashMap<Integer, Double> ();
    for(int key : mapScores.keySet()) {
      auxScores.put(key, mapScores.get(key)[2]);
    }

    for(PgxEdge edge : moviesWatched) {

      PgxVertex movieVertex = edge.getDestination();
      Vect<Double> movieFeatures = features.get(movieVertex);

      int idx = (Integer) movieVertex.getId();
      String idx_str = Integer.toString(idx);
      idx = Integer.parseInt(idx_str.substring(1));

      double realRating = rating.get(edge);
      double r = dotProduct(userIdFeatures, movieFeatures);
      double e = Math.abs(realRating - r);

      if (realRating - e > minScore) {
        if (auxScores.size() == topk) {
          int minKey = getMinKey(auxScores);
          minScore = auxScores.get(minKey);
          auxScores.remove(minKey);
          mapScores.remove(minKey);

          minKey = getMinKey(auxScores);
          minScore = auxScores.get(minKey);
        }
        double[] vScores = new double[] {realRating, r, realRating - e};
        auxScores.put(idx, realRating - e);
        mapScores.put(idx, vScores);
      }
    }

    mapScores.put(-1, new double[] {minScore});
    return mapScores;
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

  private static int getMinKey(Map<Integer, Double> bestScores) {
    int minKey = 0;
    double min = Double.POSITIVE_INFINITY;

    for (int key : bestScores.keySet()) {
      double score = bestScores.get(key);
      if (min > score) {
        min = score;
        minKey = key;
      }
    }
    return minKey;
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

  private static Map<Integer, String> createAuxiliarMap(Path inputDir, int columnId) {
    Path movieInfo = inputDir.resolve("movies.csv");

    try {
      return Files.lines(movieInfo)
          .skip(1)
          .map(str -> str.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
          .collect(Collectors.toMap(column -> Integer.parseInt(column[0]), column -> column[columnId]));
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the movies.", e);
    }
  }
}
