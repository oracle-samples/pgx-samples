#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sat Jan 13 14:11:37 2018

@author: albyr
"""

#%%
import argparse
import time
import pickle
import utils
import ntpath
import json

def get_instance_type(entity_name, instance_dict=None):
    """
    :param entity_name: name of an entity;
    :param instance_dict: dictionary that contains the instance type of each entity;
    :return: the instance type of the provided entity;
    
    Get the instance type of a given entity, as specified by the instance_dict;
    If the entity is not present, return "owl#Thing";
    """
    if (instance_dict is None) or (entity_name not in instance_dict):
        return "owl#Thing"
    else:
        return instance_dict[entity_name]


def write_graph(input_path, output_path_v, output_path_e, instance_dict=None, lines_to_read=0, vertex_dict=None,
                print_details=True, instance_dict_vertices_only=True, ignore_literals=True,
                add_orphan_vertices=False, add_triples=False, track_vertex_pairs=False, track_all_edges=False, edge_set=None, skip_existing_pairs=False):
    """
    :param input_path: a text file containing a list of RDF triples;
    :param output_path_v: the file path where the vertices are written;
    :param output_path_e: the file path where the edges are written;
    :param graph_dict_path: path to where the vertex id dictionary is stored,
        as a pickled file;
    :param instance_dict: the dictionary that contains the instance type of each vertex;
    :param lines_to_read: the maximum number of triples to be read;
        if < 1 read the entire file;
    :param add_orphan_vertices: if True, add to the graph all the vertices in instance dict;
    :param print_details: boolean, if True print details about the state of the processing;
    :param add_triples: if True, return a dictionary that contains all the triples that have been addded to the graph;
    :param track_vertex_pairs: if True, store the vertex pairs that are added as edges, in a directed way.
    :param track_all_edges: if True, store all the vertex pairs, otherwise store only redirects/disambiguations.
    :param edge_set: if present, add vertex pairs to this set;
    :param skip_existing_pairs: if True, don't add the edges that are present in the edge_set;
    :return: tuple that contains the number of lines that have been read,
        and the number of vertices in the graph;
        
    Write a list of RDF triples into a PGX-compatible graph, 
    written in EDGELIST format. 
    """
    
    # Read the input file line-by-line, and add the information to a dictionary 
    # that represents the graph;
    # Write the graph as EDGELIST text files;
    
    # A dictionary in which each entity is given a unique id;
    # If specified, load an existing one;
    if vertex_dict is not None:
        write_mode = "a+"
    else:
        vertex_dict = {}
        write_mode = "w+"
    
    # Time the execution;
    start_time = time.time()

    triple_dict = {}
    if edge_set is None:
        edge_set = set()
    edge_filter = ["wikiPageRedirects", "wikiPageDisambiguates"]
    
    current_line = 0
    edge_count = 0
    skipped_self_loops = 0
    # ID of the next vertex to be added;
    # using len(graph) + 1 allows incremental additions;
    vertex_id = len(vertex_dict) + 1
    with open(output_path_v, write_mode, encoding="utf-8") as outfile_v:
        with open(output_path_e, write_mode, encoding="utf-8") as outfile_e:
            with utils.read_compressed(input_path) as infile:
                for line in infile:
                    # Skip the header line, and skip commented-out lines;
                    if current_line != 0 and line[0] != "#":
                        # Create a triple from the given line;
                        triple = utils.clean_line(line, ignore_literals, obtain_resource_manually=True)
                        source, relation, destination = triple
                         
                        if source == destination:
                            skipped_self_loops += 1
                        
                        # It is possible to skip the current edge if its vertices are not in the list of vertices;
                        if not instance_dict_vertices_only or (instance_dict is None) \
                            or ((source in instance_dict) and (destination in instance_dict)):
                         
                            # Add the triple to the graph;
                            # Also add a unique vertex_id to each vertex that is added;
                            
                            # The third element is processed first,
                            # so that we have its unique ID if we have to add a new edge;
                            skip_dest = False
                            # Check if the current triple should be skipped;
                            if (source == "" or relation == ""):
                                skip_dest = True
                            if ignore_literals and destination == "":
                                skip_dest = True
                                
                            # Add source and destination vertices;
                            
                            # Add the source vertex;
                            if source not in vertex_dict:
                                # Keep track of the vertex with a unique ID
                                vertex_dict[source] = vertex_id
                                                                
                                # Write the name of the entity and its type;
                                # each line has the form "entity_name, {instance_type}";
                                outfile_v.write('"{}" * "{}"\n'.format(source,
                                                get_instance_type(source, instance_dict)))
                                vertex_id += 1
                            
                            # Add the destination vertex;
                            if (not skip_dest) and (destination not in vertex_dict):
                                # Keep track of the vertex with a unique ID
                                vertex_dict[destination] = vertex_id
                                                                
                                # Write a new vertex like before;
                                outfile_v.write('"{}" * "{}"\n'.format(destination,
                                                get_instance_type(destination, instance_dict)))
                                vertex_id += 1
                                    
                            # Add a new edge;
                            
                            # Skip self-loops;
                            if not skip_dest and (source != destination):
                                if add_triples:
                                    if source in triple_dict:
                                        triple_dict[source] += [triple]
                                    else:
                                        triple_dict[source] = [triple]
                                # Write a new edge;
                                
                                if not (skip_existing_pairs and (vertex_dict[source], vertex_dict[destination]) in edge_set):
                                    outfile_e.write('"{}" "{}" "{}"\n'.format(source, destination, relation))
                                    edge_count += 1
                                # Keep track of the pairs (source, destination), after writing the current edge
                                # (otherwise no edge is added!);
                                if track_vertex_pairs:
                                    if not track_all_edges and relation in edge_filter:
                                        edge_set.add((vertex_dict[source], vertex_dict[destination]))
                                    else:
                                        edge_set.add((vertex_dict[source], vertex_dict[destination]))

                    current_line += 1
                    
                    if not current_line % 100000 and print_details:
                        print("\tLINES READ: {} -- TIME: {:.2f} seconds -- TOT. VERTICES: {} -- EDGES ADDED: {}"
                              .format(current_line, time.time() - start_time, vertex_id, edge_count))
                    
                    # Stop reading if enough lines have been read;
                    if lines_to_read > 0 and current_line > lines_to_read:
                        break
    
        # Add all the remaining vertices;
        if add_orphan_vertices and instance_dict is not None:
            additional_vertices = 0
            for v in instance_dict:
                if v not in vertex_dict:
                    vertex_dict[v] = vertex_id
                    outfile_v.write('"{}" * "{}"\n'.format(v,
                                    get_instance_type(v, instance_dict)))
                    vertex_id += 1
                    additional_vertices += 1
    
            if print_details:
                print("ADDITIONAL VERTICES FROM INSTANCE DICT: {}".format(additional_vertices))
     
    print("SKIPPED SELF LOOPS: {}".format(skipped_self_loops))

    return (current_line-1, triple_dict, edge_set, vertex_dict, edge_count)


def run(input_path,
        output_path_v,
        output_path_e,
        output_path_j,
        instance_dict_path=None,
        instance_dict_vertices_only=True,
        ignore_literals=False,
        add_orphan_vertices=False,
        lines_to_read=0,
        print_details=True):

    if print_details:
        print("\nCONVERTING RDF TRIPLES INTO A PGX GRAPH:")
        print("\tInput: {}".format("; ".join(input_path)))
        print("\tVertices Output: {}".format(output_path_v))
        print("\tEdges Output: {}".format(output_path_e))
        print("\tConfiguration Output: {}".format(output_path_j))
        print("\tInstance Dictionary: {}".format(instance_dict_path))
        print("\tInstance Dictionary Vertices Only: {}".format(instance_dict_vertices_only))
        print("\tSkip Non-Resource Vertices: {}".format(ignore_literals))
        print("\tAdd Orphan Vertices: {}".format(add_orphan_vertices))
        print("\tLines To Read: {}\n".format(lines_to_read if lines_to_read > 0 else "ALL"))
        print("\n")

    instance_dict = {}
    if instance_dict_path is not None:
        with open(instance_dict_path, 'rb') as f:
            instance_dict = pickle.load(f)

    start_time_main = time.time()

    # If True, we only add wikiLinks that are not existing edges.
    # Otherwise, we add all wikiLinks that are not redirects or disambiguations;
    track_all_edges = True

    existing_vertices_dict = None
    triple_dict = {}
    edge_set = set()
    edge_count = 0
    # Build the graph;
    for input_path_i in input_path:
        print("Processing: {}".format(input_path_i))

        # Check if we are processing a redirects or disambiguations file; if so, track the edges that are added;
        if not track_all_edges:
            track_vertex_pairs = "redirects" in input_path_i or "disambiguations" in input_path_i
        else:
            track_vertex_pairs = True

        if "page_links" in input_path_i:
           edge_set_in = edge_set
           skip_existing_pairs = True
            # edge_set_in = None
            # skip_existing_pairs = False
        else:
            edge_set_in = None
            skip_existing_pairs = False

        lines_read, triples_dict_temp, edge_set_temp, existing_vertices_dict, edge_count_temp = write_graph(
            input_path=input_path_i,
            output_path_v=output_path_v,
            output_path_e=output_path_e,
            instance_dict=instance_dict,
            lines_to_read=lines_to_read,
            vertex_dict=existing_vertices_dict,
            print_details=print_details,
            instance_dict_vertices_only=instance_dict_vertices_only,
            ignore_literals=ignore_literals,
            add_orphan_vertices=add_orphan_vertices,
            track_vertex_pairs=track_vertex_pairs,
            track_all_edges=track_all_edges,
            edge_set=edge_set_in,
            skip_existing_pairs=skip_existing_pairs,
            add_triples=False)

        triple_dict.update(triples_dict_temp)
        edge_set.update(edge_set_temp)
        edge_count += edge_count_temp
        print("Current Edge Count: {}".format(edge_count))
        print("Current Edge Pairs Count: {}".format(len(edge_set)))
    exec_time = time.time() - start_time_main

    print("\nEXECUTION DONE:")
    print("Lines Read: {}".format(lines_read))
    print("Number of Vertices: {}".format(len(existing_vertices_dict)))
    print("Number of Edges: {}".format(edge_count))
    print("Execution Time: {:.2f} seconds".format(exec_time))


    # Write the JSON configuration file;
    config_dict = {"uris": [ntpath.basename(output_path_v), ntpath.basename(output_path_e)],
                   "format": "edge_list",
                   "vertex_id_type": "string",
                   "vertex_labels": False,
                   "edge_label": True,
                   "vertex_props": [{"name": "type", "type": "string"}],
                   "loading": {"load_vertex_labels":False, "load_edge_label": True},
                   "separator":" "
                   }

    # Write the JSON to output;
    with open(output_path_j, 'w') as f:
        json.dump(config_dict, f)

#%%

if __name__ == "__main__":
    
    # Used to parse the input arguments;
    parser = argparse.ArgumentParser(description="Convert a list of RDF triples to a PGX graph.")
    parser.add_argument("-i", "--input", metavar="<path/to/input/data>", nargs="+",
                        help="Paths to the files where the RDF triples are stored, as text files.")
    parser.add_argument("-v", "--output_v", metavar="<path/to/output/vertices/file>",
                        help="Path to the file where the vertices are stored, as a text file.")
    parser.add_argument("-e", "--output_e", metavar="<path/to/output/edges/file>",
                        help="Path to the file where the edges are stored, as a text file.")   
    parser.add_argument("-j", "--output_j", metavar="<path/to/output/json/config>",
                        help="Path to the file where the graph configuration file is saved, as a JSON.")   
    parser.add_argument("-t", "--instance_dict", nargs="?", metavar="path/to/instance/type/dict",
                        help="Path to an existing pickled dictionary\
                        that contains the instance type of each entity.")
    parser.add_argument("-o", "--instance_dict_vertices_only", action='store_true',
                        help="If present, add to the graph only the vertices present in the list of resources.")
    parser.add_argument("-r", "--ignore_literals", action='store_true',
                        help="If present, ignore non-resource object triples, such as literals.")
    parser.add_argument("-a", "--add_orphan_vertices", action='store_true',
                        help="If present, add all the vertices in the instance dict, even if they have no edges.")
    parser.add_argument("-n", "--max_lines", nargs="?", type=int, metavar="N",
                        help="Maximum number of RDF triples to be read")
    parser.add_argument("-p", "--print_details", action='store_true',
                        help="If present, print details about the computation.")
    
    # Parse the input arguments;
#    args = parser.parse_args(["-i",
#                              "../../../graph_data/original/redirects_en.ttl",
#                              "../../../graph_data/original/disambiguations_en.ttl",
#                              "../../../graph_data/original/mappingbased_objects_en.ttl", 
#                              "../../../graph_data/original/mappingbased_literals_en.ttl",
#                              "../../../graph_data/original/infobox_properties_en.ttl",
#                              "../../../graph_data/original/page_links_en.ttl",
#                              "-v", "../../../graph_data/graph_small_v.edgelist",
#                              "-e", "../../../graph_data/graph_small_e.edgelist", "-j", "../../../graph_data/graph_small.json",
#                              "-t", "../../../graph_data/instance_types", "-p", "-o",
#                              "-n", "100000"])
    args = parser.parse_args()

    run(args.input,
        args.output_v,
        args.output_e,
        args.output_j,
        lines_to_read=args.max_lines if args.max_lines is not None else 0,
        instance_dict_path=args.instance_dict,
        print_details=True if args.print_details is not None else False,
        instance_dict_vertices_only=args.instance_dict_vertices_only,
        ignore_literals=args.ignore_literals,
        add_orphan_vertices=args.add_orphan_vertices)

