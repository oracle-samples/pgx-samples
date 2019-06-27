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