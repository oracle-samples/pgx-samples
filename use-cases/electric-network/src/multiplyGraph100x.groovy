input_file_path  = "../data/electric_graph.edge"
output_file_path = "../data/electric_graph100x.edge"

offset  = 17179900000
size_factor = 100

file_writer = new FileWriter(new File(output_file_path), false)
buff_writer = new BufferedWriter(file_writer)

count = 0
new File(input_file_path).withReader { reader ->
    while ((line = reader.readLine())!=null) {
        line_elements = line.split(" ")
        if (line_elements[1] == "*") {
            for (i = 0; i < size_factor; i ++) {
                line_elements[0] = (line_elements[0].toLong() + offset * i).toString()
                line = line_elements.join(" ")
                buff_writer.write(line + "\n")
            }
        }
        else {
            for (i = 0; i < size_factor; i ++) {
                line_elements[0] = (line_elements[0].toLong() + offset * i).toString()
                line_elements[1] = (line_elements[1].toLong() + offset * i).toString()
                line = line_elements.join(" ")
                buff_writer.write(line + "\n")
            }
        }
        if (count == 1000)
        {
            buff_writer.flush()
        }
        count ++
    }
}

buff_writer.flush()
buff_writer.close()


println "Graph multiplied by " + size_factor + " and saved to " + output_file_path