# spatial-core
The prototype allows to generalize, automatically and based on previously defined functions, spatio-temporal events at 
different spatial levels of detail. The purpose of the prototype is to study how changing the spatial level of detail
affects our understanding about a phenomenon as well as the number of items in each spatial level of detail.  

The server was implemented in Java (using the REST API RESTeasy JAX-RS) and it is responsible for listening to client requests, processing them and 
retrieving the appropriate results. The browser-based client handles user interaction and data presentation and 
is written in Javascript, HTML5, WebGL, and it uses Leaflet to support the visualization of data on a map.

It handles with data on the following format (longitude, latitude, a1, ..., an), i.e., spatio-temporal events. 
The prototype was tested with data about accidents in USA and forest fires in Portugal

Then, it allows to choose the level of detail according the map zoom level. This way, according to the map zoom level, we may load much less data to client without affecting the analytical capability.

![Alt text](https://github.com/RFASilva/spatial-core/blob/master/screenshot.png "Screenshot Spatial-Core")

