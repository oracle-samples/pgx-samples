/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package graph_preprocessor

import java.io.{BufferedReader, File, InputStream, InputStreamReader}
import java.lang.System.currentTimeMillis
import java.nio.file.Paths

import scala.collection.JavaConverters._
import oracle.pgx.api.filter.{EdgeFilter, VertexFilter}
import oracle.pgx.api._
import oracle.pgx.api.internal.AnalysisResult
import oracle.pgx.common.types.PropertyType
import oracle.pgx.config.{FileGraphConfig, Format}
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import utils.PgxManager

import scala.collection.mutable


class GraphPreprocessor(graphName: Option[String] = None,
                        inputPath: String,
                        minError: Double = GraphPreprocessor.DEFAULT_MIN_ERROR,
                        maxIterationCount: Int = GraphPreprocessor.DEFAULT_MAX_ITERATIONS,
                        computeSalience: Boolean = GraphPreprocessor.DEFAULT_COMPUTE_SALIENCE,
                        computePageRank: Boolean = GraphPreprocessor.DEFAULT_COMPUTE_PAGERANK,
                        outputDir: String) {

  private val log = LoggerFactory.getLogger(GraphPreprocessor.getClass)

  // Create a PgxManager to handle the graph;
  val mgr = new PgxManager(graphName = graphName, inputGraphPath = Some(inputPath))

  // Compile GreenMarl code;
  var salienceRankingGM: Option[CompiledProgram] = None
  var pagerankRankingGM: Option[CompiledProgram] = None

  if (this.computeSalience) try { // Compile the GreenMarl code;
    val srcSalience: InputStream = classOf[GraphPreprocessor].getResourceAsStream("/gm/salience_ranking.gm")
    salienceRankingGM = Some(mgr.session.compileProgram(srcSalience))
  } catch {
    case e: Exception => throw new RuntimeException(e)
  } else if (this.computePageRank) try { // Compile the GreenMarl code;
    val srcPageRank: InputStream = classOf[GraphPreprocessor].getResourceAsStream("/gm/pagerank_u.gm")
    pagerankRankingGM = Some(mgr.session.compileProgram(srcPageRank))
  } catch {
    case e: Exception => throw new RuntimeException(e)
  }

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Preprocess a KB graph: separates redirect/disambiguation edges from the other edge,
    * and compute entropy & salience properties.
    *
    * This method processes the graph on which the GraphPreprocessor was defined, and stores:
    * - A graph with redirects and disambiguations edges (graphname_redirects), and salience rankings
    * - The original graph with pre-computed entropy and salience properties (graphname_preprocessed)
    *
    */
  def preprocess(): Unit = {

    val startOverall = currentTimeMillis

    if (mgr.inputGraph.isEmpty) {
      throw new IllegalStateException("Input graph in GraphPreprocessor is not defined!")
    }
    val graphOriginal = mgr.inputGraph.get

    // Create two subgraphs by filtering redirects and disambiguations;
    log.info("Preprocessing: {}", graphOriginal)

    log.info("\tBuilding disambiguation graph...")
    var start = currentTimeMillis
    val disambiguationGraph = GraphPreprocessor.extractDisambiguationGraph(graphOriginal)
    log.info("\tDisambiguation graph: {}, exec. time: {} sec", disambiguationGraph, (currentTimeMillis - start) / 1000)

    // Compute Salience or PageRank;
    if (this.computeSalience && this.salienceRankingGM.isDefined) {

      log.info("\tComputing entropies...")
      start = currentTimeMillis
      GraphPreprocessor.computeEntropies(graphOriginal)
      log.info("\tEntropy computation done, exec. time: {} sec", (currentTimeMillis - start) / 1000)

      log.info("Computing Salience...")
      start = currentTimeMillis
      val numIterations = computeSalienceScores(graphOriginal, this.minError, this.maxIterationCount)
      log.info(f"\tSalience computation done, number of iterations: {}, exec. time: {} sec",
        numIterations, (currentTimeMillis - start) / 1000)

    } else if (this.computePageRank) {

      log.info("Computing PageRank...")
      start = currentTimeMillis
      val numIterations = computePageRankScores(graphOriginal, this.minError, this.maxIterationCount)
      log.info(f"\tPageRank computation done, number of iterations: {}, exec. time: {} sec",
        numIterations, (currentTimeMillis - start) / 1000)

    }
    else log.info("Skipping Salience/PageRank computation")

    // Copy salience values to the disambiguation graph;
    GraphPreprocessor.copySalience(graphOriginal, disambiguationGraph)

    // Write graph to disk;
    writeGraph(disambiguationGraph, f"${mgr.graphName.getOrElse(mgr.inputGraph.get.getName)}_disambiguation")
    disambiguationGraph.destroy()
    writeGraph(graphOriginal, f"${mgr.graphName.getOrElse(mgr.inputGraph.get.getName)}_property")
    graphOriginal.destroy()

    log.info("Finished preprocessing, exec. time {} sec", (currentTimeMillis - startOverall) / 1000)
  }

  /////////////////////////////////////
  /////////////////////////////////////
  /**
    * Compute the salience rankings of a graph.
    *
    * @param g                 the graph on which the salience rankings are computed
    * @param minError          value used to assess the convergence of the algorithm.
    *                          Stop if the difference from the last iteration is less than this value
    * @param maxIterationCount maximum number of iterations to perform
    * @return the error between the last two iterations
    */
  protected def computeSalienceScores(g: PgxGraph, minError: Double, maxIterationCount: Int): Int = {

    // Check if the "salience" property already exists: if it doesn't, create it,
    // otherwise set it to an arbitrary value;
    if (!g.getVertexProperties.contains(PgxManager.SALIENCE)) {
      g.createVertexProperty(PropertyType.DOUBLE, PgxManager.SALIENCE)
    } else{
      g.getVertexProperty(PgxManager.SALIENCE).fill(0d)
    }

    // Compute the salience ranking of each vertex;
    val res: AnalysisResult[Integer] = this.salienceRankingGM.get.run(
      g,
      minError.asInstanceOf[java.lang.Double],
      maxIterationCount.asInstanceOf[java.lang.Integer],
      GraphPreprocessor.DEFAULT_DAMPING_FACTOR.asInstanceOf[java.lang.Double],
      g.getEdgeProperty(PgxManager.ENTROPY),
      g.getVertexProperty(PgxManager.SALIENCE)
    )
    res.getReturnValue.toInt
  }

  /////////////////////////////////////
  /////////////////////////////////////
  /**
    * Compute the PageRank scores of a graph.
    *
    * @param g                 the graph on which the PageRank scores are computed
    * @param minError          value used to assess the convergence of the algorithm.
    *                          Stop if the difference from the last iteration is less than this value
    * @param maxIterationCount maximum number of iterations to perform
    * @return the error between the last two iterations
    */
  protected def computePageRankScores(g: PgxGraph, minError: Double, maxIterationCount: Int): Int = {

    // Check if the "salience" property already exists: if it doesn't, create it,
    // otherwise set it to an arbitrary value;
    if (!g.getVertexProperties.contains(PgxManager.SALIENCE)) {
      g.createVertexProperty(PropertyType.DOUBLE, PgxManager.SALIENCE)
    } else{
      g.getVertexProperty(PgxManager.SALIENCE).fill(0d)
    }

    // Compute the salience ranking of each vertex;
    val res: AnalysisResult[Integer] = this.pagerankRankingGM.get.run(
      g,
      minError.asInstanceOf[java.lang.Double],
      maxIterationCount.asInstanceOf[java.lang.Integer],
      GraphPreprocessor.DEFAULT_DAMPING_FACTOR.asInstanceOf[java.lang.Double],
      g.getVertexProperty(PgxManager.SALIENCE)
    )
    res.getReturnValue.toInt
  }

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Store the given graph to disk, as edgelist.
    *
    * @param graph  a graph to store
    * @param graphName  the name of the graph
    */
  def writeGraph(graph: PgxGraph, graphName: String): Unit = {
    try {
      val pathEdgeList = Paths.get(outputDir, f"$graphName.edgelist").toString
      val pathConfig = Paths.get(outputDir, f"$graphName.json").toString
      log.info(s"\t\tWriting files: $pathEdgeList; $pathConfig")

      val config = graph.store(Format.EDGE_LIST, pathEdgeList, true)
      // Fix the graph path, by default it is used the path relative to the execution directory,
      // but the path has to be the same where the configuration file is stored;
      val graphPath = config.getVertexUris.get(0)
      config.getValues.put(FileGraphConfig.Field.VERTEX_URIS, Array[String](new File(graphPath).getName))
      FileUtils.write(new File(pathConfig), config.toString, "UTF-8")
      log.info(f"\t\tWritten files: $pathEdgeList; $pathConfig")
    } catch {
      case e: Exception =>
        log.error(f"ERROR IN WRITING FILES $outputDir/$graphName")
        throw new RuntimeException(e)
    }
  }
}

