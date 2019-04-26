/**
 * Copyright (C) 2013 - 2019 Oracle and/or its affiliates. All rights reserved.
 */

// This script generates graph in EDGE_LIST format from the datasets in:
/// NodeData-8500.csv
// ConnectData-8500.csv
// SwitchConfig-8500.csv
//
// The resulting graph will be stored in:
// electric_graph.edge

// Get dataset paths
nodes_file_path = "../data/NodeData-8500.csv"
connections_file_path = "../data/ConnectData-8500.csv"
switch_config_file_path = "../data/SwitchConfig-8500.csv"

// Configure path to save the generated graph
output_file_path = "../data/electric_graph.edge"

// NodeData-8500.csv related property indexes
idx_node_id = 0 // "Node ID"
idx_node_nick = 1 // "Nickname"
idx_node_lat = 2 // "Latitude"
idx_node_lon = 3 // "Longitude"
idx_node_parent = 4 // "Node Parent"
idx_node_volts = 5 // "Base Volts"
idx_node_curr = 6 // "Base Current"
idx_node_power = 7 // "Base Power"
idx_node_conf = 8 // "Configuration"
idx_node_remote = 9 // "RemoteControlAvailable"
idx_node_segment = 10 // "SegmentId"
idx_node_upstream = 11 // "Upstream Connection"
idx_node_downstream = 12 // "Downstream Connection"
idx_node_phase = 13 // "Phase"
idx_node_nominal = 14 // "Nominal Feeder"

// ConnectData-8500.csv related property indexes
idx_connection_id = 0 // "Connect ID"

// SwitchConfig-8500.csv related property indexes
idx_switch_conf = 0 // "Configuration"
idx_switch_default = 1 // "Normal Position"

// Define a buffered file writer to save the graph to a file
file_writer = new FileWriter(new File(output_file_path), false)
buff_writer = new BufferedWriter(file_writer)

// Build a switch configuration HashMap called switch_map. This HashMap helps
// to set the correct value for every node's "switch_default" property
// in accordance to the value of its "node_conf" property. So, if a node
// has a "node_conf" property set as a switch, switch_map will aid in
// providing its default configuration.
first_line = true
switch_map = new HashMap<String, Boolean>() // keeps track of default switch configurations
new File(switch_config_file_path).splitEachLine(',') { row ->
  if (first_line) {
    first_line = false
  } else {
    switch_conf = row[idx_switch_conf]
    switch_default = row[idx_switch_default]
    if (switch_default == '"open"') {
      switch_default = false // Deny pass
    } else {
      switch_default = true // Allow pass
    }
    switch_map.put(switch_conf, switch_default)
  }
}

