# Climate Change Dashboard
## Overview
Implement a complete application processing several gigabytes of data. 
### Purpose
Create an interactive dashboard of climate data to visualize the evolution of temperatures and temperature deviations over time.
Use weather station data to interpolate the average temperature of each point on the globe over the past ten years- coloring in acordance to its temperature. 
Then  display the user's selection with Functional Reactive Programming.
## Structure
### Data Extraction - Extraction.scala
Collected 2 csv from the [National Center for Environmental Information](https://www.ncei.noaa.gov/):

1) Weather station’s locations: stations.csv file
   - STN identifier
   - WBAN identifier
   - Latitude
   - Longitude
2) Temperature records for a year: 1975.csv, 1976.csv, etc.
   - STN identifier
   - WBAN identifier
   - Month
   - Day
   - Temperature (in degrees Fahrenheit)
     
### Raw Data Visualization - Visualization.scala
3 algorithms we must create:

1) *Spatial Interpolation*: 
To find the temperature at a coordinate not located at a weather station, we must interpolate its
true distance around the earth

3) *Linear Interpolation*:
To determine what color each pixel should be represented as.

4) *Visualization*: 
Now we'll build an image using Java's scrimage 

### Interactive visualization in a Web app - Interaction.scala
To efficiently load the required data for our application we have to decompose this large dataset. 

Following our intuition about GIS data we'll use Mercator projection to represent the earth as pixels. 
We'll create an image (composed of tiles) that represents a subsection of our projected map. 
Then, tiles will be computed based on their initial value.

The next implementation in Interation2.scala will allow a user to actively filter and change which tiles are
computed and displayed.

### Data Manipulation - Manipulation.scala
This Module is used to Spatially interpret the temperature of what each pixel should be represented as.
To do this we have to implement 2 methods

1) *Average*: computes the average temperature of the tiles color representation 
2) *Deviation*: computes the deviations from its normal temperature

### Value-added information visualization - Visualization2.scala
To speed up the load time of our application we must implement a new algorithm.
Rather than using inverse distance weighting to calculate a tile value, we'll use 
bilinear interpolation to weigh the value based on existing tiles. 

### Interactive user interface - Interation2.scala
To finish the GUI we'll allow the user the select between two layers called temperatures (average) or deviations.
Then we'll add a legend giving the array of deviations- or temperatures- and their respective color representation.
Finally, we'll add a slider that allows the user to select the year.
To make the GUI reactive we'll store these variables in signals.

### Dashboard Demo
![Demo](https://github.com/brand0nM/Scala_Course/assets/79609464/b3954635-580a-405b-b179-60fe4e9ca618)

## Summary
Using Only Scala we've made a web application that efficiently displays temperatures or deviations based on the
user's selections. 
