/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package embeddings_linker

import java.nio.file.{Files, Paths}

import breeze.linalg.{DenseMatrix, DenseVector}
import candidate_finder.{CandidateFinderInputDoc, Mention}
import net.liftweb.json.Serialization.write
import net.liftweb.json._
import org.slf4j.LoggerFactory
import spark.{Request, Response}
import spark.Spark.{after, get, notFound}

import scala.io.Source

object EmbeddingsLinkerMain {

  private val log = LoggerFactory.getLogger(EmbeddingsLinkerMain.getClass)

  // Default path to the DBPedia graph;
  val DEFAULT_GRAPH_PATH: String = "data/graph_data/graph_disambiguation.json"
  // Default path to the vertex embeddings;
  val DEFAULT_EMBEDDINGS_PATH: String = "data/embeddings/dbpedia_100.csv"

  // Number of lines to read in an input file that should be linked;
  var maxDocLinesToRead: Int = -1

  def main(args: Array[String]): Unit = {

    // Path to the input file;
    var graphPath: String = DEFAULT_GRAPH_PATH
    // Path to the embeddings;
    var embeddingsPath: String = DEFAULT_EMBEDDINGS_PATH
    // Number of embedding dimensions;
    var dimensions: Int = -1

    log.info("Starting entity linker... ")

    parser.parse(args, Config()) match {
      case Some(config) =>
        graphPath = config.graphPath
        embeddingsPath = config.embeddingsPath
        dimensions = config.dimensions
      case None =>
        log.error("Invalid input parameters: {}", args)
        log.error("CLOSING SERVER")
        return
    }

    log.info("Setting up entity linker...")
    log.info("\tGraph file: {}", graphPath)
    log.info("\tEmbeddings file: {}", embeddingsPath)

    var startTime = System.currentTimeMillis

    // Create an entity linker;
    val entityLinker = new EmbeddingsLinker(graphPath, embeddingsPath, dimensions = dimensions)
    log.info("\nEntity linker ready, setup time: %1d ms".format(System.currentTimeMillis - startTime))

    startTime = System.currentTimeMillis
    // Manage input queries;
    manageGetRequest(entityLinker)

    log.info("\nExecution Time: %1d ms".format(System.currentTimeMillis - startTime))
  }

  /////////////////////////////////////
  /////////////////////////////////////

  // Configuration of the input parameters;
  case class Config(
                     graphPath: String = EmbeddingsLinkerMain.DEFAULT_GRAPH_PATH,
                     embeddingsPath: String = EmbeddingsLinkerMain.DEFAULT_EMBEDDINGS_PATH,
                     dimensions: Int = -1
                   )

  // Define a command line args parse to handle the input file
  // and other parameters;
  val parser: scopt.OptionParser[Config] = new scopt.OptionParser[Config]("entity-linker-main") {
    head("entity-linker-main", "0.1")

    opt[String]('g', "graph").valueName("path/to/graph")
      .action((x, c) => c.copy(graphPath = x)).text("path to the graph configuration file")
      .withFallback(() => DEFAULT_GRAPH_PATH)

    opt[String]('e', "embeddings").valueName("path/to/embeddings")
      .action((x, c) => c.copy(embeddingsPath = x)).text("path to the vertex embeddings file")
      .withFallback(() => DEFAULT_EMBEDDINGS_PATH)

    opt[Int]('d', "dimensions").valueName("num")
      .action((x, c) => c.copy(dimensions = x)).text("number of embedding dimensions to use")
      .withFallback(() => -1)
  }

  /////////////////////////////////////
  /////////////////////////////////////

  // Handle GET requests;
  def manageGetRequest(entityLinker: EmbeddingsLinker): Unit = {
    get("/link", (request: Request, response: Response) => {
      response.`type`("application/json")
      // Parse the configuration parameters;
      parseConfigParams(request, entityLinker)
      // Parse the mentions to link and,
      // or process an input document;
      parseQueryParams(request, entityLinker)
    })
    // Restore default parameter values;
    after("/*", (_: Request, _: Response) => {
      entityLinker.settings = EmbeddingsLinkerSettings()
      this.maxDocLinesToRead = -1
    })
  }

