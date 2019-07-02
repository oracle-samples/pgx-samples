/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

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

