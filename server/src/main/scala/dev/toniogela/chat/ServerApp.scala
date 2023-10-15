package dev.toniogela.chat

import cats.syntax.option.*
import cats.effect.{ExitCode, IO}
import com.comcast.ip4s.Port
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts

object ServerApp extends CommandIOApp("fs2chat-server", "Fs2 powered TCP chat server"):

  override def main: Opts[IO[ExitCode]] = Opts
    .option[Int]("port", "Port to bind for connection requests")
    .withDefault(5555)
    .mapValidated(Port.fromInt(_).toValidNel("Invalid port number"))
    .map(port =>
      Clients().flatMap(Server.start(_, port).compile.drain).as(ExitCode.Success)
    )
