package embeddings_linker

case class EntityLinkerResult(
                               mention: String,
                               var result: String = "",
                               var resultID: String,
                               var golden: String = "",
                               var score: Double = 0d,
                               var salience: Double = 0d
                             )

case class EntityLinkerOutDoc(
                               var id: String,
                               var text: String,
                               entityLinkerResults: Seq[EntityLinkerResult],
                               score: Double,
                               execTime: Long = 0L,
                               cfTime: Long = 0L,
                               cfStringMatchTime: Long = 0L,
                               cfDisTime: Long = 0L,
                               optTime: Long = 0L,
                               settings: EmbeddingsLinkerSettings
                             )

