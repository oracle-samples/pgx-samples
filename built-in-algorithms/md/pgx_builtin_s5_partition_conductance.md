# Partition Conductance

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s5_partition_conductance
- **Time Complexity:** O(E) with E = number of edges
- **Space Requirement:** O(1)
- **Javadoc:**
  - [Analyst#partitionConductance(PgxGraph graph, Partition<ID> partition)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#partitionConductance_oracle_pgx_api_PgxGraph_oracle_pgx_api_Partition_)
  - [Analyst#PgxGraph graph, Partition<ID> partition, Scalar<java.lang.Double> avgConductance, Scalar<java.lang.Double> minConductance](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#partitionConductance_oracle_pgx_api_PgxGraph_oracle_pgx_api_Partition_oracle_pgx_api_Scalar_oracle_pgx_api_Scalar_)

This variant of the conductance algorithm will compute the conductance for the given number of components, returning an output with the minimum value of conductance found from the corresponding partitions and their average conductance value.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `member` | vertexProp<long> | vertex property with the component label for each vertex in the graph. |
| `num_comp` | long | number of components in the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `min_cond` | double | minimum conductance value found from the given partitions. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | double | average conductance value of the partitions in the graph. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class PartitionConductance {
  public double conductanceSmallPartition(PgxGraph g, VertexProperty<Long> member, long numComp,
      @Out Scalar<Double> minCond) {
    Scalar<Long> num = Scalar.create(0L);
    minCond.set(0D);
    double sumCondcond = 0;

    while (num.get() < numComp) {
      long dIn = g.getVertices() //
          .filter(u -> member.get(u) == num.get()) //
          .sum(PgxVertex::getOutDegree);

      long dOut = g.getVertices() //
          .filter(u -> member.get(u) != num.get()) //
          .sum(PgxVertex::getOutDegree);

      long cross = g.getVertices() //
          .filter(u -> member.get(u) == num.get()) //
          .sum(u -> u.getOutNeighbors().filter(j -> member.get(j) != num.get()).size());

      double m = dIn < dOut ? dIn : dOut;
      double cond = m == 0 ? 0.0 : cross / m;
      minCond.reduceMin(cond);
      sumCondcond += cond;
      num.increment();
    }

    return numComp == 0 ? 0 : sumCondcond / numComp;
  }
}
```
