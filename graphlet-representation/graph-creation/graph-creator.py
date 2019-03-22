#
# Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#

import pandas as pd
import os
import json
from os.path import dirname

if __name__ == "__main__":

    data_dir = os.path.join(dirname(os.getcwd()), "data/")
    input_dir = os.path.join(data_dir, "input/NCI109/")
    graph_dir = os.path.join(data_dir, "graph/NCI109/")
    if not os.path.exists(input_dir):
        os.makedirs(input_dir)
    if not os.path.exists(graph_dir):
        os.makedirs(graph_dir)

    # sparse (block diagonal) adjacency matrix for all graphs
    f_edges = pd.read_csv(input_dir+"NCI109_A.txt", header=None, names=["src", "dst"])

    # column vector of graph identifiers for all nodes of all graphs
    f_graph_id = pd.read_csv(input_dir+"NCI109_graph_indicator.txt", header=None, names=["graph_id"])

    # column vector of node labels
    f_node_label = pd.read_csv(input_dir+"NCI109_node_labels.txt", header=None, names=["node_label"])

    # information for all the nodes
    f_node_info = pd.merge(f_graph_id, f_node_label, left_index=True, right_index=True)

    # create the vertex file
    f_node_info.to_csv(graph_dir+"NCI109_v.csv", header=True, index=True, index_label="v_id")

    # create the edge file
    f_edges.to_csv(graph_dir+"NCI109_e.csv", header=True, index=False)


    # create a configuration file
    config = {
        "header": True,
        "vertex_id_column": "v_id",
        "edge_source_column": "src",
        "edge_destination_column": "dst",
        "format": "csv",
        "separator": ",",
        "vertex_id_type": "int",
        "edge_uris": ["NCI109_e.csv"],
        "vertex_uris": ["NCI109_v.csv"],
        "vertex_props": [
            {"name": "graph_id", "type": "int"},
            {"name": "node_label", "type": "int"}
        ]
    }

    # Save to file;
    with open(graph_dir+"NCI109.json", "w") as f:
        json.dump(config, f, indent=4)

    print("Created the graph representation in %s ready to load in PGX" % graph_dir)