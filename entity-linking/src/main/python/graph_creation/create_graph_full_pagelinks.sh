#!/bin/bash

# Create the set of vertices with their instance types;
python3 create_vertex_dict.py -i ../../../../data/graph_data/original/labels_en.ttl -o ../../../../data/graph_data/vertex_set -p;

python3 rdf_to_instance_type_dict.py -i ../../../../data/graph_data/original/instance_types_en.ttl -o ../../../../data/graph_data/instance_types -p -s ../../../../data/graph_data/vertex_set;

# Create the graph;
python3 rdf_to_edgelist.py -i ../../../../data/graph_data/original/redirects_en.ttl ../../../../data/graph_data/original/disambiguations_en.ttl ../../../../data/graph_data/original/mappingbased_objects_en.ttl ../../../../data/graph_data/original/mappingbased_literals_en.ttl ../../../../data/graph_data/original/infobox_properties_en.ttl ../../../../data/graph_data/original/page_links_en.ttl -v ../../../../data/graph_data/graph_v.edgelist -e ../../../../data/graph_data/graph_e.edgelist -j ../../../../data/graph_data/graph.json -t ../../../../data/graph_data/instance_types -p -o
