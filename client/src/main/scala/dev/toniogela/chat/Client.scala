package dev.toniogela.chat

import fs2.*
import fs2.io.net.Network
import Protocol.*
import cats.effect.*
import com.comcast.ip4s.*

object Client:

  private type Server = MessageSocket[ServerCommand, ClientCommand]

  def connectAndHandle(
      address: SocketAddress[Host],
      desiredUsername: String,
      printer: Printer
  ): Stream[IO, Unit] =
    for
      socket <- Stream.resource(Network[IO].client(address))
      server <- Stream.eval(MessageSocket[ServerCommand, ClientCommand](socket))
      _      <- Stream.eval(server.write(ClientCommand.RequestUsername(desiredUsername)))
      _      <- processIncoming(server, printer).concurrently(processOutgoing(server, printer))
    yield ()

  def processOutgoing(server: Server, printer: Printer): Stream[IO, Unit] = Stream
    .repeatEval(printer.readLine)
    .unNone
    .map(ClientCommand.SendMessage(_))
    .evalMap(server.write)

  def processIncoming(server: Server, printer: Printer): Stream[IO, Unit] = server
    .read
    .evalMap:
      case ServerCommand.SetUsername(username)  => printer.alert("Assigned username: " + username)
      case ServerCommand.Alert(txt)             => printer.alert(txt)
      case ServerCommand.Message(username, txt) => printer.println(s"$username> $txt")

end Client
