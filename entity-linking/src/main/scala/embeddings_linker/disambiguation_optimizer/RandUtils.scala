/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package embeddings_linker.disambiguation_optimizer

import candidate_finder.{Candidate, CandidateFinderResult}
import embeddings_linker.EntityLinkerResult
import org.slf4j.LoggerFactory

object RandUtils {

  private val log = LoggerFactory.getLogger(RandUtils.getClass)

  val rnd = new scala.util.Random

  /**
    * Obtain a random integer in the specified range (min inclusive, max exclusive)
    *
    * @param min lower bound (inclusive)
    * @param max upper bound (exclusive)
    * @return a random integer in [min, max)
    */
  def randInt(min: Int = 0, max: Int): Int = {
    rnd.nextInt(max - min) + min
  }

  /**
    * Obtain a random tuple from the given sequence of candidates
    *
    * @param mentions a list of [[CandidateFinderResult]]
    * @return a list of [[Candidate]] extracted from it
    */
  def randTuple(mentions: Array[CandidateFinderResult]): Array[EntityLinkerResult] = {
    mentions.map { x => {
      val candidate = x.candidates(randInt(max = x.candidates.size))
      EntityLinkerResult(
        mention = x.inputName,
        resultID = candidate.candidateID,
        score = candidate.score,
        golden = x.golden,
        salience = candidate.salience
      )
    }
    }
  }
}
