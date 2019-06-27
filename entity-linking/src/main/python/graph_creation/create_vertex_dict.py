#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Create a set that contains all the resources on DBPedia, 
so that they can be used as vertices of the knowledge graph.

Created on Wed Apr 18 14:07:50 2018

@author: aparravi
"""

import time
import pickle
import argparse
import utils


def generate_vertex_set(input_file_path, lines_to_read, print_details):
    """
    :param input_file_path: path to the instance file used to generate the dictionary
    :param lines_to_read: number of lines to read in the file; if <= 0, read the entire file
    :param print_details: boolean, if True print details about the state of the processing
    :return: set that contains the vertices of the graph
    """
    
    # Set where the vertices are stored;
    vertex_set = set()
    start_time = time.time()
    current_line = 0
    with utils.read_compressed(input_file_path) as infile:
        for line in infile:
            # Skip the header line, and skip commented-out lines;
            if current_line != 0 and line[0] != "#":
                # Clean the line and split it in 3; keep only the first;
                resource_link = utils.split_triple(line)[0]
                
                # Obtain entity name and type;
                resource_name = utils.clean_resource(resource_link)
                
                if resource_name != "":
                    vertex_set.add(resource_name)
                
            current_line += 1

            if not current_line % 100000 and print_details:
                print("LINES READ: {} -- ELAPSED TIME: {:.2f} seconds -- VERTICES: {}".format(current_line, time.time() - start_time, len(vertex_set)))
                 
            # Stop reading if enough lines have been read;
            if lines_to_read > 0 and current_line > lines_to_read:
                break
                
    return vertex_set


def run(input_file_path, set_path, lines_to_read=0, print_details=True):
    if print_details:
        print("\nPROCESSING WIKIPEDIA LISTS:")
        print("\tInput Path: ", input_file_path)
        print("\tOutput Path: ", set_path)
        print("\tLines To Read: ", str(lines_to_read) if lines_to_read > 0 else "ALL")
        print("\n")

    # Time the execution;
    start_time = time.time()
    # Build the dictionary;
    vertex_set = generate_vertex_set(input_file_path, lines_to_read, print_details)
    exec_time = time.time() - start_time

    if print_details:
        print("\nEXECUTION DONE:")
        print("Number Of Vertices: {}".format(len(vertex_set)))
        print("Execution Time: {:.2f} seconds".format(exec_time))

        # Store the graph dictionary;
    with open(set_path, "wb") as f:
        pickle.dump(vertex_set, f)
        
    return vertex_set


if __name__ == "__main__":
    
    # Used to parse the input arguments;
    parser = argparse.ArgumentParser(description="Generate a set that contains all the vertices of the graph.")
    parser.add_argument("-i", "--input", metavar="<path/to/input/data>",
                        help="Path to the file where the resource list is stored, as a ttl file.")
    parser.add_argument("-o", "--output", metavar="<path/to/output/set>",
                        help="Path to the file where the pickled set is stored.")    
    parser.add_argument("-n", "--max_lines", nargs="?", type=int, metavar="N",
                        help="Maximum number of RDF triples to be read")
    parser.add_argument("-p", "--print_details", action='store_true',
                        help="If present, print details about the computation.")
    
    # Parse the input arguments;
#    args = parser.parse_args(["-i", "../../../../data/graph_data/original/labels_en.ttl",
#                              "-o", "../../../../data/graph_data/vertex_set", "-p"])
    args = parser.parse_args()

    vertex_set = run(args.input,
            args.output,
            lines_to_read=0 if args.max_lines is None else args.max_lines,
            print_details=args.print_details)


