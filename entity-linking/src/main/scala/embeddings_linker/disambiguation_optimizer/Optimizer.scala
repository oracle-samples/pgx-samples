package embeddings_linker.disambiguation_optimizer

import embeddings_linker.{EmbeddingsLinkerSettings, EntityLinkerResult}
import org.slf4j.LoggerFactory

class Optimizer(
                 settings: EmbeddingsLinkerSettings
               ) {

  private val log = LoggerFactory.getLogger(classOf[Optimizer])

  val numSteps: Int = settings.optMaxSteps
  val earlyStoppingSteps: Int = settings.optEarlyStop

  // Store the best tuple of candidates found so far;
  var bestTuple: Array[EntityLinkerResult] = Array[EntityLinkerResult]()
  // Store the score of the best tuple;
  var bestScore: Double = Double.MaxValue
  // True if the optimization has already been done;
  var optimizationDone: Boolean = false

  def optimize(problem: Problem): Unit = {
    if (optimizationDone) {
      log.info(f"Optimization already executed with best solution $bestTuple and score $bestScore")
    } else {
      // Stop if more than earlyStoppingSteps have passed without improving the score;
      var currEarlyStopping: Int = 0

      // Find an initial state at random;
      var (currTuple, currScore): (Array[EntityLinkerResult], Double) = problem.initialHeuristic()

      log.debug(f"starting tuple: ${currTuple.map(_.resultID).toSeq}, score: $currScore")

      // Initialize the best result;
      bestTuple = currTuple
      bestScore = currScore

      // Iterate numSteps times;
      for (i <- 0 until numSteps) {
        // Create a list of children from the current state;
        val newChild = problem.createChild(currTuple)
        currTuple = newChild._1
        currScore = newChild._2

        // Update the best result;
        if (currScore <= bestScore) {
          bestTuple = currTuple
          bestScore = currScore
          currEarlyStopping = 0
          log.debug(f"${i+1}/$numSteps) current tuple: ${bestTuple.map(_.resultID).toSeq}, score: $bestScore")
        } else {
          currEarlyStopping += 1
          // Stop if the score hasn't improved for more than earlyStoppingSteps;
          if (currEarlyStopping > this.earlyStoppingSteps) {
            optimizationDone = true
            return
          }
        }
        optimizationDone = true
      }
    }
  }
}

object Optimizer {
  // Default number of optimization steps;
  val DEFAULT_NUM_STEPS: Int = 10
  val DEFAULT_EARLY_STOP: Int = 5
  val DEFAULT_NUM_CHILDREN: Int = 100
}
