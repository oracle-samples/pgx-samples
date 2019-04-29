specialtiesPath = "../data/specialties.txt"
graphFilePath = "../data/bipartiteMedicGraph.txt"

specialties = []
"Reading specialties"
new File(specialtiesPath).eachLine { line ->
    specialties.push(line)
}

"Creating the graph config"
builder = GraphConfigBuilder.forFileFormat(Format.EDGE_LIST)
builder.setUri(graphFilePath)
builder.addVertexProperty("speciality", PropertyType.STRING, "")
builder.setVertexIdType(IdType.INTEGER)
cfg = builder.build()

"Load the graph from a config into PGX"
directedG = session.readGraphWithProperties(cfg)
g = directedG.undirect()
directedG.destroy()

// Compile a program that runs a personalized pagerank algorithm taking a set of vertices as the source
pprFromKind = session.compileProgram("./personalized_pagerank_from_kind.gm")
// We create a VertexProperty that is going to hold the actual pagerank
pgRank = g.createVertexProperty(PropertyType.DOUBLE, "pgRank")
"Iterating over all specialties"

for (item in specialties) {
    println (item)
    // // We set the value that we want to filter out
    // // We run the personlized pagerank algorithm
    pprFromKind.run(g, item, g.getVertexProperty("speciality"),0.001, 0.85, 1000, pgRank)
    // // We create a subgraph to filter out the nodes that we don't need to evaluate
    subgraph = g.filter(new VertexFilter("vertex.speciality != '"+item+"' AND vertex.speciality != 'HCPCS'"))
    // // We get the top pagerank values from the subgraph
    resultSet = subgraph.queryPgql("SELECT suspect, suspect.${pgRank.getName()} AS pagerank MATCH (suspect) ORDER BY suspect.${pgRank.getName()} DESC LIMIT 10")
    resultSet.print()
    resultSet.close()
    // // We clean up the subgraph since we don't need it anymore
    subgraph.destroy()
}