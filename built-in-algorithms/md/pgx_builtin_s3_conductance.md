# Conductance

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s3_conductance
- **Time Complexity:** O(V) with V = number of vertices
- **Space Requirement:** O(1)
- **Javadoc:**
  - [Analyst#conductance(PgxGraph graph, Partition<ID> partition, long partitionIndex)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#conductance_oracle_pgx_api_PgxGraph_oracle_pgx_api_Partition_long_)
  - [Analyst#conductance(PgxGraph graph, Partition<ID> partition, long partitionIndex, Scalar<java.lang.Double> conductance)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#conductance_oracle_pgx_api_PgxGraph_oracle_pgx_api_Partition_long_oracle_pgx_api_Scalar_)

Conductance in a graph is computed for a specific cut of it. A cut is a partition of the graph into two subsets (components), disconnecting the graph if the edges from the cut are removed. Thus the algorithm requires a labeling for the vertices in the different subsets of the graph, then the conductance is computed by the ratio of the edges belonging to the given cut (i.e. the edges that split the graph into disconnected components) and the edges belonging to each of these subsets. If there is more than one cut (or partition), this implementation will take the given component number as reference to compute the conductance associated with that particular cut.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `member` | vertexProp<long> | vertex property with the component label for each vertex in the graph. |
| `num` | long | number of the component to be used for computing its conductance. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | double | conductance value of the graph cut. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;

import static java.lang.Double.POSITIVE_INFINITY;

@GraphAlgorithm
public class Conductance {
  public double conductance(PgxGraph g, VertexProperty<Long> member, long num) {
    long dIn = g
        .getVertices()
        .filter(u -> member.get(u) == num)
        .sum(PgxVertex::getOutDegree);

    long dOut = g
        .getVertices()
        .filter(u -> member.get(u) != num)
        .sum(PgxVertex::getOutDegree);

    long cross = g
        .getVertices()
        .filter(u -> member.get(u) == num)
        .sum(u -> u.getOutNeighbors().filter(j -> member.get(j) != num).size());

    double m = dIn < dOut ? dIn : dOut;

    if (m == 0) {
      return cross == 0 ? 0.0 : POSITIVE_INFINITY;
    } else {
      return cross / m;
    }
  }
}
```
