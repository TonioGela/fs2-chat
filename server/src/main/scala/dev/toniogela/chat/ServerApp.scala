package dev.toniogela.chat

import cats.syntax.option.*
import cats.effect.{ExitCode, IO}
import cats.effect.std.Console
import com.comcast.ip4s.*
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import fs2.io.net.Network

object ServerApp extends CommandIOApp("fs2chat-server", "Fs2 powered TCP chat server"):

  val portArg: Opts[Port] = Opts.argument[Int]("port")
    .withDefault(5555)
    .mapValidated(Port.fromInt(_).toValidNel("Invalid port number"))

  def printAddress(address: SocketAddress[IpAddress]): String =
    s"${address.host.toUriString}:${address.port.value}"

  override def main: Opts[IO[ExitCode]] = portArg.map(port =>
    Network[IO].serverResource(port = port.some).use((address, socketStream) =>
      Console[IO].println(s"Server started on port ${printAddress(address)}") >>
        Clients.create
          .flatMap(Server.start(_, socketStream).compile.drain)
          .as(ExitCode.Success)
    )
  )
