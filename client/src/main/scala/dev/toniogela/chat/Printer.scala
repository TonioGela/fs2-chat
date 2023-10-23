package dev.toniogela.chat

import cats.effect.std.Console
import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.io.*
import scala.Console.{BLUE, GREEN, MAGENTA, RED, RESET}

trait Printer:
  def println(msg: String): IO[Unit]
  def info(msg: String): IO[Unit]
  def privateMessage(username: Username, message: String): IO[Unit]
  def alert(msg: String): IO[Unit]
  def errorln(msg: String): IO[Unit]
  def readLine: IO[Option[String]]

object Printer:

  def create: IO[Printer] = IO.delay {
    new Printer {

      def println(msg: String): IO[Unit] = Console[IO].println(msg)

      def info(msg: String): IO[Unit] = println(s"$BLUE*** $msg ***$RESET")

      def alert(msg: String): IO[Unit] = println(s"📢 $GREEN$msg$RESET")

      def privateMessage(username: Username, message: String) =
        println(s"$MAGENTA${username.name} says: $message$RESET")

      def errorln(msg: String): IO[Unit] = println(s"❌ $RED$msg$RESET")

      private val ESC: String = "\u001B"

      private val deleteLine: String = s"$ESC[2K"

      private val upLine: String = s"$ESC[F"

      def readLine: IO[Option[String]] =
        stdinUtf8[IO](1024).take(1).compile.foldMonoid.map(_.trim.some.filterNot(_.isBlank))
          .flatTap(_ => Console[IO].print(s"$upLine$deleteLine"))
          .handleErrorWith { case t => Console[IO].errorln(t) >> t.raiseError }
    }
  }

  extension (io: IO[Unit])
    inline def stream: Stream[IO, Unit] = Stream.exec(io)
