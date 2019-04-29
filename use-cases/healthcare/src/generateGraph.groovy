"Creating Dictionaries"
readyToProcess = false
doctors = new HashMap<Integer,List>()
hcpcs = new HashSet<Integer>()
specialties = new HashSet<String>()
inputFile = "../data/Medicare_Provider_Util_Payment_PUF_CY2012.txt"
outputFile = "../data/bipartiteMedicGraph.txt"
specialtiesOutFile = "../data/specialties.txt"
npiPosition = -1
specPosition = -1
hcpcsPosition = -1

new File(inputFile).splitEachLine("\t") { fields ->
    if (readyToProcess) {
        if (!doctors.containsKey(fields[npiPosition])) {
            doctors.put(fields[npiPosition],[new HashSet<Integer>(),fields[specPosition]])
        }
        if (!fields[hcpcsPosition].isInteger()) {
            newHcpcs = ""
            for (int i = 0; i < fields[hcpcsPosition].length(); i++) {
                if (String.valueOf(fields[hcpcsPosition].charAt(i)).isInteger()) {
                    newHcpcs = newHcpcs + fields[hcpcsPosition].charAt(i)
                }
                else{
                    newHcpcs = newHcpcs + ((int)(fields[hcpcsPosition].charAt(i)))
                }
            }
            fields[hcpcsPosition] = newHcpcs
        }
        specialties.add(fields[specPosition])
        hcpcsValue = Integer.parseInt(fields[hcpcsPosition])
        hcpcs.add(hcpcsValue)
        doctors.get(fields[npiPosition])[0].add(hcpcsValue)
    } else {
        /*
        * We are using this line because the first row is the header and in the
        * second we see the copyright, therefore we are not going to process that
        */
        readyToProcess = (fields.size() == 2)
        if (npiPosition < 0 || specPosition < 0 || hcpcsPosition < 0) {
            npiPosition = fields.indexOf("NPI")
            specPosition = fields.indexOf("PROVIDER_TYPE")
            hcpcsPosition = fields.indexOf("HCPCS_CODE")
        }
    }
}

"Creating files"
ln = System.getProperty("line.separator")
graphFile = new File(outputFile)

if (graphFile.exists()) {
    graphFile.delete()
    graphFile.createNewFile()
}

append = true
fileWriter = new FileWriter(graphFile, append)
buffWriter = new BufferedWriter(fileWriter)

for (element in hcpcs.iterator()) {
    buffWriter.write(element + " * \"HCPCS\"" + ln)
}

for (key in doctors.keySet()) {
    buffWriter.write(key + " * \"" + doctors.get(key)[1] + "\"" + ln)
}

for (key in doctors.keySet()) {
    for (key2 in doctors.get(key)[0].iterator()) {
        buffWriter.write(key + " " + key2 + ln)
    }
}

buffWriter.flush()
buffWriter.close()

specialtiesFile = new File(specialtiesOutFile)

if (specialtiesFile.exists()) {
    specialtiesFile.delete()
    specialtiesFile.createNewFile()
}

fileWriter = new FileWriter(specialtiesFile, append)
buffWriter = new BufferedWriter(fileWriter)

"Found " + specialties.size().toString() + " specialties, writing them to file"
for (spec in specialties.iterator()) {
    buffWriter.write(spec + ln)
}

buffWriter.flush()
buffWriter.close()