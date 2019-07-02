/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package candidate_finder

import me.xdrop.fuzzywuzzy.FuzzySearch
import oracle.pgx.api.{PgxGraph, VertexProperty}
import org.slf4j.LoggerFactory
import utils.PgxManager

import scala.collection.JavaConverters._
import scala.collection.mutable

class CandidateFinder(inputGraphPath: String) {

  private val log = LoggerFactory.getLogger(CandidateFinder.getClass)

  // Index used to find the initial candidates;
  var mgr: PgxManager = new PgxManager(candidateGraphPath = Some(inputGraphPath))
  val index: Seq[String] = CandidateFinder.setupIndexFromGraph(mgr.candidateGraph.get)

  // Salience map, used to add salience to candidates;
  val salienceMap: VertexProperty[String, Double] = mgr.candidateGraph.get.getVertexProperty(PgxManager.SALIENCE)

  var maxMatches: Int = CandidateFinder.DEFAULT_MAX_MATCHES
  var minScore: Float = CandidateFinder.DEFAULT_MIN_SCORE

  /////////////////////////////////////
  /////////////////////////////////////

  /**
    * Find a list of candidates for the given input name,
    * and return each with its similarity score
    * @param inputName an input for which candidates should be found
    * @return a list of tuples containing matches and scores in [0, 1], sorted from highest to lowest score
    */
  def findCandidates(inputName: String, golden: String = ""): CandidateFinderResult = {

    val startTime = System.currentTimeMillis

    // Obtain the candidates with StatSim;
    val results = FuzzySearch.extractTop(inputName, index.asJava, maxMatches).asScala
    var fixed_results = results.map(res => (res.getString, res.getScore.toFloat / 100))
    fixed_results = fixed_results.filter(res => res._2 > minScore)

    val queryTime = System.currentTimeMillis - startTime 

    // Extract candidates from results;
    val startCfDisTime = System.currentTimeMillis
    var candidates = collection.mutable.Map(
      fixed_results.map(res => (res._1, res._2)): _*)

    // Disambiguate candidates;
    candidates = CandidateFinder.processDisambiguations(mgr.candidateGraph.get, candidates)

    val cfDisTime = System.currentTimeMillis - startCfDisTime    

    // Extract the corresponding entries;
    CandidateFinderResult(
      inputName = inputName,
      indexMaxMatches = maxMatches,
      indexMinScore = minScore,
      candidates = candidates.map(pair =>
        Candidate(
          pair._1,
          pair._1,
          pair._2,
          salienceMap.get(pair._1)
        )).toSeq.sortBy(-1 * _.score),
      execTime = System.currentTimeMillis - startTime,
      stringMatchTime = queryTime,
      cfDisTime = cfDisTime,
      golden = golden
    )
  }

  /**
    * Restore the default settings of the StatSim String Matcher;
    */
  def restoreIndexSettings(): Unit = {
    maxMatches = CandidateFinder.DEFAULT_MAX_MATCHES
    minScore = CandidateFinder.DEFAULT_MIN_SCORE
  }
}


/////////////////////////////////////
/////////////////////////////////////

object CandidateFinder {

  val DEFAULT_MAX_MATCHES = 10
  val DEFAULT_MIN_SCORE = 0.1f

  private val log = LoggerFactory.getLogger(CandidateFinder.getClass)

  /**
    * Used to load data and setup the index of StatSim,
    * starting from a [[PgxGraph]].
    *
    * @param g an instance of a [[PgxGraph]]
    * @return a sequence of [[String]], which represents our index
    */
  protected def setupIndexFromGraph(g: PgxGraph): Seq[String] = {
    val startTime = System.currentTimeMillis

    log.info(f"Collecting list of ${g.getNumVertices} vertices...")
    val index = g.getVertices[String].asScala.par.map(_.getId).toSeq.seq
    log.info(f"\tIndex length: ${index.length}, time elapsed: ${(System.currentTimeMillis - startTime) / 1000}%d sec")
    index
  }

  /**
    * Given a disambiguation graph and a map that associates each candidate to its score,
    * create a new map that contains, for each original candidate, its disambiguated candidates,
    * with scores that have been propagated accordingly.
    *
    * @param g  a disambiguation graph
    * @param candidates a map that associates candidates and scores
    * @return a new map that associates the disambiguated candidates to their scores
    */
  protected def processDisambiguations(g: PgxGraph, candidates: mutable.Map[String, Float]): mutable.Map[String, Float] = {
    val visitedVertices = candidates
    val frontierVertices: mutable.Queue[String] = mutable.Queue[String](candidates.keys.toList: _*)
    val outCandidates = mutable.Map[String, Float]()

    // Perform a BFS on the disambiguation graph to propagate the scores
    // of each candidate, and process redirections and disambiguations;
    while (frontierVertices.nonEmpty) {
      // Retrieve the first element in the queue;
      val currElem: String = frontierVertices.dequeue()
      val currElemScore = visitedVertices(currElem)

      val neighbors = g.getVertex(currElem).getOutNeighbors

      // If the current vertex is a leaf, add it as an output candidate;
      if (neighbors.isEmpty) {
        outCandidates(currElem) = currElemScore
      } else {
        // Process each neighbour, which represent a redirection or a disambiguation;
        neighbors.forEach(v => {

          val currVertexId = v.getId
          // Update the score of the current vertex.
          // If the current vertex was considered already, keep the maximum score;
          val score = if (visitedVertices.contains(currVertexId)) {
            Math.max(visitedVertices(currVertexId), currElemScore)
          } else {
            currElemScore
          }
          // If the neighbor was not seen yet or its score was updated, add it;
          if (!visitedVertices.contains(currVertexId) || score > visitedVertices(currVertexId)) {
            visitedVertices(currVertexId) = score
            frontierVertices += currVertexId
          }
        })
      }
    }
    // Return the updated candidates map;
    outCandidates
  }
}
