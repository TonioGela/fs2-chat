package dev.toniogela.chat

import fs2.*
import fs2.io.*
import cats.syntax.all.*
import cats.effect.*
import cats.effect.std.*
import scala.concurrent.duration.*

object Client extends IOApp.Simple:

  def run: IO[Unit] =
    for
      a <- readLine.foreverM.start
      b <- printNext.foreverM.start
      _ <- (a.join, b.join).parSequence_
    yield ()

def printNext: IO[Unit] = Console[IO].println("foo > hi!") >> IO.sleep(5.seconds)

def readLine: IO[Unit] = stdin[IO](1024).through(stdout[IO]).compile.drain >> readLine
