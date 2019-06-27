package create_embeddings

import java.lang.System.currentTimeMillis

import oracle.pgx.api.beta.mllib.DeepWalkModel
import oracle.pgx.api.{Analyst, PgxGraph}
import org.slf4j.LoggerFactory
import utils.PgxManager


object CreateEmbeddingsMain {

  private val log = LoggerFactory.getLogger(CreateEmbeddingsMain.getClass)

  // Default settings for DeepWalk;
  val DEFAULT_SIZE = 100
  val DEFAULT_LENGTH = 4
  val DEFAULT_WALK_PER_V = 6
  val DEFAULT_NUM_EPOCH = 1

  // Custom settings for DeepWalk;
  val embeddings_size: Int = 10//200
  val length: Int = 10//30
  val walk_per_v: Int = 4//8
  val num_epoch: Int = 1

  // Default path to the DBPedia graph;
  val DEFAULT_GRAPH_PATH: String = "data/graph_data/graph.json"
  // Default path to the vertex embeddings;
  val DEFAULT_EMBEDDINGS_PATH: String = f"data/embeddings/embeddings_d${embeddings_size}_w${walk_per_v}_l${length}_e${num_epoch}.csv"

  def createModel(analyst: Analyst): DeepWalkModel = {
    analyst.deepWalkModelBuilder().
      setMinWordFrequency(1).
      setBatchSize(512).
      setNumEpochs(num_epoch).
      setLayerSize(embeddings_size).
      setLearningRate(0.05).
      setMinLearningRate(0.0001).
      setWindowSize(3).
      setWalksPerVertex(walk_per_v).
      setWalkLength(length).
      setSampleRate(0.00001).
      setNegativeSample(2).
      setValidationFraction(0.01).
      build()
  }

  def main(args: Array[String]): Unit = {

    log.info("Starting embeddings creation... ")

    var graphName: Option[String] = None
    var inputPath: String = DEFAULT_GRAPH_PATH
    var outputPath: String = DEFAULT_EMBEDDINGS_PATH

    parser.parse(args, Config()) match {
      case Some(config) =>
        graphName = config.graphName
        inputPath = config.inputPath
        outputPath = config.outputPath
      case None =>
        log.error("Invalid input parameters: {}", args)
        log.error("ENDING EMBEDDINGS CREATION")
        return
    }

    log.info("Setting up embeddings creation...")
    log.info("\tInput graph: {}", inputPath)

    // Load the graph. The graph must be undirected to compute the embeddings;
    // Create a PgxManager to handle the graph;
    val mgr = new PgxManager(graphName = graphName, inputGraphPath = Some(inputPath))

    // Create an analyst to create the embeddings;
    val analyst: Analyst = mgr.session.createAnalyst

    // Undirect the graph, as DeepWalk requires undirected graphs;
    log.info("Creating undirected graph...")
    val start = currentTimeMillis
    val graph: PgxGraph = mgr.inputGraph.get.undirect()
    log.info(s"Undirected graph created, exec. time: ${(currentTimeMillis - start) / 1000} sec")

    // Create a DeepWalk model.
    // Parameters might depend on the graph, and are chosen according to these papers:
    //   https://cs.stanford.edu/~jure/pubs/node2vec-kdd16.pdf
    //   https://arxiv.org/pdf/1403.6652.pdf
    val model: DeepWalkModel = createModel(analyst)

    log.info("Started embeddings generation...")
    model.fit(graph)
    log.info("Embeddings generation completed!")
    log.info(s"\tLoss: ${model.getLoss}")

    // Store the embeddings in a csv;
    val vertexVectors = model.getTrainedVertexVectors.flattenAll
    vertexVectors.write.overwrite(true).csv.separator('\t').store(outputPath)
    log.info(f"Embeddings stored in $outputPath")
  }

  /////////////////////////////////////
  /////////////////////////////////////

  // Configuration of the input parameters;
  case class Config(graphName: Option[String] = None,
                    inputPath: String = DEFAULT_GRAPH_PATH,
                    outputPath: String = DEFAULT_EMBEDDINGS_PATH
                   )

  // Define a command line args parse to handle the input file
  // and other parameters;
  val parser: scopt.OptionParser[Config] = new scopt.OptionParser[Config]("create-embeddings-main") {
    head("create-embeddings-main", "0.1")

    opt[String]('g', "graph_name").valueName("name-of-graph")
      .action((x, c) => c.copy(graphName = Some(x))).text("name of the graph")

    opt[String]('i', "input").valueName("path/to/graph")
      .action((x, c) => c.copy(inputPath = x)).text("path to the graph configuration file")
      .withFallback(() => DEFAULT_GRAPH_PATH)

    opt[String]('o', "output").valueName("path/to/output/embeddings")
      .action((x, c) => c.copy(outputPath = x)).text("path to where the embeddings are stored")
      .withFallback(() => DEFAULT_EMBEDDINGS_PATH)
  }
}
