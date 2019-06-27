/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

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
