package observatory

import java.time.LocalDate
import scala.io.Source
import org.apache.spark._
import org.apache.log4j.{Level, Logger}

/**
  * 1st milestone: data extraction
  */
object Extraction extends ExtractionInterface:

  val conf = new SparkConf().setAppName("Observatory").setMaster("local[2]")
  val sc = new SparkContext(conf) 
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Year, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Temperature)] = 
    // Convert Fahrenheit
    def toCelcius(temp: Double): Double = 5*(temp-32)/9

    // Create Dataframe for stations
    val stations = sc
      .parallelize(
        Source
          .fromInputStream(getClass.getResourceAsStream(stationsFile), "utf-8")
          .getLines
          .toSeq)
      .map(row =>
        val splitted = row.split(",")
        val location = if (splitted.indices.last>2) 
          new Location(splitted(2).toDouble, splitted(3).toDouble) 
          else new Location(-1000.0, -1000.0)
        ((splitted(0), splitted(1)), location))
      // Remove stations with no coordinates
      .filter(_._2.lat != -1000.0)

    // Create Dataframe for temperatures
    val temperatures = sc
      .parallelize(
        Source
          .fromInputStream(getClass.getResourceAsStream(temperaturesFile), "utf-8")
          .getLines
          .toSeq)
      .map(row => 
        val splitted = row.split(",")
        ((splitted(0), splitted(1)),
          (LocalDate.parse(year.toString+"-"+splitted(2)+"-"+splitted(3)), 
          toCelcius(splitted(4).toDouble))))

    // Merge stations and temperatures dataframes
    stations
      .join(temperatures)
      // Get (LocalDate, Location, Temperature) for each entry
      .map(row => (row._2._2._1, row._2._1, row._2._2._2))
      // Collect column as Array
      .collect()

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Temperature)]): Iterable[(Location, Temperature)] =
    records
      // group by location, and map to a collection of experienced temperatures. Then reduce by summing
      .groupMapReduce(_._2)(v => (v._3, 1.0))((a,b) => (a._1 + b._1, a._2 + b._2))
      .toArray
      // compute average
      .map((loc, temp) => (loc, temp._1/temp._2))