object GraphPreprocessor {

  private val log = LoggerFactory.getLogger(GraphPreprocessor.getClass)

  val DEFAULT_MIN_ERROR: Double = 0d
  val DEFAULT_MAX_ITERATIONS: Int = 1000
  val DEFAULT_COMPUTE_SALIENCE: Boolean = false
  val DEFAULT_COMPUTE_PAGERANK: Boolean = true
  val DEFAULT_DAMPING_FACTOR: Double = 0.95d

  val DEFAULT_REDIRECT = "wikiPageRedirects"
  val DEFAULT_DISAMBIGUATE = "wikiPageDisambiguates"

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Create a subgraph that removes all the edges with a name different from wikiPageRedirects or wikiPageDisambiguates;
    * Vertices with no edges are kept.
    *
    * @param graphOriginal a graph from which the edges are filtered
    * @return a filtered graph
    */
  protected def extractDisambiguationGraph(graphOriginal: PgxGraph): PgxGraph = {

    // Obtain a graph that contains only the "wikiPageRedirects" redirects;
    val zeroDegree = new VertexFilter("vertex.outDegree() == 0 && vertex.inDegree() == 0")
    val edgeRedirect = new EdgeFilter(f"edge.label() == '$DEFAULT_REDIRECT'")
    val edgeDisambiguate = new EdgeFilter(f"edge.label() == '$DEFAULT_DISAMBIGUATE'")

    log.info("\t\tAdding edges to disambiguation graph...")
    val graphRedirects = graphOriginal.filter(edgeRedirect.union(edgeDisambiguate).union(zeroDegree))
    // Add all the remaining vertices;
    val missingVerticesNumber = graphOriginal.getNumVertices - graphRedirects.getNumVertices

    log.info("\t\tAdding {} remaining vertices...", missingVerticesNumber)
    val changeSet = graphRedirects.createChangeSet
    val vertexSet = graphOriginal.getVertices

    // Add missing vertices to the changeset;
    vertexSet.forEach(missingVertex => {
      if (!graphRedirects.hasVertex(missingVertex.getId)) changeSet.addVertex(missingVertex.getId)
    })

    val graphRedirectsFixed: PgxGraph = changeSet.build
    graphRedirects.close()
    graphRedirectsFixed
  }

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Compute the relation entropy for each relation in the graph,
    * and stores the results in the graph.
    *
    * @param g the graph on which entropies are computed
    */
  protected def computeEntropies(g: PgxGraph): Unit = {

    // Check if the "entropy" property already exists: if it doesn't, create it,
    // otherwise set it to an arbitrary value;
    if (!g.getEdgeProperties.contains(PgxManager.ENTROPY)) {
      g.createEdgeProperty(PropertyType.DOUBLE, PgxManager.ENTROPY)
    } else {
      g.getEdgeProperty(PgxManager.ENTROPY).fill(0d)
    }

    // Obtain the set of edge properties;
    val propertySet: PgqlResultSet = g.queryPgql("SELECT label(e) MATCH ()-[e]->(d) GROUP BY label(e)")
    log.info(f"\tGraph ${g.getName} contains ${propertySet.getNumResults} edge properties")

    var start = currentTimeMillis

    // For each edge property, compute the entropy;
    val entropyMap = propertySet.asScala
      .map(_.getString(1))
      .zipWithIndex
      .map { case (relationName: String, i: Int) =>
        try {
          // Compute the occurrences of each destination of the current relation;
          val countStatement: PgxPreparedStatement = g.preparePgql ("SELECT count(d) MATCH ()-[e]->(d) WHERE label(e) = ? GROUP BY d")
          countStatement.setString (1, relationName)
          val relationCounts: PgqlResultSet = countStatement.executeQuery

          val destinationCounts = mutable.ListBuffer[Int]()
          relationCounts.forEach(res => {
            destinationCounts.append(res.getInteger(1).toInt)
          })
          relationCounts.close()

          // Compute the entropy;
          val entropy = relationEntropy(destinationCounts.toList)
          log.info(f"\t\t${i + 1}/${propertySet.getNumResults}) $relationName " +
            f"has ${relationCounts.getNumResults} different destinations, entropy=$entropy")
          (relationName, entropy)
        } catch {
          case e: Exception =>
            log.error(f"ERROR IN COMPUTING ENTROPY FOR $relationName: $e")
            (relationName, 0d)
        }
      }.toMap.par
    propertySet.close()

    log.info("\tFinished entropy values computation, exec. time {} sec", (currentTimeMillis - start) / 1000)

    // Set entropy values;
    start = currentTimeMillis
    var totEntropy: Double = 0d
    g.getEdges.forEach(e => {
      e.setProperty(PgxManager.ENTROPY, entropyMap(e.getLabel))
      totEntropy += entropyMap(e.getLabel)
    })
    log.info("\tEntropy values stored, exec. time {} sec", (currentTimeMillis - start) / 1000)

    // Normalize entropies so that their sum is 1;
    start = currentTimeMillis
    g.getEdges.forEach(e => {
      e.setProperty(PgxManager.ENTROPY, e.getProperty(PgxManager.ENTROPY).asInstanceOf[Double] / totEntropy)
    })
    log.info("\tEntropy values normalized, exec. time {} sec", (currentTimeMillis - start) / 1000)
  }

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Compute the entropy of the given vector of occurrences;
    *
    * @param occurrencesList list of occurrences of each destination vertex
    * @return the entropy of the list of edges
    */
  private def relationEntropy(occurrencesList: List[Int]): Double = {
    val occSum = occurrencesList.sum
    -1 * occurrencesList.map(v => {
      val p = v.toDouble / occSum
      if (p == 0) 0 else p * Math.log(p)
    }).sum
  }

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Copy values of salience from the vertices of a [[PgxGraph]] g1 to another graph g2.
    * It is assumed that g2 contains all the vertices of g1
    *
    * @param g1 a graph
    * @param g2 another graph
    * @param propertyName name of the property which will be stored as "salience".
    *                     Used if the property was computed with another name, e.g. "pagerank"
    */
  def copySalience(g1: PgxGraph, g2: PgxGraph, propertyName: String = PgxManager.SALIENCE): Unit = {

    // Create salience property;
    if (!g2.getVertexProperties.contains(propertyName)) {
      g2.createVertexProperty(PropertyType.DOUBLE, PgxManager.SALIENCE)
    } else{
      g2.getVertexProperty(PgxManager.SALIENCE).fill(0d)
    }

    g1.getVertices.forEach(v => {
      if (g2.hasVertex(v.getId)) {
        g2.getVertex(v.getId).setProperty(PgxManager.SALIENCE, v.getProperty(propertyName))
      } else {
        log.error("Vertex $v is missing from the disambiguation graph")
      }
    })
  }
}
