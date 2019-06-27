package candidate_finder

import java.nio.file.{Files, Paths}

import net.liftweb.json.{Formats, NoTypeHints}
import net.liftweb.json.Serialization.write
import net.liftweb.json._
import org.slf4j.LoggerFactory
import spark.{Request, Response}
import spark.Spark._

import scala.io.Source

object CandidateFinderMain {

  private val log = LoggerFactory.getLogger(CandidateFinderMain.getClass)

  val DEFAULT_GRAPH_PATH: String = "data/graph_data/graph_disambiguation.json"

  // Number of lines to read in an input file;
  var maxDocLinesToRead: Int = -1

  // If true optimize the index before querying, and reoptimize at the end with default values;
  var optimizeIndex: Boolean = false

  def main(args: Array[String]): Unit = {

    // Path to the input file;
    var inputPath = DEFAULT_GRAPH_PATH

    log.info("Starting candidate finder... ")

    parser.parse(args, Config()) match {
      case Some(config) =>
        inputPath = config.inputPath
      case None =>
        log.error("Invalid input parameters: {}", args)
        log.error("ENDING SERVER")
        return
    }

    log.info("Setting up candidate finder...")
    log.info("\tInput file: {}", inputPath)

    var startTime = System.currentTimeMillis

    // Create a candidate finder;
    val candidateFinder = new CandidateFinder(inputPath)
    log.info("\nCandidate finder ready, setup time: %1d ms".format(System.currentTimeMillis - startTime))

    startTime = System.currentTimeMillis
    // Manage input queries;
    manageGetRequest(candidateFinder)

    log.info("\nExecution Time: %1d ms".format(System.currentTimeMillis - startTime))
  }

  /////////////////////////////////////
  /////////////////////////////////////

  // Configuration of the input parameters;
  case class Config(inputPath: String = CandidateFinderMain.DEFAULT_GRAPH_PATH
                   )

  // Define a command line args parse to handle the input file
  // and other parameters;
  val parser: scopt.OptionParser[Config] = new scopt.OptionParser[Config]("candidates-finder-main") {
    head("candidates-finder-main", "0.1")

    opt[String]('i', "input").valueName("path/to/graph")
      .action((x, c) => c.copy(inputPath = x)).text("path to the graph configuration file")
      .withFallback(() => DEFAULT_GRAPH_PATH)
  }

  /////////////////////////////////////
  /////////////////////////////////////

  // Handle GET requests;
  def manageGetRequest(candidateFinder: CandidateFinder): Unit = {
    get("/find-candidates", (request: Request, response: Response) => {
      response.`type`("application/json")
      // Parse the configuration parameters;
      parseGetConfigParams(request, candidateFinder)

      // Parse the names to query and find the candidates,
      // or process an input document;
      parseNamesParams(request, candidateFinder)
    })
    // Restore default parameter values;
    after("/*", (request: Request, response: Response) => {
      candidateFinder.maxMatches = CandidateFinder.DEFAULT_MAX_MATCHES
      candidateFinder.minScore = CandidateFinder.DEFAULT_MIN_SCORE

      this.maxDocLinesToRead = -1
    })
  }

  def parseGetConfigParams(request: Request, candidateFinder: CandidateFinder): Unit = {
    // Obtain the maximum number of matches to return.
    // Note that internally it is used a higher value,
    // and matches are filtered after reranking;
    try {
      val maxMatches = request.queryParams("max-matches").toInt
      if (maxMatches > 0) {
        candidateFinder.maxMatches = maxMatches
        log.info("Max Matches set to {}", maxMatches)
      }
    } catch {
      case e: Exception =>
        log.error("ERROR SETTING MAX MATCHES, USING DEFAULT")
        candidateFinder.maxMatches = CandidateFinder.DEFAULT_MAX_MATCHES
    }

    // Set the minimum score. High values give faster search, but potentially worse results;
    try {
      val minScore = request.queryParams("min-score").toFloat
      if (minScore >= 0f && minScore <= 1) {
        candidateFinder.minScore = minScore
        log.info("Minimum score set to {}", minScore)
      }
    } catch {
      case e: Exception =>
        log.error("ERROR SETTING MINIMUM SCORE, USING DEFAULT")
        candidateFinder.minScore = CandidateFinder.DEFAULT_MIN_SCORE
    }

    // Perform index optimization before querying;
    try {
      optimizeIndex = request.queryParams("optimize-index").toBoolean
    } catch {
      case e: Exception => optimizeIndex = false
    }

    // Read the maximum number of lines to read in the input file;
    try {
      val maxDocLinesToRead = request.queryParams("max-lines").toInt
      if (maxDocLinesToRead > 0f) {
        this.maxDocLinesToRead = maxDocLinesToRead
        log.info("Input Weight set to {}", maxDocLinesToRead)
      }
    } catch {
      case e: Exception =>
        log.error("ERROR SETTING MAX LINES TO READ")
        this.maxDocLinesToRead = -1
    }
  }

  def parseNamesParams(request: Request, candidateFinder: CandidateFinder): String = {
    // Obtain a list of names to query;
    val names = Option[Array[String]](request.queryParamsValues("name"))
    // Check if a path to a file is provided, instead;
    val path = Option[String](request.queryParams("file"))

    var outputMessage = ""

    // Process a list of names;
    if (names.isDefined) {
      val queryResults: Seq[CandidateFinderResult] = names.get.toList.map(name =>
        candidateFinder.findCandidates(name.replaceAll("^\"|\"$", "")))
      // Turn the response to JSON;
      implicit val formats: Formats = Serialization.formats(NoTypeHints)
      outputMessage = write(queryResults)

      //Process a file;
    } else if (path.isDefined) {
      // Strip quotes;
      val pathProcessed = path.get.replaceAll("^\"|\"$", "")
      if (Files.exists(Paths.get(pathProcessed))) {

        val startTime = System.currentTimeMillis

        // Process the file;
        val bufferedSource = Source.fromFile(pathProcessed)
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
        val documentResult: Seq[CandidateFinderOutDoc] = documentList.par.map(document => {
          val queryResults = document.mentions.map(mention =>
            candidateFinder.findCandidates(mention.mentionText, golden = mention.golden.getOrElse("")))
          CandidateFinderOutDoc(
            id = document.id,
            text = document.text,
            candidateFinderResults = queryResults
          )
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
    "{\"message\":\"404, Name not provided\"}"
  })
}





