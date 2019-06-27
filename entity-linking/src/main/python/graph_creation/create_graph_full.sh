#!/bin/bash

# Create the set of vertices with their instance types;
python3 create_vertex_dict.py -i ../../../graph_data/original/labels_en.ttl -o ../../../graph_data/vertex_set -p;

python3 rdf_to_instance_type_dict.py -i ../../../graph_data/original/instance_types_en.ttl -o ../../../graph_data/instance_types -p -s ../../../graph_data/vertex_set;

# Create the graph;
python3 rdf_to_edgelist.py -i ../../../graph_data/original/redirects_en.ttl ../../../graph_data/original/disambiguations_en.ttl ../../../graph_data/original/mappingbased_objects_en.ttl ../../../graph_data/original/mappingbased_literals_en.ttl ../../../graph_data/original/infobox_properties_en.ttl -v ../../../graph_data/graph_full_v.edgelist -e ../../../graph_data/graph_full_e.edgelist -j ../../../graph_data/graph_full.json -t ../../../graph_data/instance_types -p -o
