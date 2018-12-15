package cats.collections

import cats.{Order, Show}

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

/**
 * Represent an inclusive range [x, y] that can be generated by using discrete operations
 */
final case class Range[A](val start: A, val end: A) {

  /**
   * Subtract a Range from this range.
   * The result will be 0, 1 or 2 ranges
   */
  def -(range: Range[A])(implicit enum: Discrete[A], order: Order[A]): Option[(Range[A], Option[Range[A]])] =
    if(order.lteqv(range.start, start)) {
      if(order.lt(range.end, start))
        Some((this, None))  // they are completely to the left of us
      else if(order.gteqv(range.end, end))
        // we are completely removed
        None
      else Some((Range(enum.succ(range.end), end), None))
    } else {
      if(order.gt(range.start, end))
        Some((this, None)) // they are completely to the right of us
      else {
        val r1 = Range(start, enum.pred(range.start))
        val r2: Option[Range[A]] = if(order.lt(range.end, end)) Some(Range(enum.succ(range.end), end)) else None
          Some((r1,r2))
      }
    }

  def +(other: Range[A])(implicit order: Order[A], enum: Discrete[A]): (Range[A], Option[Range[A]]) = {
    val (l,r) = if(order.lt(this.start,other.start)) (this,other) else (other,this)

    if(order.gteqv(l.end, r.start) || enum.adj(l.end, r.start))
      (Range(l.start, order.max(l.end,r.end)), None)
    else
      (Range(l.start, l.end), Some(Range(r.start,r.end)))

  }

  def &(other: Range[A])(implicit order: Order[A]): Option[Range[A]] = {
    val start = order.max(this.start, other.start)
    val end = order.min(this.end, other.end)
    if(order.lteqv(start,end)) Some(Range(start,end)) else None
  }

  /**
    * Verify that the passed range is a sub-range
    */
  def contains(range: Range[A])(implicit order: Order[A]): Boolean =
    order.lteqv(start, range.start) && order.gteqv(end, range.end)

  /**
    * Return all the values in the Range as a List
    */
  def toList(implicit enum: Discrete[A], order: Order[A]): List[A] = {
    @tailrec def inner(buffer: ListBuffer[A], range: Range[A]): ListBuffer[A] = {
      order.compare(range.start, range.end) match {
        case 0 =>
          buffer.append(range.start)
          buffer
        case x if x < 0 =>
          buffer.append(range.start)
          inner(buffer, Range(enum.succ(range.start), end))
        case _ =>
          buffer.append(range.start)
          inner(buffer, Range(enum.pred(range.start), end))
      }
    }
    inner(ListBuffer.empty, this).toList
  }

  /**
    * Returns range [end, start]
    */
  def reverse: Range[A] = Range(end, start)

  /**
    * Verify is x is in range [start, end]
    */
  def contains(x: A)(implicit A: Order[A]): Boolean = A.gteqv(x, start) && A.lteqv(x, end)

  /**
    * Apply function f to each element in range [star, end]
    */
  def foreach(f: A => Unit)(implicit enum: Discrete[A], order: Order[A]): Unit = {
    var i = start
    while(order.lteqv(i,end)) {
      f(i)
      i = enum.succ(i)
    }
  }

  def map[B](f: A => B): Range[B] = Range[B](f(start), f(end))

  /**
    * Folds over the elements of the range from left to right; accumulates a value of type B
    * by applying the function f to the current value and the next element.
    */
  def foldLeft[B](s: B, f: (B, A) => B)(implicit discrete: Discrete[A], order: Order[A]): B = {
    var b = s
    foreach { a =>
      b = f(b,a)
    }
    b
  }

  /**
    * Folds over the elements of the range from right to left; accumulates a value of type B
    * by applying the function f to the current value and the next element.
    */
  def foldRight[B](s: B, f: (A, B) => B)(implicit discrete: Discrete[A], order: Order[A]): B =
    reverse.foldLeft(s, (b: B, a: A) => f(a, b))(discrete.inverse, Order.reverse(order))
}

object Range {
  implicit def rangeShowable[A](implicit s: Show[A]): Show[Range[A]] = new Show[Range[A]] {
    override def show(f: Range[A]): String = {
      val (a, b) = (s.show(f.start), s.show(f.end))
      s"[$a, $b]"
    }
  }

}
