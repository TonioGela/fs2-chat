package dev.toniogela.chat

import cats.effect.*
import com.comcast.ip4s.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

val username = Opts.argument[String]("username")

object ClientApp extends CommandIOApp("fs2chat-client", "Fs2 powered TCP chat client"):
  def main = username.map: username =>
    for
      printer <- Printer.create
      address  = SocketAddress(host"scala.show", port"5555")
      _       <- Client.connectAndHandle(address, username, printer).compile.drain
    yield ExitCode.Success
