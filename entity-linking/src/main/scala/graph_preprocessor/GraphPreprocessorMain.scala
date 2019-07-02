/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package graph_preprocessor

import org.slf4j.LoggerFactory
import java.lang.System.currentTimeMillis


object GraphPreprocessorMain {

  private val log = LoggerFactory.getLogger(GraphPreprocessorMain.getClass)

  val DEFAULT_GRAPH_PATH: String = "data/graph_data/graph.json"
  val DEFAULT_OUTPUT_DIR: String = "data/graph_data"

  def main(args: Array[String]): Unit = {

    var graphName: Option[String] = None
    // Path to the input file;
    var inputPath = DEFAULT_GRAPH_PATH
    var minError = GraphPreprocessor.DEFAULT_MIN_ERROR
    var maxIterationCount = GraphPreprocessor.DEFAULT_MAX_ITERATIONS
    var computeSalience = GraphPreprocessor.DEFAULT_COMPUTE_SALIENCE
    var computePageRank = GraphPreprocessor.DEFAULT_COMPUTE_PAGERANK
    var outputDir = DEFAULT_OUTPUT_DIR

    log.info("Starting graph preprocessing... ")

    parser.parse(args, Config()) match {
      case Some(config) =>
        graphName = config.graphName
        inputPath = config.inputPath
        minError = config.minError
        maxIterationCount = config.maxIterationCount
        computeSalience = config.computeSalience
        computePageRank = config.computePageRank
        outputDir = config.outputDir
      case None =>
        log.error("Invalid input parameters: {}", args)
        log.error("ENDING PREPROCESSING")
        return
    }

    log.info("Setting up graph preprocessor...")
    log.info("\tInput graph: {}", inputPath)

    var startTime = currentTimeMillis

    // Create a graph preprocessor;
    val preprocessor = new GraphPreprocessor(
      graphName = graphName,
      inputPath = inputPath,
      minError = minError,
      maxIterationCount = maxIterationCount,
      computeSalience = computeSalience,
      computePageRank = computePageRank,
      outputDir = outputDir
    )
    log.info("")
    log.info("Graph preprocessor, setup time: {} sec", (currentTimeMillis - startTime) / 1000)
    // Perform preprocessing;
    startTime = currentTimeMillis
    preprocessor.preprocess()
    log.info("")
    log.info("Preprocessing completed, exec. time: {} sec", (currentTimeMillis - startTime) / 1000)
  }

  /////////////////////////////////////
  /////////////////////////////////////

  // Configuration of the input parameters;
  case class Config(graphName: Option[String] = None,
                    inputPath: String = DEFAULT_GRAPH_PATH,
                    minError: Double = GraphPreprocessor.DEFAULT_MIN_ERROR,
                    maxIterationCount: Int =  GraphPreprocessor.DEFAULT_MAX_ITERATIONS,
                    computeSalience: Boolean = GraphPreprocessor.DEFAULT_COMPUTE_SALIENCE,
                    computePageRank: Boolean = GraphPreprocessor.DEFAULT_COMPUTE_PAGERANK,
                    outputDir: String = DEFAULT_OUTPUT_DIR
                   )

  // Define a command line args parse to handle the input file
  // and other parameters;
  val parser: scopt.OptionParser[Config] = new scopt.OptionParser[Config]("graph-preprocessor-main") {
    head("graph-preprocessor-main", "0.1")

    opt[String]('g', "graph_name").valueName("name-of-graph")
      .action((x, c) => c.copy(graphName = Some(x))).text("name of the graph")

    opt[String]('i', "input").valueName("path/to/graph")
      .action((x, c) => c.copy(inputPath = x)).text("path to the graph configuration file")
      .withFallback(() => DEFAULT_GRAPH_PATH)

    opt[Double]('e', "min-error")
      .action((x, c) => c.copy(minError = x)).text("tolerance value used to establish convergence")
      .withFallback(() => GraphPreprocessor.DEFAULT_MIN_ERROR)
      .validate( x =>
        if (x >= 0f) success
        else failure("value must be >= 0"))

    opt[Int]('n', "max-iterations")
      .action((x, c) => c.copy(maxIterationCount = x)).text("maximum number of iterations ot be performed")
      .withFallback(() => GraphPreprocessor.DEFAULT_MAX_ITERATIONS)
      .validate( x =>
        if (x > 0) success
        else failure("value must be > 0"))

    opt[Unit]('s', "compute-salience")
      .action((_, c) => c.copy(computeSalience = true)).text("if present, compute the salience rankings of vertices")

    opt[Unit]('p', "compute-pagerank")
      .action((_, c) => c.copy(computePageRank = true)).text("if present, compute the pagerank rankings of vertices," +
      " instead of Salience rankings. Ignored if compute-salience is specified")

    opt[String]('o', "output-dir").valueName("path/to/output/dir")
      .action((x, c) => c.copy(outputDir = x)).text("path to the directory where the preprocessed graph is stored")
      .withFallback(() => DEFAULT_OUTPUT_DIR)
  }
}
