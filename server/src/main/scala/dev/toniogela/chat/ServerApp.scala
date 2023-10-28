package dev.toniogela.chat

import cats.syntax.option.*
import cats.effect.{IO, IOApp}
import com.comcast.ip4s.*
import fs2.io.net.Network

object ServerApp extends IOApp.Simple:
  def run: IO[Unit] =
    for
      state       <- State.create
      socketStream = Network[IO].server(port = port"5555".some)
      _           <- Server.start(state, socketStream).compile.drain
    yield ()
