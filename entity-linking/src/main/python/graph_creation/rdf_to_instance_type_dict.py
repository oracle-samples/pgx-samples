# -*- coding: utf-8 -*-
"""
Created on Thu Jan 18 11:00:37 2018

This script is used to build a dictionary that associates each entity in DBPedia
to its RDF instance-type (e.g. owl#Thing).
The resulting dictionary is used as input of "rdf_to_edgelist.py", in order to 
obtain additional information about the vertices.

@author: albyr
"""
import time
import pickle
import argparse
import utils


def generate_inst_dict(instance_file_path, lines_to_read, vertex_set=None, print_details=False):
    """
    :param instance_file_path: path to the instance file used to generate the dictionary;
    :param lines_to_read: number of lines to read in the file; if <= 0, read the entire file;
    :param print_details: boolean, if True print details about the state of the processing;
    :param vertex_set: if present, use a set of vertices as starting point, with all the vertices being "owl#Thing";
    :return: dictionary that contains for each entity its instance type;
    """
    
    # Dictionary where the entity types are stored;
    instance_dict = {}
    
    current_line = 0
    start_time = time.time()
    with utils.read_compressed(instance_file_path) as infile:
        for line in infile:
            # Skip the header line, and skip commented-out lines;
            if current_line != 0 and line[0] != "#":
                # Clean the line and split it in 4; keep only the first 3;
                triple = utils.clean_line(line, ignore_literals=True, obtain_resource_manually=True)
                entity_name, _, entity_type = triple
 
                # Add to the dict;
                # Note: data are dirty, some entities appears more than once 
                # with different types.
                # As this problem doesn't occur often, the last type that appears is kept.
                # If the entity is already present, with type "owl#Thing", the type is overwritten with something more specific.
                if len(entity_name) > 0 and len(entity_type) > 0:
                    if vertex_set is None or entity_name in vertex_set:
                        if entity_name not in instance_dict or (entity_name in instance_dict and instance_dict[entity_name] == "owl#Thing"):
                            instance_dict[entity_name] = entity_type
                
            current_line += 1

            if not current_line % 100000 and print_details:
                print("LINES READ: {} -- ELAPSED TIME: {:.2f} seconds".format(current_line, time.time() - start_time))
                 
            # Stop reading if enough lines have been read;
            if lines_to_read > 0 and current_line > lines_to_read:
                break
            
    # Add all the other vertices;
    for v in vertex_set:
        if v not in instance_dict:
            instance_dict[v] = "owl#Thing"
    
    return instance_dict


def run(instance_file_path, dict_path, lines_to_read=0, vertex_set_path=None, print_details=True):
    if print_details:
        print("\nPROCESSING RDF INSTANCE TYPES:")
        print("\tInput Path: ", instance_file_path)
        print("\tOutput Path: ", dict_path)
        print("\tLines To Read: ", str(lines_to_read) if lines_to_read > 0 else "ALL")
        print("\n")

    vertex_set = {}
    if vertex_set_path is not None:
        with open(vertex_set_path, 'rb') as f:
            vertex_set = pickle.load(f)

            # Time the execution;
    start_time = time.time()
    # Build the dictionary;
    instance_dict = generate_inst_dict(instance_file_path, lines_to_read, vertex_set, print_details)
    exec_time = time.time() - start_time

    if print_details:
        print("\nEXECUTION DONE:")
        print("Number Of Vertices: {}".format(len(instance_dict)))
        print("Execution Time: {:.2f} seconds".format(exec_time))

        # Store the graph dictionary;
    with open(dict_path, "wb") as f:
        pickle.dump(instance_dict, f)
        
    return instance_dict

#%%


if __name__ == "__main__":
    
    # Used to parse the input arguments;
    parser = argparse.ArgumentParser(description="Generate a dictionary that contains for each entity its RDF instance type.")
    parser.add_argument("-i", "--input", metavar="<path/to/input/data>",
                        help="Path to the file where the instance types are stored, as a text file.")
    parser.add_argument("-o", "--output", metavar="<path/to/output/dictionary>",
                        help="Path to the file where the pickled dictionary is stored.")    
    parser.add_argument("-s", "--vertex_set", nargs="?", metavar="<path/to/vertex/set>",
                        help="Path to where the pickled vertex set is stored.")
    parser.add_argument("-n", "--max_lines", nargs="?", type=int, metavar="N",
                        help="Maximum number of RDF triples to be read")
    parser.add_argument("-p", "--print_details", action='store_true',
                        help="If present, print details about the computation.")

    # Parse the input arguments;
#    args = parser.parse_args(["-i", "../../../../data/graph_data/original/instance_types_en.ttl",
#                              "-o", "../../../../data/graph_data/instance_types", "-p",
#                              "-s", "../../../../data/graph_data/vertex_set"])
    args = parser.parse_args()

    instance_dict = run(
	args.input,
        args.output,
        lines_to_read=0 if args.max_lines is None else args.max_lines,
        vertex_set_path=args.vertex_set if args.vertex_set is not None else None,
        print_details=args.print_details
	)

