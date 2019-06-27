package embeddings_linker

import candidate_finder.CandidateFinder
import embeddings_linker.disambiguation_optimizer.{Optimizer, Problem}

case class EmbeddingsLinkerSettings(
                                     var cfNumCandidates: Int = CandidateFinder.DEFAULT_MAX_MATCHES,
                                     var cfMinScore: Float = CandidateFinder.DEFAULT_MIN_SCORE,
                                     var skipDisambiguation: Boolean = false,
                                     var optMaxSteps: Int = Optimizer.DEFAULT_NUM_STEPS,
                                     var optEarlyStop: Int = Optimizer.DEFAULT_EARLY_STOP,
                                     var optNumChildren: Int = Optimizer.DEFAULT_NUM_CHILDREN,
                                     var optOptimizeChild: Boolean = true,
                                     var optAlpha: Double = Problem.DEFAULT_ALPHA,
                                     var optBeta: Double = Problem.DEFAULT_BETA,
                                     var optAlphaExp: Double = Problem.DEFAULT_ALPHA_EXP,
                                     var optBetaExp: Double = Problem.DEFAULT_BETA_EXP,
                                     var optInitWeight: Double = Problem.DEFAULT_INIT_WEIGHT
                                   )
