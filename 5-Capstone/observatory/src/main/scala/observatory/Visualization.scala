package observatory

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.implicits.given
import scala.collection.parallel.CollectionConverters.given
import math._

/**
  * 2nd milestone: basic visualization
  */
object Visualization extends VisualizationInterface:

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(
    temperatures: Iterable[(Location, Temperature)], 
    location: Location): Temperature =

    // Get distance
    def distance(a: Location, b: Location) =
      val radius = 6371
      if ((a._1+b._1, a._2+b._2) == (0, 0))
        radius * Pi
      else 
        val λ1 = toRadians(a.lon)
        val ϕ1 = toRadians(a.lat)
        val λ2 = toRadians(b.lon)
        val ϕ2 = toRadians(b.lat)
        val Δλ = abs(λ1 - λ2)
        val Δσ = acos(sin(ϕ1) * sin(ϕ2) + cos(ϕ1) * cos(ϕ2) * cos(Δλ))
        radius*Δσ

    // Find the temperature at our station
    temperatures.filter(_._1==location) match {
      // If location is a station, use avg temp
      case l if l.nonEmpty => l.head._2
      // Else interpolate to find temperature
      case _ => 
        // Closest stations to location 
        val closestStations = temperatures
          .map((loc, temp) => (distance(loc, location), loc, temp))
          .toArray
          .sortBy(tup => tup._1)
        // If closest station is less than 1km away 
        if (closestStations.head._1<=1) then
          closestStations.head._3
        // Else weight iversly proprtionaly by distance
        else 
          val weightedTemp = closestStations
            // Inverse Weighted Distance
            .map(tup => (tup._3/tup._1, 1/tup._1))
            .reduce((a, b) => (a._1 + b._1, a._2 + b._2))
          // Get the avg of each weighted temp  
          weightedTemp._1/weightedTemp._2
    }

  /**
    * Takes a Sequence of Reference Temperatures Color Pairs and a Temp Value
    * @param points Pairs containing a value and its associated color
    * @param value The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(
    points: Iterable[(Temperature, Color)], 
    value: Temperature): Color =

    // Get a range of temperatures considered is between
    def getRange():((Temperature, Color), (Temperature, Color)) = 
      // Instantiate a the Range
      var (rngBeg, rngEnd) = (points.minBy(_._1), points.maxBy(_._1))
      // Get minimum and max Temperatures
      val (minTemp, maxTemp) = (rngBeg._1, rngEnd._1)

      // If the temperature is greater than our max Buxket
      if (value >= maxTemp) then
        // Assign range to Max Bucket
        rngBeg = rngEnd
      // If the temperature is Less than our min Bucket
      else if (value <= minTemp) then
        // Assign range to Min Bucket
        rngEnd = rngBeg
      else 
        // Find the largest Bucket our temperature is greater than
        rngBeg = points.filter(_._1 <= value).maxBy(_._1)
        // Find the smallest Bucket our temperature is less than
        rngEnd = points.filter(_._1 >= value).minBy(_._1)

      (rngBeg, rngEnd)

    // Get range of values temperature is between
    val (rngBeg, rngEnd) = getRange()
    // Interpolate new Color in range
    def linInter(begCol: Int, endCol: Int): Int =
      // In case range is flipped make channels positve Ints
      abs(round(
        // Find Temperatures Distance from end points and weight by color
        (begCol*(rngEnd._1 - value) + endCol*(value - rngBeg._1)
        // True Difference in Temperatures
        )/(rngEnd._1 - rngBeg._1)).toInt)

    // Return True Color
    rngBeg match
      // If range is on a Boundary, return its color
      case beg if beg==rngEnd => beg._2
      // Else return the interpolated color
      case _ => {
        Color(
          linInter(rngBeg._2.red, rngEnd._2.red), 
          linInter(rngBeg._2.green, rngEnd._2.green), 
          linInter(rngBeg._2.blue, rngEnd._2.blue))
      }


  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @param topLeft coordinate of Tile's start
    * @param width in pixels of Tile
    * @param height in pixels of Tile
    * @param alpha/transparancy of pixel
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def createPixelArray(
    temperatures: Iterable[(Location, Temperature)], 
    colors: Iterable[(Temperature, Color)],
    tile: Tile,
    width: Int, 
    height: Int,
    alpha: Int): Array[Pixel] = 

    // Convert tile to a location
    val topLeft = Interaction.tileLocation(tile)

    // Create an array of pixels
    {for column <- 0 until width; row <- 0 until height
      // Interpolate Color of Predicted Temperature for each Coordinate
      color = interpolateColor(
        colors, 
        // Predict temperature of location
        predictTemperature(
          temperatures,
          // Create Geographical Coordinate from pixel range
          Location(
            // Starting y-coordinate subtract sections of lat
            topLeft.lat-row*180.0/(pow(2, tile.zoom)*height),
            // Starting x-coordinate add sections of lon
            column*360.0/(pow(2, tile.zoom)*width)+topLeft.lon
          )
        )
      )
    // Construct Pixel with interpolated color 
    yield Pixel(row, column, color.red, color.green, color.blue, alpha)
    }.toArray

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(
    temperatures: Iterable[(Location, Temperature)], 
    colors: Iterable[(Temperature, Color)]): ImmutableImage =

    // Create image where each pixel represents a geographical coordinate
    val (width, height) = (360, 180)
    // Return an image
    ImmutableImage
      .wrapPixels(
        width, 
        height, 
        // Use the base tile to construct an array of pixels
        createPixelArray(
          temperatures, colors, 
          Tile(0, 0, 0), 
          width, height, 255), 
      ImageMetadata.empty
    )




    // For loop isnt in parallel good alt since pixelArray === pixels
    // val pixelArray = (0 until (width*height))
    //   // .par
    //   .map(point => 
        // // Convert pixel coord to location coord
        // val rows = (point/width).toInt; val rem = point - width*rows
        // val coord = Location((rem - 180).toDouble, (90 - rows).toDouble)
        // // Predict Temperature From Coordinate
        // val predictTemp = predictTemperature(temperatures, coord)
        // // Interpolate Color and convert to Pixel
        // val color = interpolateColor(colors, predictTemp)

    //     (coord, Pixel(coord.lat.toInt, coord.lon.toInt, color.red, color.green, color.blue, 1)))
    //   .sortBy(-_._1.lat)
    //   .map(_._2)
    //   .toArray
