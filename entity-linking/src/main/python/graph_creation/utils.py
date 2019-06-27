#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#
"""
Utility functions used when loading the graphs.

Created on Thu Jan 18 10:08:12 2018
"""
import os
import urllib.parse as url_parser
import html
import gzip
import bz2

# DBPedia prefix;
dbpedia_prefix = ["http://dbpedia.org/resource/", "http://dbpedia.org/ontology/"]


def read_compressed(path, encoding='utf8'):
    if path.endswith('.gz'):
        return gzip.open(path, mode='rt', encoding=encoding)
    elif path.endswith('.bz2'):
        return bz2.open(path, mode='rt', encoding=encoding)
    else:
        return open(path, mode='r', encoding=encoding)


def replace_prefix(line):

    for prefix in dbpedia_prefix:
        if prefix in line:
            return line.replace(prefix, "")
    return line


def clean_text(r):
    """
    :param r: some text to be cleaned;
    :return: the text, with "_" instead of spaces, remove \ and * and ",
        and unescaped HTML encodings;
    """
    
    # Unescape the URL encodings;
    r = url_parser.unquote(r)
    r = html.unescape(r)

    # Replace with _
    r = r.replace('"', "")
    r = r.replace("'", "")
    r = r.replace("`", "")

    # Replace " " with "_", as well-formed DBPedia resources don't contain spaces;
    r = r.replace(" ", "_")
    
    # Remove "(disambiguation)" in page names,
    # but keep a trailing _ to preserve uniqueness in vertex names;
    r = r.replace("_(disambiguation)", "_")
    r = r.replace("_(disambiguation", "_")
    r = r.replace("_disambiguation_page", "_")
    r = r.replace("_disambiguation_", "_")
    r = r.replace("_disambiguation2", "_")
    r = r.replace(":disambiguation", "_")
    r = r.replace("/disambiguation", "_")
    r = r.replace("_disambiguation", "_")

    # Remove \;
    r = r.replace("\\", "")
    
    # Clean *;
    r = r.replace("*", "")
    
    return r

def clean_resource(r, ignore_literals=True, obtain_resource_manually=True):
    """
    :param r: string, an RDF resource;
    :param ignore_literals: if True, put object to empty strings if they aren't DBPedia resource.
        If False, parse them as if they were resources;
    :param obtain_resource_manually: if True, extract the resource by manually
        removing "http://dbpedia.org/resource" whenever it's possible. 
        If False, parse the URI as a path, and return the last part (i.e. the "filename");
    :return: an array of 3 strings, each a portion of the RDF triple;

    Given an input line that represents an RDF resource,
    clean the text that can be used by the graph builder;
    """
    if r[0] == "<" and r[-1] == ">":
        r = r[1:-1]

        if obtain_resource_manually and any(prefix in r for prefix in dbpedia_prefix):
            r = replace_prefix(r)
        else:
            r = os.path.basename(os.path.normpath(r))
    elif not ignore_literals:
        split_line = r.split('"')
        if len(split_line) > 1:
            r = split_line[1]
        else:
            r = split_line[0]
    else:
        r = ""
        
    r = clean_text(r)
    
    return r


def split_triple(l):
    """
    :param l: string, an RDF triple;

    Given an input line that represents an RDF triple,
    divide it in 3 parts.
    """
    # Each line is split in 3, and only the entity names are kept;
    line = l.strip()[:-1].strip().split(" ")
    if len(line) > 3:
        line[2] = " ".join(line[2:])
    # Drop the rest of the line;
    line[3:] = []

    return line


def clean_line(l, ignore_literals=True, obtain_resource_manually=True):
    """
    :param l: string, an RDF triple;
    :param ignore_literals: if True, put object to empty strings if they aren't DBPedia resource.
        If False, parse them as if they were resources;
    :param obtain_resource_manually: if True, extract the resource by manually
        removing "http://dbpedia.org/resource" whenever it's possible;
    :return: an array of 3 strings, each a portion of the RDF triple;

    Given an input line that represents an RDF triple,
    clean the text and divide it in 3 parts that can be used by the graph builder;
    """
    # Split the line;
    line = split_triple(l)

    # Clean the line;
    line = [clean_resource(r, ignore_literals=ignore_literals,
                           obtain_resource_manually=obtain_resource_manually) for r in line]
    
    return line