  def parseConfigParams(request: Request, entityLinker: EmbeddingsLinker): Unit = {

    // Create new runtime settings for the Entity Linker;
    val settings = EmbeddingsLinkerSettings()

    // Obtain the maximum number of candidates to use.
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val numCandidates = request.queryParams("num-candidates").toInt
      if (numCandidates > 0) {
        settings.cfNumCandidates = numCandidates
      }
    }
    // Set the minimum score. High values give faster search, but potentially worse results;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val minScore = request.queryParams("min-score").toFloat
      if (minScore >= 0f && minScore <= 1) {
        settings.cfMinScore = minScore
      }
    }
    // Skip the disambiguation step
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      settings.skipDisambiguation = request.queryParams("skip-disambiguation").toBoolean
    }
    // Add a greedy optimization step to the disambiguation algorithm;s
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      settings.optOptimizeChild = request.queryParams("optimize-child").toBoolean
    }
    // Set the maximum number of steps done by the disambiguation optimizer;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val maxSteps = request.queryParams("max-steps").toInt
      if (maxSteps >= 0) {
        settings.optMaxSteps = maxSteps
      }
    }
    // Set the maximum number of early stopping steps done by the disambiguation optimizer;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val earlyStop = request.queryParams("early-stop").toInt
      if (earlyStop >= 0) {
        settings.optEarlyStop = earlyStop
      }
    }
    // Set the number of children generated by the optimizer at each step;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val numChildren = request.queryParams("num-children").toInt
      if (numChildren > 0) {
        settings.optNumChildren = numChildren
      }
    }

    // Set the alpha parameter, stregth of the candidate finder score in the disambiguation;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val alpha = request.queryParams("alpha").toDouble
      settings.optAlpha = alpha
    }

    // Set the beta parameter, stregth of the salience score in the disambiguation;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val beta = request.queryParams("beta").toDouble
      settings.optBeta = beta
    }

    // Set the alpha exponential parameter, power to which candidate finder scores are raised;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val alphaExp = request.queryParams("alpha-exp").toDouble
      settings.optAlphaExp = alphaExp
    }

    // Set the beta exponential parameter, power to which salience scores are raised;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val betaExp = request.queryParams("beta-exp").toDouble
      settings.optBetaExp = betaExp
    }

    // Set the initial heuristic weight, stregth of the salience score in the heuristic;
    scala.util.control.Exception.ignoring(classOf[Exception]) {
      val initWeight = request.queryParams("init-weight").toDouble
      settings.optInitWeight = initWeight
    }

    // Read the maximum number of documents to read in the input file;
    try {
      val maxDocLinesToRead = request.queryParams("num-docs").toInt
      if (maxDocLinesToRead > 0f) {
        this.maxDocLinesToRead = maxDocLinesToRead
        log.info("Reading {} documents", maxDocLinesToRead)
      }
    } catch {
      case _: Exception =>
        log.info("Reading all input documents")
        this.maxDocLinesToRead = -1
    }

    log.info(f"Using settings: $settings")

    // Use the provided settings;
    entityLinker.settings = settings
  }

  def parseQueryParams(request: Request, entityLinker: EmbeddingsLinker): String = {
    // Obtain a list of names to query;
    val names = Option[Array[String]](request.queryParamsValues("name"))
    // Check if a path to a file is provided, instead;
    val path = Option[String](request.queryParams("file"))

    var outputMessage = ""

    // Process a list of names;
    if (names.isDefined) {
      val queryResult: EntityLinkerOutDoc = entityLinker.link(
        names.get.toList.map(name => Mention(name.replaceAll("^\"|\"$", ""), 0, 0)),
      )
      // Turn the response to JSON;
      implicit val formats: Formats = Serialization.formats(NoTypeHints)
      outputMessage = write(queryResult)

    //Process a file;
    } else if (path.isDefined) {
      // Strip quotes;
      val pathProcessed = path.get.replaceAll("^\"|\"$", "")
      if (Files.exists(Paths.get(pathProcessed))) {

        val startTime = System.currentTimeMillis

        // Process the file;
        val bufferedSource = Source.fromFile(pathProcessed, "UTF-8")
        val fileContent = bufferedSource.getLines().toList.mkString("\n")
        log.info("\tProcessing file: {}", pathProcessed)
        bufferedSource.close

        implicit val formats: Formats = DefaultFormats
        // Obtain each document;
        val childrenDoc = parse(fileContent).children
        var numLines = 0
        if (this.maxDocLinesToRead > 0) numLines = this.maxDocLinesToRead
        else numLines = childrenDoc.size

        val documentList: Seq[CandidateFinderInputDoc] =
          childrenDoc.take(numLines).map(elem => elem.extract[CandidateFinderInputDoc])

        // Find candidates for each document;
        val documentResult: Seq[EntityLinkerOutDoc] = documentList.par.map(document => {
          entityLinker.link(document)
        }).seq

        val execTime = System.currentTimeMillis - startTime
        log.info(f"Processing of file $pathProcessed finished, documents read: $numLines," +
          f" execution time: ${execTime / 1000} sec, average: ${(execTime / 1000) / numLines} sec\n\n")

        outputMessage = write(documentResult)
      } else notFoundMessage()
    } else notFoundMessage()

    // Return the output JSON;
    outputMessage
  }

  def notFoundMessage(): Unit = notFound((req: Request, res: Response) => {
    log.info(f"404, Invalid request: $req")
    res.`type`("application/json")
    "{\"message\":\"404, Query not provided\"}"
  })
}
