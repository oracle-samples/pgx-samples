/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package candidate_finder

case class CandidateFinderResult(
                                  inputName: String,
                                  indexMaxMatches: Int,
                                  indexMinScore: Float,
                                  golden: String = "",
                                  execTime: Long = 0L,
                                  stringMatchTime: Long = 0L,
                                  cfDisTime: Long = 0L,
                                  candidates: Seq[Candidate]
                                )

case class Candidate(candidate: String, candidateID: String, var score: Float, var salience: Double = 0d)

case class CandidateFinderOutDoc(
                                  id: String,
                                  text: String,
                                  candidateFinderResults: Seq[CandidateFinderResult]
                                )