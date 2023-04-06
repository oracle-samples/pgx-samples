# Oracle Graph Visualization Demo Application 

An example Java web application based on [Micronaut](https://docs.micronaut.io/) which embeds Oracle's Graph
Visualization library. The server queries the graph data from an Oracle Database using 
[PGQL](https://pgql-lang.org/).

The key source files to look at are

* `src/main/resources/public/index.html`: the HTML file served in the browser embedding the visualization library
* `src/main/java/com/oracle/example/HRController.java`: implements the REST endpoints called by `index.html` 
* `src/main/java/com/oracle/example/GraphClient.java`: wraps the graph server APIs, called by HRController


## Pre-requisites

1. Oracle JDK 11 (or OpenJDK 11)
2. A running Oracle Graph Server. Download [from oracle.com](https://www.oracle.com/database/technologies/spatialandgraph/property-graph-features/graph-server-and-client/graph-server-and-client-downloads.html) 
   and install [as per documentation](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgdg/deploying-graph-visualization-application.html).
3. A running Oracle Database (e.g. [Autonomous Database](https://www.oracle.com/autonomous-database/))
4. Import the Human Resources dataset and create a PG View by running `./gradlew createHrDatasetAndPgView -Pjdbc_url=<jdbc_url> -Pusername=<username> -Ppassword=<password>`

* Note: The user must have `GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE grants on the Database`
* Note 2: You can drop the HR dataset and the created PG View by running the following command: `./gradlew dropHrDatasetAndPgView -Pjdbc_url=<jdbc_url> -Pusername=<username> -Ppassword=<password>`

## Usage

1. Clone this repository 
2. Download the "Oracle Graph Visualization library" [from oracle.com](https://www.oracle.com/database/technologies/spatialandgraph/property-graph-features/graph-server-and-client/graph-server-and-client-downloads.html)
3. Unzip the library into the `src/main/resources/public` directory. For example:

```
unzip oracle-graph-visualization-library-23.1.0.zip -d src/main/resources/public/
```

4. Run the following command to start the example app locally:

```
./gradlew run --args='-oracle.graph-server.url=<graph-server-url> -oracle.graph-server.jdbc-url=<jdbc-url> -oracle.graph-server.username=<username> -oracle.graph-server.password=<password>'
```

with

* `<graph-server-url>` being the URL of the Graph Server, e.g. `https://myhost:7007`
* `<jdbc-url>` being the JDBC URL of the Oracle Database the Graph Server should connect to, e.g. `jdbc:oracle:thin:@myhost:1521/orcl` 
* `<username>` being the Oracle Database username to authenticate the example application with the Graph Server, e.g. `scott`
* `<password>` being the Oracle Database password to authenticate the example application with the Graph Server, e.g. `tiger`

Then open your browser at `http://localhost:8080`.

When you click on the <em>Query</em> button, a request is made to `/hr/neighbors`, which fetches the direct reports of 
the given employee (by default `SKING`) from the HR graph using a PGQL query. 

![](screenshot.jpg)

When you right-click on one of the resulting nodes and then select <em>Expand</em>, a request to `/hr/directs` is being 
made, which fetches the neighbors of that node via another PGQL query.

## Troubleshooting

If you get any errors, 
* check the log output from the server on the terminal where the Gradle command is running
* use browser debug tools (e.g. Chrome Developer Tools) to inspect request/response and console logs