/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package utils

import java.io.File

import oracle.pgx.api.{PgxSession, _}
import org.slf4j.LoggerFactory

class PgxManager(
                var graphName: Option[String] = None,
                val inputGraphPath: Option[String] = None,
                val candidateGraphPath: Option[String] = None
                ) extends AutoCloseable {

  private val log = LoggerFactory.getLogger(classOf[PgxManager])
  log.info("Creating Pgx session...")
  val session: PgxSession = Pgx.createSession("session_1")
  log.info("Session created")

  // Read the main input graph, if present;
  var inputGraph: Option[PgxGraph] = None
  if (inputGraphPath.isDefined) {
    log.info("Opening input graph: {}...", inputGraphPath.get)
    val startTime = System.currentTimeMillis
    inputGraph = Some(session.readGraphWithProperties(inputGraphPath.get))
    val inputName: String = inputGraph.get.getName
    log.info(f"Loaded input graph: $inputName, loading time: ${(System.currentTimeMillis - startTime) / 1000} sec")

    // Store the graph name;
    if (graphName.isEmpty) graphName = Some(new File(inputGraph.get.getName).getName.replaceFirst("[.][^.]+$", ""))
  }

  // Read the candidate graph, if present;
  var candidateGraph: Option[PgxGraph] = None
  if (candidateGraphPath.isDefined) {
    log.info("Opening candidate graph: {}...", candidateGraphPath.get)
    val startTime = System.currentTimeMillis
    candidateGraph = Some(session.readGraphWithProperties(candidateGraphPath.get))
    val candidateName: String = candidateGraph.get.getName
    log.info(f"Loaded candidate graph: $candidateName, loading time: ${(System.currentTimeMillis - startTime) / 1000} sec")

    // Store the graph name;
    if (graphName.isEmpty && inputGraph.isEmpty) {
      graphName = Some(new File(candidateGraph.get.getName).getName.replaceFirst("[.][^.]+$", ""))
    }
  }

  @Override
  def close(): Unit = {
    if (inputGraph.isDefined) inputGraph.get.destroy()
    if (candidateGraph.isDefined) candidateGraph.get.destroy()
  }
}

object PgxManager {
  val ENTROPY: String = "entropy"
  val SALIENCE: String = "salience"
  val PAGERANK: String = "pagerank"
  val TYPE: String = "type"
}