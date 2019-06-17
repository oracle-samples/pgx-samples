 # Analyzing a Superhero Network With Patterns and Computations

This document describes how you can combine computational graph analysis and graph pattern matching with PGX. 

## Data Preparation

In this example, we use a data set which represents a network of fictional characters
from Marvel Comics. The vertices in the graph are fictional characters. The edges between
them represent a co-appearance relationship. That is, an edge indicates that two characters
had appeared in the same issue of a comic. The data is publicly available dataset -
[The Marvel Social Network](https://github.com/gephi/gephi/wiki/Datasets). It also availble on [Kaggle](https://www.kaggle.com/csanhueza/the-marvel-universe-social-network). We have preprocessed the
dataset to edge-list format, and the data are available [for download](https://docs.oracle.com/cd/E56133_01/latest/data/superhero-network-1.0.zip).

The archive contains the data file and graph configuration files: `hero-network.csv` and `config.json`.
Please download the archive and extract it to `data` folder.

The data file is a comma-separated text file (CSV) where each line describes an edge of the graph data. The following is a sample excerpted from the data file. 

```
"IRON MAN IV/JAMES R.","FORTUNE, DOMINIC"
"IRON MAN IV/JAMES R.","ERWIN, CLYTEMNESTRA"
"IRON MAN IV/JAMES R.","IRON MAN/TONY STARK "
"IRON MAN/TONY STARK ","FORTUNE, DOMINIC"
"IRON MAN/TONY STARK ","ERWIN, CLYTEMNESTRA"
"ERWIN, CLYTEMNESTRA","FORTUNE, DOMINIC"
"PRINCESS ZANDA","BLACK PANTHER/T'CHAL"
"PRINCESS ZANDA","LITTLE, ABNER"
"LITTLE, ABNER","BLACK PANTHER/T'CHAL"
```

As you can see, the file is indeed a list of edges in the data graph, while each vertex is represented with a (unique) string ID. The dataset is a *multi-graph* - meaning that
there can be multiple edges between one source vertex and one destination vertex.

The configuration file points PGX to the datafile and specifies the data file format.

```json
{
 "uri": "hero-network.csv",
 "format": "edge_list",
 "vertex_id_type": "string",
 "separator": ","
}
```

For more information about the configuration file, please, see the [Graph Config section](https://docs.oracle.com/cd/E56133_01/latest/reference/loader/graph-config/index.html) of the documentation.

## Superhero Graph Analysis

The first step is to extract the archive containing the data file and configuration file into the `data` folder. From the command line, you can
use `unzip superhero-network-1.0.zip` or use your favourite tool. 

The script analysing the graph is available in `use-cases/superhero/src/superhero-analysis.groovy` 
([show online](src/superhero-analysis.groovy)). We will describe individual commands below, but first, let's run the complete analysis. Open the terminal and move to the `use-cases/superhero/` directory in this repository.
Then run `pgx ./superhero-analysis.groovy`.

The script produces results and shows them on the screen.

### Loading the graph into PGX 

The first step of the graph analysis is to load the graph. In this dataset, we use undirected graph. As the original data file encodes directed edges, we need to *remove* edge direction by calling `undirect()` method.

```groovy
// load the graph and undirect it
G = session.readGraphWithProperties("../data/config.json").undirect()
```

### Computing Centralities with Built-in Algorithms

Now we let's perform some computational analysis on the loaded graph instance. Specifically, we will run
two different centrality algorithms on the graph instance and compare these values. 

In graph theory, [Centrality](http://www.wikipedia.com/wiki/Centrality) is a numeric property associated
with every vertex that indicates the relative importance of the vertex in the graph. 
There exist several different definitions of centralities, however, because there are many different
ways to define *relative importance* of a vertex in a graph. 

For instance, the popular **PageRank** is a kind of centrality. Here the relative importance is
defined recursively -- a vertex is important if it is followed by many other important vertices. Moreover,
it has been shown that the high PageRank value also indicates that the vertex has a high probability of being visited from a random walk. Intuitively, a vertex must be *central* if a high percentage of random walks are likely to go through that vertex.

**Betweenness Centrality** is another centrality definition which originated from Social Science.
In this definition, a vertex has high centrality value if it
frequently appears in the shortest paths between any pair of other vertices. Removing such a vertex would decrease the number of shortest paths between those pairs.
In other words, a vertex is *central*, if it is essential to bridge other
vertices in between. 

PGX provides [built-in algorithms](https://docs.oracle.com/cd/E56133_01/latest/reference/analytics/builtins.html) for computing both of these centrality definitions. The following code snippet
shows how to run them. The first line invokes `PageRank` algorithm
with termination threshold of 0.0001, damping factor 0.85, and maximum iteration of 100. The second line invokes `vertexBetweenessCentrality` algorithm. Note that the `pagerank` method creates a new vertex property
of name `pagerank`, and the `vertexBetweenessCentrality` methods create another of name `betweenness`. 

```groovy
analyst.pagerank(G, 0.0001, 0.85, 100)

analyst.vertexBetweennessCentrality(G)
```

We would like to mention that Betweenness Centrality is very expensive to compute. The algorithm takes O(NM) 
asymptotic execution time where N is a number of the vertices in the graph and M is the number of edges. PGX, however, provides an extremely efficient implementation of this algorithm which makes it possible to run this algorithm on fairly sizeable graphs.


#### Results 

Now, we will check the result of the above analyses. We can see vertices with top 10
pagerank values and top 10 betweenness centrality with following PGQL queries:

```groovy
G.queryPgql("SELECT id(n), n.pagerank, n.betweenness MATCH (n) ORDER BY n.pagerank DESC").print(10).close()
G.queryPgql("SELECT id(n), n.pagerank, n.betweenness MATCH (n) ORDER BY n.betweenness DESC").print(10).close()
```

The above queries are asking to retrieve all the vertices and their
Pagerank and Betweenness Centrality values side by side, but ordered by either
Pagerank or Betweenness Centrality. The results of these two queries are as
below:

```
+--------------------------------------------------------------------+
| id(n)                  | n.pagerank           | n.betweenness      |
+--------------------------------------------------------------------+
| "CAPTAIN AMERICA"      | 0.011065830748883301 | 3879925.0255827564 |
| "SPIDER-MAN/PETER PAR" | 0.010883248192971052 | 5358034.226807129  |
| "IRON MAN/TONY STARK " | 0.00822400232340639  | 2136707.930969874  |
| "WOLVERINE/LOGAN "     | 0.007184179121356004 | 2128411.7635667296 |
| "THOR/DR. DONALD BLAK" | 0.007111076442093899 | 1542352.9408411689 |
| "THING/BENJAMIN J. GR" | 0.007040224708813351 | 1365684.834724244  |
| "HUMAN TORCH/JOHNNY S" | 0.006715685790441994 | 1079956.6513928263 |
| "MR. FANTASTIC/REED R" | 0.006474776413326844 | 1132323.1091922105 |
| "SCARLET WITCH/WANDA " | 0.006292296186722285 | 833361.7039250986  |
| "INVISIBLE WOMAN/SUE " | 0.00607097561105347  | 733228.7710968459  |
+--------------------------------------------------------------------+
```

```
+---------------------------------------------------------------------+
| id(n)                  | n.pagerank            | n.betweenness      |
+---------------------------------------------------------------------+
| "SPIDER-MAN/PETER PAR" | 0.010883248192971052  | 5358034.226807129  |
| "CAPTAIN AMERICA"      | 0.011065830748883301  | 3879925.0255827564 |
| "IRON MAN/TONY STARK " | 0.00822400232340639   | 2136707.930969874  |
| "WOLVERINE/LOGAN "     | 0.007184179121356004  | 2128411.7635667296 |
| "DR. STRANGE/STEPHEN " | 0.004430547838645417  | 1695310.6266366695 |
| "HAVOK/ALEX SUMMERS "  | 0.0031846267944861954 | 1627470.9985449612 |
| "THOR/DR. DONALD BLAK" | 0.007111076442093899  | 1542352.9408411689 |
| "HULK/DR. ROBERT BRUC" | 0.00550332137830238   | 1490075.2090063898 |
| "THING/BENJAMIN J. GR" | 0.007040224708813351  | 1365684.834724244  |
| "DAREDEVIL/MATT MURDO" | 0.0041480217972017204 | 1356349.10782244   |
+---------------------------------------------------------------------+
```

We can see that the top 10 results from two centrality analyses are similar
but not the same. The most popular characters (Spider-Man, Captain America,
Iron Man and Wolverine) are ranked very high in both centralities. For Pagerank,
characters that belong to favourite hero teams (Avengers and Fantastic Four) get higher ranks due to close inter-relationship between them. For Betweenness
Centrality, on the other hand, characters that have their titles
get higher ranks because they act as a bridge between different groups of
characters.


### Graph Pattern Matching 

In the above examples, we used PGQL for ordering vertices and selecting their properties, as in standard SQL. However, PGQL is much more powerful since it can
query complex subgraph patterns.

#### Finding Common Neighbours

As an example, let us consider the following question: 
*"Shang-chi, White Tiger and Iron Fists are three characters in Marvel comic books whose power is based on martial arts. Are there any characters who are linked to all of the three characters? If so, who are they?"*
  
The above question can be expressed with the following PGQL query:

```groovy
G.queryPgql(" \
         SELECT id(x) \
         MATCH (a) - (x) \
             , (b) - (x) \
             , (c) - (x) \
         WHERE id(a) = 'SHANG-CHI' \
           AND id(b) = 'WHITE TIGER/HECTOR A' \
           AND id(c) = 'IRON FIST/DANIEL RAN' \
         ORDER BY id(x)").print(10).close()
```
Note that the query now describes a subgraph pattern of four vertices (`a,b,c,x`) in the WHERE clause.
Here, `a, b, c` are vertices that are uniquely identified by their string ID while `x` is another vertex that has links from all of the three vertices.

The result of this query is, however, somewhat disappointing because the same
character repeatedly appears in the result set. This is because the graph is
a multi-graph. There can be multiple edges between the same pair of nodes -- each
edge introduces a separate answer. 

```
+------------------------+
| id(x)                  |
+------------------------+
| "AYALA, FILIPPO"       |
| "AYALA, FILIPPO"       |
| "AYALA, FILIPPO"       |
| "AYALA, FILIPPO"       |
| "BLACK WIDOW/NATASHA " |
| "BLACK WIDOW/NATASHA " |
| "BLACK WIDOW/NATASHA " |
| "BLACK WIDOW/NATASHA " |
| "BLACK WIDOW/NATASHA " |
| "BLACK WIDOW/NATASHA " |
+------------------------+
```

To avoid this situation, we can easily *simplify* the graph with following command: 

```groovy
G = G.simplify(MultiEdges.REMOVE_MULTI_EDGES, SelfEdges.REMOVE_SELF_EDGES, TrivialVertices.REMOVE_TRIVIAL_VERTICES, Mode.CREATE_COPY,null)
```

The above command removes all the multi-edges (i.e. repeated edges between same
set of nodes), self edges (i.e. an edge whose source and destination vertex is
same), as well as trivial vertices (i.e. a vertex that does not have any incoming or outgoing edge). Also, note that the command, in fact, creates a
simplified copy of the original graph; all the vertex properties of the
original graph is copied into the simplified graph.

Now let us repeat the original query; this time, the result is more informative.
```groovy
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
```

```
+------------------------+
| id(x)                  |
+------------------------+
| "AYALA, FILIPPO"       |
| "BLACK WIDOW/NATASHA " |
| "BOOMERANG/FRED MYERS" |
| "BROWN, ABE"           |
| "BYRD, NATHANIEL ALEX" |
| "CAPTAIN MARVEL/CAPTA" |
| "D'ANGELO, LIEUTENANT" |
| "DAREDEVIL/MATT MURDO" |
| "DIAMOND, BOB"         |
| "FU MANCHU"            |
| "HERCULES [GREEK GOD]" |
| "HULK/DR. ROBERT BRUC" |
| "IRON MAN/TONY STARK " |
| "JACK OF HEARTS/JACK " |
| "KILLDRAGON, HARMONY"  |
| "KNIGHT, MISTY"        |
| "MOON KNIGHT/MARC SPE" |
| "NOVA/RICHARD RIDER"   |
| "SHINCHUKO, LOTUS"     |
| "SPIDER-MAN/PETER PAR" |
| "SUN, LIN"             |
+------------------------+
```

Note that the answer contains not only generally favorite characters who teamed up with these martial-art heroes
(e.g. Iron Man or Spider-Man), but also minor villains whom the three heroes commonly confronted.
For instance, you can see the name of Fu Manchu, an archaic Chinese-themed villain.

#### More Patterns Using Computational Analysis

You can query more complicated patterns with PGQL. Moreover, you can even
make use of the results from the computational analysis in your patterns. 

Let us consider another question: 
*"Among the characters who are linked to Dr. Octopus, find me every minor character who is also linked to a major character other than Spider-Man. Give me a pair of these two characters."*
 
Now we can assume the 'majority' of a character can be indicated by
Pagerank values that we computed early in this document. Now the above question 
can be translated  into the following PGQL query:

```groovy
G.queryPgql(" \
         SELECT id(b), id(x) \
         MATCH (a) - (x) - (b) \
         WHERE id(a) = 'DR. OCTOPUS/OTTO OCT' \
           AND id(b) <> 'SPIDER-MAN/PETER PAR' \
           AND x.pagerank < 0.0001 \
           AND b.pagerank > 0.005").print(10).close()
```

The result of the above query returns 102 rows, of which the first 10 are printed.
Since the query has no `ORDER BY`, the results are returned in an arbitrary order.
The result reveals that some minor
Spider-Man related characters (e.g. Mary and Richard Parker, the parents of
Peter Paker) have connections to other popular superheroes like Fantastic
Four. The query also found some minor villains (e.g. GOG)
that are linked to Dr. Octopus but also confronted other major superheroes 
(e.g. Mr. Fantastic).

```
+-------------------------------------------------+
| id(b)                  | id(x)                  |
+-------------------------------------------------+
| "BEAST/HENRY &HANK& P" | "HOWARD, PROFESSOR MA" |
| "IRON MAN/TONY STARK"  | "BAKER, ANNE-MARIE"    |
| "MR. FANTASTIC/REED R" | "PARKER, RICHARD"      |
| "THING/BENJAMIN J. GR" | "PARKER, RICHARD"      |
| "HUMAN TORCH/JOHNNY S" | "PARKER, RICHARD"      |
| "MR. FANTASTIC/REED R" | "PARKER, MARY"         |
| "MR. FANTASTIC/REED R" | "GOG"                  |
| "THING/BENJAMIN J. GR" | "GOG"                  |
| "INVISIBLE WOMAN/SUE"  | "GOG"                  |
| "HUMAN TORCH/JOHNNY S" | "GOG"                  |
+-------------------------------------------------+
```

## Summary

In this document, we have shown how to use PGX to analyse a public data set of a network of
Marvel Comics superheroes.  We showed that PGX supports both computational analyses as
well as pattern matching. By using both kinds of analysis together, PGX enables
the user to get interesting information even from this simple data set. 
