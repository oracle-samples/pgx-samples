//load the graph and undirect it
G = session.readGraphWithProperties("../data/config.json").undirect()


//Run analytics algorithms
println("Running PageRank algorithm...")
analyst.pagerank(G, 0.0001, 0.85, 100)

println("Running Betweenness Centrality algorithm...")
analyst.vertexBetweennessCentrality(G)

//Show results
println("Showing superheros, pagerank and between centrality ordered by their pagerank value.")
G.queryPgql("SELECT id(n), n.pagerank, n.betweenness MATCH (n) ORDER BY n.pagerank DESC").print(10).close()

println("Showing superheros, pagerank and between centrality ordered by their between centrality value.")
G.queryPgql("SELECT id(n), n.pagerank, n.betweenness MATCH (n) ORDER BY n.betweenness DESC").print(10).close()


//Pattern matching in the graph.
println("Show superheros appearing in a comics with all of following superheros: Shang-chi, White Tiger/Hector A and Iron First/Daniel Ran.")
G.queryPgql(" \
         SELECT id(x) \
         MATCH (a) - (x) \
             , (b) - (x) \
             , (c) - (x) \
         WHERE id(a) = 'SHANG-CHI' \
           AND id(b) = 'WHITE TIGER/HECTOR A' \
           AND id(c) = 'IRON FIST/DANIEL RAN' \
         ORDER BY id(x)").print(10).close()

//Remove duplicate edges, self edges and isolated vertices, 
G = G.simplify(MultiEdges.REMOVE_MULTI_EDGES, SelfEdges.REMOVE_SELF_EDGES, TrivialVertices.REMOVE_TRIVIAL_VERTICES, Mode.CREATE_COPY,null)

println("After graph simplification, show superheros appearing in a comics with all of following superheros: Shang-chi, White Tiger/Hector A and Iron First/Daniel Ran.")
resultSet = G.queryPgql(" \
         SELECT id(x) \
         MATCH (a) - (x) \
             , (b) - (x) \
             , (c) - (x) \
         WHERE id(a) = 'SHANG-CHI' \
           AND id(b) = 'WHITE TIGER/HECTOR A' \
           AND id(c) = 'IRON FIST/DANIEL RAN' \
         ORDER BY id(x)")
println("There are " + resultSet.getNumResults() + " superheros appearing in comics with Shang-chi, White Tiger/Hector A and Iron First/Daniel Ran.")
resultSet.print(resultSet.getNumResults())
resultSet.close()

println("Showing superheros appearing with Dr. Octopus/Otto Oct and Spider-man/Peter Par.")
G.queryPgql(" \
         SELECT id(b), id(x) \
         MATCH (a) - (x) - (b) \
         WHERE id(a) = 'DR. OCTOPUS/OTTO OCT' \
           AND id(b) <> 'SPIDER-MAN/PETER PAR' \
           AND x.pagerank < 0.0001 \
           AND b.pagerank > 0.005").print(10).close()
