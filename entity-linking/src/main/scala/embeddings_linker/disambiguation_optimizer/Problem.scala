package embeddings_linker.disambiguation_optimizer

import breeze.linalg.{DenseVector, norm}
import candidate_finder.CandidateFinderResult
import embeddings_linker.{EmbeddingsLinkerSettings, EntityLinkerResult}
import org.slf4j.LoggerFactory

import scala.collection.parallel.immutable.ParMap
import scala.collection.parallel.mutable.ParArray
import scala.util.Random

class Problem(
               val mentions: Array[CandidateFinderResult],
               val embeddings: ParMap[String, DenseVector[Double]],
               val embeddingsSize: Int,
               val settings: EmbeddingsLinkerSettings
             ) {

  private val log = LoggerFactory.getLogger(classOf[Problem])

  val numChildren: Int = settings.optNumChildren
  val optimize: Boolean = settings.optOptimizeChild

  val alpha: Double = Math.min(Math.max(settings.optAlpha, 0d), 1d)
  val beta: Double = Math.min(Math.max(settings.optBeta, 0d), 1d)
  val globalWeight: Double = Math.max(0d, 1d - alpha - beta)

  val alphaExp: Double = Math.max(0d, settings.optAlphaExp)
  val betaExp: Double = math.max(0d, settings.optBetaExp)

  val initHeuristicWeight: Double = Math.min(Math.max(settings.optInitWeight, 0d), 1d)

  /**
    * Given a tuple, compute its score as sum of distances of the candidate embeddings from the mean embedding.
    *
    * @param tuple an input tuple
    * @return the score of the input tuple
    */
  def score(tuple: ParArray[EntityLinkerResult]): Double = {

    // Compute the mean embedding;
    val mean: DenseVector[Double] = tuple.map(c => embeddings(c.resultID)).reduce(_ + _) / tuple.length.toDouble

    // Compute the embedding distances from the mean;
    val mean_norm: Double = norm(mean)
    val globalScore = tuple.map(c => Distances.cosine(embeddings(c.resultID), mean, x_norm=Some(1), y_norm=Some(mean_norm))).sum

    // Compute the sum of candidates scores;
    val candidateScore: Double = tuple.map(1 - _.score).sum

    // Compute the sum of candidate saliences;
    val salience: Double = tuple.map(1 - _.salience).sum

    globalWeight * globalScore  + alpha * candidateScore + beta * salience
  }

  /**
    * Create a new child from a given tuple: generate a number of random tuples from the input one,
    * then keep and optimize the best one.
    *
    * @param currTuple an input tuple
    * @return a new tuple and its score
    */
  def createChild(currTuple: Array[EntityLinkerResult]): (Array[EntityLinkerResult], Double) = {

    // Start creating some children with a simple swap;
    val (currChild, currScore): (Array[EntityLinkerResult], Double) = (0 until numChildren)
      .map(_ => swap(currTuple, RandUtils.randInt(max = currTuple.length))).par
      .map(tuple => (tuple, score(tuple.par)))
      .minBy(_._2)

    // Greedy optimization step;
    if (optimize) {
      log.debug(f"\n\tOptimizing ${currChild.map(_.resultID).toSeq}")
      optimizeTuple(currChild, currScore)
    } else {
      (currChild, currScore)
    }
  }

  /**
    * Create a new tuple by modifying the specified amount of candidates
    *
    * @param tuple an input tuple
    * @param numSwaps number of changes to perform
    * @return a new tuple
    */
  def swap(tuple: Array[EntityLinkerResult], numSwaps: Int = 1): Array[EntityLinkerResult] = {

    // Pick a tuple element to swap;
    val toSwapIds = (1 until numSwaps).map { _ => RandUtils.randInt(max = tuple.length) }

    // Swap with another element from the same candidate set;
    val newTuple: Array[EntityLinkerResult] = tuple.map(_.copy())
    toSwapIds.foreach { id =>
      val newCandidate = mentions(id).candidates(RandUtils.randInt(max = mentions(id).candidates.size))
      newTuple(id).resultID = newCandidate.candidateID
      newTuple(id).score = newCandidate.score
      newTuple(id).salience = newCandidate.salience
    }
    newTuple
  }

  /**
    * Given a tuple, create a new tuple which is an improved version of the first one.
    *
    * @param tuple an input tuple
    * @param initScore score of the input tuple, used as baseline of the optimization
    * @return a modified tuple with an improved score, and the score
    */
  def optimizeTuple(
                     tuple: Array[EntityLinkerResult],
                     initScore: Double = Double.MaxValue
                   ): (Array[EntityLinkerResult], Double) = {

    var resTuple: Array[EntityLinkerResult] = tuple.map(_.copy())
    var currScore = initScore
    val indices = Random.shuffle(0 to tuple.length - 1)

    // Optimize each mention, in random order;
    indices.foreach { i =>
      val optimized = optimizeMention(resTuple, i, currScore)
      resTuple = optimized._1
      currScore = optimized._2
    }

    // If the score was not improved, create a random tuple;
    if (currScore >= initScore) {
      resTuple = RandUtils.randTuple(mentions)
      currScore = score(resTuple.par)
    }

    log.debug(f"\tOptimized: ${resTuple.map(_.resultID).toSeq}, score: $currScore")
    (resTuple, currScore)
  }

  def optimizeMention(
                       tuple: Array[EntityLinkerResult],
                       toOptimizeId: Int,
                       initScore: Double = Double.MaxValue
                     ): (Array[EntityLinkerResult], Double) = {

    log.debug(s"\t\toptimizing index $toOptimizeId, initial tuple: ${tuple.map(_.resultID).toSeq}, score: $initScore")

    val candidates = mentions(toOptimizeId).candidates.par

    // Try all alternatives for a candidate, pick the one with the lowest score;
    val (bestChild, minScore): (Array[EntityLinkerResult], Double) = candidates.map { c =>
      val newTuple = tuple.map(_.copy())
      newTuple(toOptimizeId).resultID = c.candidateID
      newTuple(toOptimizeId).score = c.score
      newTuple(toOptimizeId).salience = c.salience
      newTuple
    }
      .map(t => (t, score(t.par)))
      .minBy(_._2)

    // If the score was not improved, return the starting tuple, else use the best candidate;
    if (minScore < initScore) {
      log.debug(f"\t\toptimized: $toOptimizeId, new tuple: ${bestChild.map(_.resultID).toSeq}, score: $minScore")
      (bestChild, minScore)
    } else {
      log.debug(f"\t\tfailed to optimize: $toOptimizeId, new tuple: ${tuple.map(_.resultID).toSeq}, score: $initScore")
      (tuple, initScore)
    }
  }

  /**
    * Heuristic used for the initial state: keep for each mention the candidate with the best score;
    *
    * @return the best initial tuple, and its score
    */
  def initialHeuristic(): (Array[EntityLinkerResult], Double) = {

    // Raise all mentions candidate scores and salience with the chosen exponential;
    mentions.foreach { m =>
      m.candidates.foreach { c =>
        c.score = Math.pow(c.score, alphaExp).toFloat
        c.salience = Math.pow(c.salience, betaExp)
      }
    }

    val initTuple: Array[EntityLinkerResult] = mentions.map { m =>
      // Pick the candidate with best weighted sum of string similarity and salience;
      val bestCandidate = m.candidates.maxBy(c =>
        c.score * initHeuristicWeight + c.salience * (1 - initHeuristicWeight))
      EntityLinkerResult(
        mention = m.inputName,
        resultID = bestCandidate.candidateID,
        score = bestCandidate.score,
        golden = m.golden,
        salience = bestCandidate.salience
      )
    }
    log.debug(f"initial tuple, bf opt: ${initTuple.map(_.resultID).toSeq}, score: ${score(initTuple.par)}")
    // Run an optimization step;
    if (optimize) optimizeTuple(initTuple, score(initTuple.par))
    else (initTuple, score(initTuple.par))
  }
}

object Problem {
  val DEFAULT_ALPHA: Double = 0d
  val DEFAULT_BETA: Double = 0d
  val DEFAULT_ALPHA_EXP: Double = 1d
  val DEFAULT_BETA_EXP: Double = 1d
  val DEFAULT_INIT_WEIGHT: Double = 0.5
}
