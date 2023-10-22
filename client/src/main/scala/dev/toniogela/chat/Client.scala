package dev.toniogela.chat

import fs2.*
import fs2.io.net.Network
import Protocol.*
import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import scala.util.matching.Regex
import dev.toniogela.chat.Printer.stream

object Client:

  private type Server = MessageSocket[ServerCommand, ClientCommand]

  def connectAndHandle(
      address: SocketAddress[Host],
      desiredUsername: Username,
      printer: Printer
  ): Stream[IO, Unit] = printer.info(s"Connecting to server $address").stream ++
    Stream.resource(Network[IO].client(address))
      .flatMap(socket =>
        printer.info("🎉 Connected! 🎊").stream ++
          Stream.eval(MessageSocket[ServerCommand, ClientCommand](socket)).flatMap(server =>
            server.write(ClientCommand.RequestUsername(desiredUsername)).stream ++
              processIncoming(server, printer).concurrently(processOutgoing(server, printer))
          )
      ).handleErrorWith {
        case UserQuit => printer.println("Bye bye!").stream
        case t        => Stream.raiseError(t)
      }

  private val directMessage: Regex = "@(\\S+):(.+)".r

  private def processOutgoing(
      server: Server,
      printer: Printer
  ): Stream[IO, Unit] = Stream
    .repeatEval(printer.readLine)
    .unNone
    .map {
      case directMessage(name, msg) if name.trim.nonEmpty && msg.trim.nonEmpty =>
        ClientCommand.DirectMessage(Username(name.trim), msg.trim)
      case msg                                                                 => ClientCommand.SendMessage(msg)
    }
    .evalMap(server.write)

  private def processIncoming(
      messageSocket: Server,
      printer: Printer
  ): Stream[IO, Unit] = messageSocket.read
    .evalMap {
      case ServerCommand.DirectMessage(name, message) => printer.privateMessage(name, message)
      case ServerCommand.Alert(txt)                   => printer.alert(txt)
      case ServerCommand.Message(username, txt)       => printer.println(s"$username> $txt")
      case ServerCommand.SetUsername(username)        => printer.alert("Assigned username: " + username)
      case ServerCommand.Disconnect                   => UserQuit.raiseError[IO, Unit]
    }

end Client
