package observatory

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.metadata.ImageMetadata
import math.{sqrt, pow, ceil}

/**
  * 5th milestone: value-added information visualization
  */
object Visualization2 extends Visualization2Interface:

  /**
    * @param point (x, y) coordinates of a point in the grid cell
    * @param d00 Top-left value
    * @param d01 Bottom-left value
    * @param d10 Top-right value
    * @param d11 Bottom-right value
    * @return A guess of the value at (x, y) based on the four known values, using bilinear interpolation
    *         See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
    */
  def bilinearInterpolation(
    point: CellPoint,
    d00: Temperature,
    d01: Temperature,
    d10: Temperature,
    d11: Temperature
  ): Temperature =

    {d00*(1-point.x)*(1-point.y) + 
    d10*(point.x)*(1-point.y) + 
    d01*(1-point.x)*(point.y) + 
    d11*(point.x)*(point.y)}

  /**
    * @param grid Grid to visualize
    * @param colors Color scale to use
    * @param tile Tile coordinates to visualize
    * @return The image of the tile at (x, y, zoom) showing the grid using the given color scale
    */
  def visualizeGrid(
    grid: GridLocation => Temperature,
    colors: Iterable[(Temperature, Color)],
    tile: Tile
  ): ImmutableImage = ???
    // // This implementation is slow, 
    // // try using parallelization of lazy list techniques

    // val topLeft = Interaction.tileLocation(tile)
    // // Need an until? not sure how to handle boundaries in grid situation yet
    // val temperatures = {
    //   for x <- 0 to ceil(360.0/pow(2, tile.zoom)).toInt; y <- 0 to ceil(180.0/pow(2, tile.zoom)).toInt
    //     newX = (topLeft.lon + x).toInt
    //     newY = (topLeft.lat - y).toInt
    //   yield (Location(newY, newX), grid(GridLocation(newY, newX)))
    // }

    // Interaction.tile(temperatures, colors, tile)