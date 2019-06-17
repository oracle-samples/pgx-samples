specialties = []
"Reading specialties"
specialtiesPath = "../data/specialties.txt"
graphFilePath = "../data/bipartiteMedicGraph.txt"
new File(specialtiesPath).eachLine { line ->
    specialties.push(line)
}

"Creating the graph config"
builder = GraphConfigBuilder.forFileFormat(Format.EDGE_LIST)
builder.setUri(graphFilePath)
builder.addVertexProperty("specialty", PropertyType.STRING, "")
builder.setVertexIdType(IdType.INTEGER)
cfg = builder.build()

"Load the graph from a config into PGX"
directedG = session.readGraphWithProperties(cfg)
g = directedG.undirect()
directedG.destroy()

"Iterating over all specialties"
for (item in specialties) {
    println item // Printing the specialty that was filtered out
    v = g.getVertices(new VertexFilter("vertex.specialty == '" + item + "'"))
    pgRank = analyst.personalizedPagerank(g, v, 0.001, 0.85, 1000)
    subgraph = g.filter(new VertexFilter("vertex.specialty != '" + item + "' AND vertex.specialty != 'HCPCS'"))
    // We get the top pagerank values from the subgraph
    resultSet = subgraph.queryPgql("SELECT suspect, suspect.${pgRank.getName()} AS pagerank MATCH (suspect) ORDER BY suspect.${pgRank.getName()} DESC LIMIT 10")
    resultSet.print();
    // We clean up the subgraph since we don't need it anymore
    subgraph.destroy()
}