// Add "device" nodes to the graph file
first_line = true
new File(nodes_file_path).splitEachLine('","') { row ->
  if (first_line) {
    first_line = false
  } else {
    // Extract node properties from each row
    node_id = row[idx_node_id]
    node_nick = row[idx_node_nick]
    node_lat = row[idx_node_lat]
    node_lon = row[idx_node_lon]
    node_parent = row[idx_node_parent]
    node_volts = row[idx_node_volts]
    node_curr = row[idx_node_curr]
    node_power = row[idx_node_power]
    node_conf = row[idx_node_conf]
    node_remote = row[idx_node_remote]
    node_segment = row[idx_node_segment]
    node_upstream = row[idx_node_upstream]
    node_downstream = row[idx_node_downstream]
    node_phase = row[idx_node_phase]
    node_nominal = row[idx_node_nominal]
    switch_default = true // Asume nodes allow pass, this will be changed
    // later for switches set to false by default

    connection_id = 0 // Since the node is not a connection node this
    // property is set to 0

    node_class = '"Device"' // Device/Connection/Switch

    // Remove spurious quotes
    node_id = node_id.replaceAll('"', '')
    node_nominal = node_nominal.replaceAll('"', '')

    // Make sure string properties are between quotes
    node_nick = '"' + node_nick + '"'
    node_conf = '"' + node_conf + '"'
    node_segment = '"' + node_segment + '"'
    node_upstream = '"' + node_upstream + '"'
    node_downstream = '"' + node_downstream + '"'
    node_phase = '"' + node_phase + '"'

    // If no geographical information is defined then define default impossible coordinates
    if (node_lat == "" || node_lon == "") {
      node_lat = "200"
      node_lon = "200"
    }

    // If the "node_parent" property is not set, then set it to-1
    if (node_parent == "") {
      node_parent = "-1"
    }

    // If the "nominal_feeder" property for a node is not defined, then set it to -1
    if (node_nominal == "") {
      node_nominal = "-1"
    }

    // If the node's "node_conf" property corresponds to a switch, then it should
    // be defined in the switch_map HashMap. If the switch_map maps the value of
    // "node_conf" to false, then set the "switch_default" property to false.
    if (switch_map.get(node_conf) == false) {
      switch_default = false
    }

    // If the node is a switch, then set the class to "Switch"
    if (switch_map.get(node_conf) != null) {
      node_class = '"Switch"'
    }

    // Compose the line that will be writen to the graph file
    line = node_id + " * " \
         + "{" + node_class + "} " \
         + node_nick + " " \
               + node_lat + " " \
               + node_lon + " " \
               + node_parent + " " \
               + node_volts + " " \
               + node_curr + " " \
               + node_power + " " \
               + node_conf + " " \
               + node_remote + " " \
               + node_segment + " " \
               + node_upstream + " " \
               + node_downstream + " " \
               + node_phase + " " \
               + node_nominal + " " \
               + switch_default + " " \
               + connection_id + "\n"

    // Write line to file
    buff_writer.write(line)
  }
}
// Flush the buffer of the buffered file writer
buff_writer.flush()

// Add "connection" nodes to the graph file
first_line = true
new File(connections_file_path).splitEachLine(',') { row ->
  if (first_line) {
    first_line = false
  } else {
    // Extract node properties from each row.
    // Connection nodes set all of its properties to their default
    // values except for "node_id", "connection_id" and "node_class".
    node_id = row[idx_connection_id]
    node_nick = '"null"'
    node_lat = 200
    node_lon = 200
    node_parent = -1
    node_volts = -1
    node_curr = -1
    node_power = -1
    node_conf = '"null"'
    node_remote = false
    node_segment = '"null"'
    node_upstream = '"null"'
    node_downstream = '"null"'
    node_phase = '"null"'
    node_nominal = -1
    switch_default = true // True == allow pass, False == deny pass
    connection_id = row[idx_connection_id]
    node_class = '"Connection"' // Device/Connection/Switch

    // Remove spurious quotes
    node_id = node_id.replaceAll('"', '')
    connection_id = connection_id.replaceAll('"', '')

    // Compose the line that will be writen to the graph file
    line = node_id + " * " \
         + "{" + node_class + "} " \
               + node_nick + " " \
               + node_lat + " " \
               + node_lon + " " \
               + node_parent + " " \
               + node_volts + " " \
               + node_curr + " " \
               + node_power + " " \
               + node_conf + " " \
               + node_remote + " " \
               + node_segment + " " \
               + node_upstream + " " \
               + node_downstream + " " \
               + node_phase + " " \
               + node_nominal + " " \
               + switch_default + " " \
               + connection_id + "\n"

    // Write line to file
    buff_writer.write(line)
  }
}
// Flush the buffer of the buffered file writer
buff_writer.flush()

// Add "connection" edges to the graph file
first_line = true
new File(connections_file_path).splitEachLine(',') { row ->
  if (first_line) {
    first_line = false
  } else {
    edge_source = row[idx_connection_id].replaceAll('"', '')

    // Iterate over nodes listed for each connection row
    // Remember that each connection node is linked with
    // at most 8 devices.
    for (i = 1; i < 9; i++) {
      if (row[i] != '""') {
        edge_sink = row[i].replaceAll('"', '')

        // Compose line to be writen to file
        line = edge_source + " " \
                         + edge_sink + "\n"

        // Write line to file
        buff_writer.write(line)
      }
    }
  }
}
// Flush the buffer of the buffered file writer and close it
buff_writer.flush()
buff_writer.close()

println "Type exit to quit"

