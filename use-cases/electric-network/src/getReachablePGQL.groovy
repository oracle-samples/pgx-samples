// Input and output config
graph_path  = "../data/electric_graph.edge.json"
output_path = "../data/reachable_subgraph.csv"

// Load electric network graph in to PGX and create useful properties
startTime = System.currentTimeMillis()
g = session.readGraphWithProperties(graph_path)
g = g.undirect()
loadingTime = System.currentTimeMillis() - startTime

g.createVertexProperty(PropertyType.VERTEX, "bfs_parent")
g.createVertexProperty(PropertyType.INTEGER, "bfs_level")

// Select source vertex
s = g.getVertex(21474836490)

// Compile and execute BFS Green-Marl program
gm_bfs = session.compileProgram("bfs_reachable.gm")
execution_summary_dic = gm_bfs.run(g, s, g.getVertexProperty("switch_default"), g.getVertexProperty("bfs_level"),  g.getVertexProperty("bfs_parent"))

// Query vertices using PGQL
startTime = System.currentTimeMillis();
rs = g.queryPgql("SELECT n, n.bfs_level MATCH (n) WHERE n.bfs_level >= 0 ORDER BY n.bfs_level")
pgqlTime = System.currentTimeMillis() - startTime;

// Build and save a report
startTime = System.currentTimeMillis();
file_writer = new FileWriter(new File(output_path), false)
buff_writer = new BufferedWriter(file_writer)

println "Dumping output results to " + output_path

line = "n,n.bfs_level,n.bfs_parent\n"
buff_writer.write(line)

parent = g.getVertexProperty("bfs_parent")
rs.getResults().each {
    v = it.getVertex("n")
    line = v.getId() + "," + it.getInteger("n.bfs_level") + "," + parent.get(v).getId() + "\n"
    buff_writer.write(line)
}

buff_writer.flush()
buff_writer.close()
reportTime = System.currentTimeMillis();

"\n\n"
"\tGraph loading time: " + loadingTime/1000 + " seconds"
"\tBFS elapsed time: " + execution_summary_dic["executionTimeMs"] + " millisenconds"
"\tPGQL execution time: " + pgqlTime + " milliseconds"
"\tReached nodes: " + rs.getNumResults()
"\tReport saving time: " + (reportTime - startTime)/1000 + " seconds"
"\tTotal vertices: " + g.getNumVertices()
"\tTotal edges: " + g.getNumEdges()
"\n"
"\tType exit to quit ... "
"\n\n"
