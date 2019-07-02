/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package embeddings_linker

import candidate_finder.{CandidateFinder, CandidateFinderInputDoc, CandidateFinderResult, Mention}
import org.slf4j.LoggerFactory
import java.lang.System.currentTimeMillis

import collection.JavaConverters._
import breeze.linalg._
import embeddings_linker.disambiguation_optimizer.{Optimizer, Problem}

import scala.collection.parallel.immutable.{ParMap, ParSet}
import scala.collection.parallel.mutable.ParArray
import scala.io.Source

class EmbeddingsLinker(
                        val graphPath: String,
                        val embeddingsPath: String,
                        var dimensions: Int = 100
                      ) {
  
  // Create a configuration with default parameter values;
  var settings: EmbeddingsLinkerSettings = EmbeddingsLinkerSettings()

  private val log = LoggerFactory.getLogger(classOf[EmbeddingsLinker])

  // Create a candidate finder which contains the disambiguation graph inside a PgxManager;
  var candidateFinder: CandidateFinder = new CandidateFinder(graphPath)

  // Load the embeddings;
  var embeddings: ParMap[String, DenseVector[Double]] = loadEmbeddings()

  // Fix the embedding dimensionality;
  dimensions = embeddings.head._2.length

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Load the embeddings stored in a CSV file and store them in a map that associates each vertex ID to its embedding
    *
    * @return an embeddings map
    */
  def loadEmbeddings(): ParMap[String, DenseVector[Double]] = {

    log.info(f"Opening embeddings stored at $embeddingsPath...")
    val startTime = currentTimeMillis

    // Preallocate a map with the specified embedding size;
    val embeddings: ParMap[String, DenseVector[Double]] = candidateFinder.mgr.candidateGraph.get
      .getVertices.asScala.par
      .map(v => (v.getId.toString, DenseVector.zeros[Double](dimensions))).toMap

    // Process each line of the embeddings file;
    val bufferedSource = Source.fromFile(embeddingsPath, "UTF-8")
    var numAdded = 0
    for (line <- bufferedSource.getLines) {
      val fields: Seq[String] = line.split("\t").map(_.trim)

      // Extract the vertex name;
      val id = fields.head

      // Store each embedding in the appropriate row in the map.
      // The +- 1 offset is required as the first element of the file row is the vertex name.
      // If the embedding dimension is larger than what is desired by the user, ignore the extra values.
      // If it is smaller keep the 0 padding created in the map allocation;
      val effSize = if (dimensions > 1) Math.min(dimensions, fields.size - 1) else fields.size - 1
      if (embeddings.contains(id)) {
        var curr_embedding = DenseVector.zeros[Double](effSize)
        for (i <- 0 until effSize) {
          curr_embedding(i) = fields(i + 1).toDouble
        }
        // Normalize the embedding;
        curr_embedding = curr_embedding / norm(curr_embedding)
        for (i <- 0 until effSize) {
          embeddings(id)(i) = curr_embedding(i)
        }
        numAdded += 1
      }
      else log.debug(f"\t\tMissing embedding: $id")

      if (numAdded % 100000 == 0) {
        log.info(f"\tItems added: $numAdded, time elapsed: ${(currentTimeMillis - startTime) / 1000}%d sec")
      }
    }
    bufferedSource.close

    val missingCount = candidateFinder.mgr.candidateGraph.get.getNumVertices - numAdded
    if (missingCount > 0) log.warn(f"\tAdded $missingCount missing embeddings")
    log.info(f"\tItems added: ${embeddings.size}, time elapsed: ${(currentTimeMillis - startTime) / 1000}%d sec")

    embeddings
  }

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Given a sequence of mentions, perform entity linking on each of them.
    *
    * @param mentions a sequence of mentions
    * @return for each mention, an entity linker result
    */
  def link(
            mentions: Seq[Mention],
          ): EntityLinkerOutDoc = {

    val startLinking = currentTimeMillis

    // Set candidate finder settings;
    candidateFinder.maxMatches = settings.cfNumCandidates
    candidateFinder.minScore = settings.cfMinScore

    // We process each unique mention only once, to save time;
    val mentionSet: ParSet[String] = mentions.par.map(m => m.mentionText).toSet
    log.debug(f"\tProcessing ${mentionSet.size} unique values for ${mentions.size} mentions")

    // Find candidates for each mention;
    val candidatesAll: Array[CandidateFinderResult] = mentionSet.map(m => candidateFinder.findCandidates(m)).toArray
    val cfTime = currentTimeMillis - startLinking
    val stringMatchTimeTot = candidatesAll.map(_.stringMatchTime).sum
    val cfDisTimeTot = candidatesAll.map(_.cfDisTime).sum
    log.info(s"\tCandidates found: ${candidatesAll.map(c => c.inputName + ": " + c.candidates.size).toSeq}," +
      s" exec. time $cfTime")
    val candidates = candidatesAll.filter(_.candidates.nonEmpty)

    // Perform disambiguation.
    // Don't do it if there is only one mention:
    //  in this case return the best candidate and use its score as global score;
    val (matchesMap, score, optTime): (Map[String, EntityLinkerResult], Double, Long) =
      if (!settings.skipDisambiguation && candidates.length > 1) {

        // Create an optimizer;
        val startOpt = currentTimeMillis
        val optimizer = new Optimizer(settings)

        // Create a problem;
        val problem = new Problem(candidates, embeddings, dimensions, settings)

        // Perform optimization;
        optimizer.optimize(problem)
        val optTime = currentTimeMillis - startOpt
        log.info(s"\tOptimization completed, exec. time $optTime")

        // Fix the result names;
        optimizer.bestTuple.foreach(c => c.result = c.resultID)

        // Return the best tuple;
        (optimizer.bestTuple.map(c => (c.mention, c)).toMap, optimizer.bestScore, optTime)
      } else {
        // Extract the best candidate for each mention;
        (candidates.map(c => (c.inputName, EntityLinkerResult(
          c.inputName,
          c.candidates.head.candidate,
          c.candidates.head.candidateID,
          c.golden,
          c.candidates.head.score
        ))).toMap, 0f, 0L)
      }

    // Restore candidate finder settings;
    candidateFinder.restoreIndexSettings()

    // Associate each mention object to its result, and add an empty result if a mention didn't have candidates;
    val matches: Seq[EntityLinkerResult] = mentions.map(m =>
      matchesMap.getOrElse(
        m.mentionText,
        EntityLinkerResult(m.mentionText, "", "", m.golden.getOrElse("")
        ))).seq
    // Restore golden values;
    matches.zipWithIndex.foreach(m => m._1.golden = mentions(m._2).golden.getOrElse(""))

    EntityLinkerOutDoc(
      id = "",
      text = "",
      entityLinkerResults = matches,
      score = score,
      execTime = currentTimeMillis - startLinking,
      cfTime = cfTime,
      cfStringMatchTime = stringMatchTimeTot,
      cfDisTime = cfDisTimeTot,
      optTime = optTime,
      settings = settings
    )
  }

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Given a document that contains a sequence of mentions, perform entity linking on each of them.
    *
    * @param document a document to be processed
    * @return for each mention, an entity linker result
    */
  def link(
            document: CandidateFinderInputDoc,
          ): EntityLinkerOutDoc = {
    val result = link(document.mentions)
    result.id = document.id
    result.text = result.text
    result
  }
}

