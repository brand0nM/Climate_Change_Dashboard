package observatory

import scala.collection.parallel.CollectionConverters.given
import math.{round, abs}

/**
  * 4th milestone: value-added information
  */
object Manipulation extends ManipulationInterface:

  /**
    * @param temperatures Known temperatures
    * @return A function that, given a latitude in [-89, 90] and a longitude in [-180, 179],
    *         returns the predicted temperature at this location
    */
  def makeGrid(temperatures: Iterable[(Location, Temperature)]): GridLocation => Temperature =
    (grid: GridLocation) => Visualization.predictTemperature(temperatures, Location(grid.lat, grid.lon))

  /**
    * @param temperaturess Sequence of known temperatures over the years (each element of the collection
    *                      is a collection of pairs of location and temperature)
    * @return A function that, given a latitude and a longitude, returns the average temperature at this location
    */
  def average(temperaturess: Iterable[Iterable[(Location, Temperature)]]): GridLocation => Temperature =
    (grid: GridLocation) => {
      val red = temperaturess
        .map(year => (makeGrid(year)(grid), 1))
        .reduce((a, b) => (a._1+b._1, a._2+b._2))

      red._1/red._2
    }


  /**
    * @param temperatures Known temperatures
    * @param normals A grid containing the “normal” temperatures
    * @return A grid containing the deviations compared to the normal temperatures
    */
  def deviation(temperatures: Iterable[(Location, Temperature)], normals: GridLocation => Temperature): GridLocation => Temperature =
    (grid: GridLocation) => makeGrid(temperatures)(grid) - normals(grid) 
