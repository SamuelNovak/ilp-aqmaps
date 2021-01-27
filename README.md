# Informatics Large Practical - Air Quality Maps

This project was a coursework assignment in the course Informatics Large Practical at the University of Edinburgh.
*(Marks not released yet)*

## Description

The problem is as follows: There is a (fictional) research team evaluating air quality in an urban area. For a pilot project, they set up a collection of sensors in and around the Central Campus of the University of Edinburgh, and they need an autonomous drone to visit those sensors and collect their readings.

My task was to design and implement the software controlling this drone, including navigation and control. Finally, the program simulated the drone's flight and exported the collected data as a precise log of the drone's operation, as well as a colour-coded map showing its trajectory and air quality readings.

The geographical data (no-fly zones, sensor locations) were loaded from a local web server running on the drone.

You can read more about my solution in the [Report](ilp-report.pdf). Below are a few example flights of the drone:

| <img src="/report/pictures/01-01-2020.png" alt="Flight path 01-01-2020" title="Flight path 01-01-2020" width="400" /> | <img src="/report/pictures/05-05-2020.png" alt="Flight path 05-05-2020" title="Flight path 05-05-2020" width="400" /> |
| --- | --- |
| <img src="/report/pictures/07-07-2020.png" alt="Flight path 07-07-2020" title="Flight path 07-07-2020" width="400" /> | <img src="/report/pictures/25-12-2020.png" alt="Flight path 25-12-2020" title="Flight path 25-12-2020" width="400" /> |



## Technical details

This project uses the Maven build system. Dependencies include MapBox GEOJson and Google GSON.

In order to run the project, one must first start up the web server by running the following command in the [WebServer](/WebServer) directory:
```
java -jar <path>/WebServerLite.jar <web-root> <port>
```

For example, usually you would `cd WebServer` and run
```
java -jar WebServerLite.jar . 8888
````

*Note: The WebServer is included here exactly as it was given to us by the Course Organizers in ILP.*

Then you can run the program (which should, after a successful Maven build, be located in the `target/` directory) by specifycing the following arguments:
```
java -jar <path>/aqmaps-0.0.1-SNAPSHOT.jar <day> <month> <year> <starting-latitude> <starting-longitude> <randomness-seed> <server-port>
```
where the day/month/year can be any date from the years 2020 and 2021, and the port has to be the same as the port on which WebServerLite.jar is running.

In the usual case, this would be (assuming we are in the root directory of the project)
```
java -jar target/aqmaps-0.0.1-SNAPSHOT.jar <day> <month> <year> <starting-latitude> <starting-longitude> <randomness-seed> 8888
```
