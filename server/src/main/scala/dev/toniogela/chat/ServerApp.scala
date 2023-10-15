package dev.toniogela.chat

import cats.syntax.option.*
import cats.effect.{ExitCode, IO}
import cats.effect.std.Console
import com.comcast.ip4s.Port
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import fs2.io.net.Network

object ServerApp extends CommandIOApp("fs2chat-server", "Fs2 powered TCP chat server"):

  override def main: Opts[IO[ExitCode]] = Opts
    .option[Int]("port", "Port to bind for connection requests")
    .withDefault(5555)
    .mapValidated(Port.fromInt(_).toValidNel("Invalid port number"))
    .map(port =>
      Network[IO].serverResource(port = port.some).use((address, socketStream) =>
        Console[IO].println(
          s"Server started on port ${address.host.toUriString}:${address.port.value}"
        ) >> Clients().flatMap(Server.start(_, socketStream).compile.drain).as(ExitCode.Success)
      )
    )
