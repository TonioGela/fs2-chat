package dev.toniogela.chat

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

val port: Opts[Port] = Opts.option[Int]("port", "The port to connect to")
  .withDefault(5555)
  .mapValidated(Port.fromInt(_).toValidNel("Invalid port number"))

val hostname: Opts[Hostname] = Opts.option[String]("hostname", "The hostname to connect to")
  .withDefault("http://scala.show")
  .mapValidated(Hostname.fromString(_).toValidNel("Invalid hostname"))

val ip: Opts[IpAddress] = Opts.option[String]("ip", "The ip address to connect to")
  .mapValidated(IpAddress.fromString(_).toValidNel("Invalid IP address"))

val socket: Opts[SocketAddress[Host]] = (ip.orElse(hostname), port).mapN(SocketAddress[Host])

val username: Opts[Username] = Opts.argument[String]("username").map(Username(_))

object ClientApp extends CommandIOApp("fs2chat-client", "Fs2 powered TCP chat client") {
  override def main: Opts[IO[ExitCode]] = (socket, username).mapN((socket, username) =>
    Printer.create.flatMap(
      Client.connectAndHandle(socket, username, _).compile.drain.as(ExitCode.Success)
    )
  )
}
