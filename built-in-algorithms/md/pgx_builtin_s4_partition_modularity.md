# Modularity

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s4_partition_modularity
- **Time Complexity:** O(E * c) with E = number of edges, c = number of components
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#partitionModularity(PgxGraph graph, Partition<ID> partition)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#partitionModularity-oracle.pgx.api.PgxGraph-oracle.pgx.api.Partition-)
  - [Analyst#partitionModularity(PgxGraph graph, Partition<ID> partition, Scalar<java.lang.Double> modularity)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#partitionModularity-oracle.pgx.api.PgxGraph-oracle.pgx.api.Partition-oracle.pgx.api.Scalar-)

Modularity in a graph is a measure for assessing the quality of the partition induced by the components (or community structures) within the graph found by any clustering algorithm (e.g. label propagation, Infomap, WCC, etc.). It compares the number of the edges between the vertices within a component against the expected number of edges if these were generated at random (assuming a uniform probability distribution). A positive modularity value means that, on average, there are more edges within the components than the amount expected (meaning stronger components), and vice-versa for a negative modularity value. This implementation is intended for directed graphs.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `member` | vertexProp<long> | vertex property with the component label for each vertex in the graph. |
| `num_comp` | long | number of components in the graph found by any clustering algorithm. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | double | modularity value corresponding to the given components in the graph. |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;

@GraphAlgorithm
public class PartitionModularity {
  public double modularitySmallPartition(PgxGraph g, VertexProperty<Long> member, long numComp) {
    Scalar<Double> firstTerm = Scalar.create(0D);
    double secondTerm = 0;
    double m1 = 1 / (double) g.getNumEdges();

    // compute first term: fraction of edges inside community
    g.getVertices().forEach(n -> {
      n.getNeighbors().forEach(m -> {
        if (member.get(n) == member.get(m)) {
          firstTerm.increment();
        }
      });
    });

    firstTerm.set(firstTerm.get() * m1);

    // compute second term: expected number of edges inside community when
    // uniform
    Scalar<Long> num = Scalar.create(0L);
    while (num.get() < numComp) {
      long dIn = g.getVertices().filter(u -> member.get(u) == num.get()).sum(PgxVertex::getInDegree);
      long dOut = g.getVertices().filter(u -> member.get(u) == num.get()).sum(PgxVertex::getOutDegree);

      secondTerm += m1 * dIn * dOut;  // avoid overflow
      num.increment();
    }
    secondTerm = secondTerm * m1;

    return firstTerm.get() - secondTerm;
  }
}
```
