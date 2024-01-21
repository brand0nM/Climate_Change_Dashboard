package scalashop

import org.scalameter.*

object HorizontalBoxBlurRunner:

  val standardConfig = config(
    Key.exec.minWarmupRuns := 5,
    Key.exec.maxWarmupRuns := 10,
    Key.exec.benchRuns := 10,
    Key.verbose := false
  ) withWarmer(Warmer.Default())

  def main(args: Array[String]): Unit =
    val radius = 3
    val width = 1920
    val height = 1080
    val src = Img(width, height)
    val dst = Img(width, height)
    val seqtime = standardConfig measure {
      HorizontalBoxBlur.blur(src, dst, 0, height, radius)
    }
    println(s"sequential blur time: $seqtime")

    val numTasks = 32
    val partime = standardConfig measure {
      HorizontalBoxBlur.parBlur(src, dst, numTasks, radius)
    }
    println(s"fork/join blur time: $partime")
    println(s"speedup: ${seqtime.value / partime.value}")

// Traverses Image pixels in parallel, modifying existing by given blur
object HorizontalBoxBlur extends HorizontalBoxBlurInterface:
  /** Blurs the columns of the source image `src` into the destination image
   * blur` traverses the pixels by going from left to right */
  def blur(src: Img, dst: Img, from: Int, end: Int, radius: Int): Unit = 
    for pixelX <- (0 until src.width); pixelY <- (from until end)
    yield dst.update(pixelX, pixelY, boxBlurKernel(src, pixelX, pixelY, radius))

  // Blurs the columns of the source image in parallel using `numTasks` tasks
  def parBlur(src: Img, dst: Img, numTasks: Int, radius: Int): Unit = 
    val taskLengths = src.width/numTasks max 1
    val newTup = 
      for specTask <- (0 until numTasks) by taskLengths
      yield task {
        blur(src, dst, specTask, specTask + taskLengths, radius)
      }
    newTup.map(_.join)
