package candidate_finder

case class Mention(
                    mentionText: String,
                    mentionStart: Int,
                    mentionEnd: Int,
                    golden: Option[String] = None
                  )

case class CandidateFinderInputDoc(
                                    id: String,
                                    text: String,
                                    mentions: Seq[Mention]
                                  )

