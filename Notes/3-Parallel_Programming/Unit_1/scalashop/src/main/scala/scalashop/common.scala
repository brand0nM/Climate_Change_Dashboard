package scalashop

import java.util.concurrent.*
import scala.util.DynamicVariable

import org.scalameter.*

// The value of every pixel is represented as a 32 bit integer
type RGBA = Int 
// Returns the red component
def red(c: RGBA): Int = (0xff000000 & c) >>> 24 
// Returns the green component
def green(c: RGBA): Int = (0x00ff0000 & c) >>> 16 
// Returns the blue component
def blue(c: RGBA): Int = (0x0000ff00 & c) >>> 8 
// Returns the alpha component
def alpha(c: RGBA): Int = (0x000000ff & c) >>> 0 
// Used to create an RGBA value from separate components
def rgba(r: Int, g: Int, b: Int, a: Int): RGBA =
  (r << 24) | (g << 16) | (b << 8) | (a << 0) 
// Restricts the integer into the specified range
def clamp(v: Int, min: Int, max: Int): Int =
  if v < min then min
  else if v > max then max
  else v 
// Image is a two-dimensional matrix of pixel values
class Img(val width: Int, val height: Int, private val data: Array[RGBA]):
  def this(w: Int, h: Int) = this(w, h, new Array(w * h))
  def apply(x: Int, y: Int): RGBA = data(y * width + x)
  def update(x: Int, y: Int, c: RGBA): Unit = data(y * width + x) = c 
// Computes the blurred RGBA value of a single pixel of the input image
def boxBlurKernel(src: Img, x: Int, y: Int, radius: Int): RGBA = 
  def computeBlur(pixels: IndexedSeq[RGBA]): RGBA =
    def average(s: Seq[Int]): Int = s.sum/s.length
     rgba( // Construct new RGBA value to represent pixel,
      average(pixels.map(red)), // Average surrounding pixels chanels
      average(pixels.map(green)), 
      average(pixels.map(blue)), 
      average(pixels.map(alpha)))
  val (min, maxX, maxY) = (0, src.width -1, src.height -1)
  // while i<= maxX
    // j = min
    // if clamp elem != elem 
      // i += 1
    // else
    // while j<= maxY

  val pixels = // Computed blured RGBA array of surrounding pixels
    for i <- -radius to radius; j <- -radius to radius // Iterate over radius
    yield src.apply( // Yield the subset of pixels to be considered in blur
      clamp(x+i, min, maxX),
      clamp(y+j, min, maxY))
  // Average each channels value
  computeBlur(pixels.distinct)


// Create an empty pool of Forks to be executed
val forkJoinPool = ForkJoinPool() 
// Abstract class for how to schedule each task
abstract class TaskScheduler:
  def schedule[T](body: => T): ForkJoinTask[T]
  def parallel[A, B](taskA: => A, taskB: => B): (A, B) =
    val right = task {
      taskB
    }
    val left = taskA
    (left, right.join()) 
// Default Task Scheduler instantiation
class DefaultTaskScheduler extends TaskScheduler:
  // Schedule tasks, or execute forks
  def schedule[T](body: => T): ForkJoinTask[T] =
    val t = new RecursiveTask[T] {
      def compute = body
    }
    Thread.currentThread match 
      // assign task to worker if unassigned
      case wt: ForkJoinWorkerThread =>
        t.fork()
      // else execute tasks on worker
      case _ =>
        forkJoinPool.execute(t)
    t
// Instantiated Schedule of tasks 
val scheduler = DynamicVariable[TaskScheduler](DefaultTaskScheduler())
// Method for adding new tasks to our scheduler
def task[T](body: => T): ForkJoinTask[T] = scheduler.value.schedule(body)
// Method for computing two tasks in parallel in scheduler
def parallel[A, B](taskA: => A, taskB: => B): (A, B) =
  scheduler.value.parallel(taskA, taskB)
// Method for computing four tasks in parallel in scheduler
def parallel[A, B, C, D](taskA: => A, taskB: => B, taskC: => C, taskD: => D): (A, B, C, D) =
  val ta = task { taskA }
  val tb = task { taskB }
  val tc = task { taskC }
  val td = taskD
  (ta.join(), tb.join(), tc.join(), td)
