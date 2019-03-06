#
# Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
# This software is licensed to you under the Universal Permissive License (UPL).
# See below for license terms.
# ____________________________
# The Universal Permissive License (UPL), Version 1.0

# Subject to the condition set forth below, permission is hereby granted to any person
# obtaining a copy of this software, associated documentation and/or data (collectively the "Software"),
# free of charge and under any and all copyright rights in the Software, and any and all patent rights
# owned or freely licensable by each licensor hereunder covering either (i) the unmodified Software as
# contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both

# (a) the Software, and
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the
# Software (each a "Larger Work" to which the Software is contributed by such licensors),

# without restriction, including without limitation the rights to copy, create derivative works of,
# display, perform, and distribute the Software and make, use, sell, offer for sale, import, export,
# have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights
# on either these or other terms.

# This license is subject to the following condition:

# The above copyright notice and either this complete permission notice or at a minimum a reference
# to the UPL must be included in all copies or substantial portions of the Software.

# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
# IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
# SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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