package observatory

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.metadata.ImageMetadata
import scala.collection.parallel.CollectionConverters.given
import math._

/**
  * 3rd milestone: interactive visualization
  */
object Interaction extends InteractionInterface:

  /**
    * @param tile Tile coordinates
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(tile: Tile): Location =
    val (x, y, z) = (tile.x, tile.y, tile.zoom) 

    // Apply Web Mercator's Projection, converting tile to the topLeft location
    Location(
      toDegrees(atan(sinh(Pi*(1.0-2.0*y.toDouble/(1<<z))))), 
      x.toDouble/(1<<z)*360.0-180.0)

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @param tile Tile coordinates
    * @return A 256×256 image showing the contents of the given tile
    */
  def tile(
    temperatures: Iterable[(Location, Temperature)], 
    colors: Iterable[(Temperature, Color)], 
    tile: Tile): ImmutableImage =
    val (width, height) = (256, 256)

    // Create a "tile", or a square image subsection
    ImmutableImage
      .wrapPixels(
        width, 
        height, 
        // Construct a new array of pixels, based on tile
        Visualization
          .createPixelArray(
            temperatures, colors, 
            tile, width, height, 127), 
        ImageMetadata.empty
      )

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    * @param yearlyData Sequence of (year, data), where `data` is some data associated with
    *                   `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
    yearlyData: Iterable[(Year, Data)],
    generateImage: (Year, Tile, Data) => Unit
  ): Unit = 
    yearlyData
      .foreach((year, data) =>
        for zoom <- 0 to 3; x <- 1 to pow(2, zoom).toInt; y <- 1 to pow(2, zoom).toInt
        yield generateImage(year, Tile(x-1, y-1, zoom), data))
