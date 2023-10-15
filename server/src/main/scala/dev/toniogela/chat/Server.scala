package dev.toniogela.chat

import cats.syntax.all.*
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.kernel.Ref
import fs2.Stream
import Protocol.*
import fs2.Pull
import com.comcast.ip4s.Port
import fs2.io.net.Network

private case class Client(username: Username, socket: MessageSocket[ClientCommand, ServerCommand])

private case class Clients private (ref: Ref[IO, Map[Username, Client]]):
  def get(username: Username): IO[Option[Client]]    = ref.get.map(_.get(username))
  def all: IO[List[Client]]                          = ref.get.map(_.values.toList)
  def unregister(name: Username): IO[Option[Client]] =
    ref.modify(old => (old - name, old.get(name)))
  def broadcast(cmd: ServerCommand): IO[Unit]        = all.flatMap(_.traverse_(_.socket.write(cmd)))
  def register(
      username: Username,
      socket: MessageSocket[ClientCommand, ServerCommand]
  ): IO[Client] = ref.modify { oldClients =>
    val finalName = Clients.reserveUsername(username, oldClients.keySet)
    val client    = Client(finalName, socket)
    (oldClients + (finalName -> client), client)
  }

private object Clients:
  def create: IO[Clients] = Ref.of[IO, Map[Username, Client]](Map.empty).map(new Clients(_))

  def reserveUsername(username: Username, usernames: Set[Username], offset: Int = 0): Username =
    val candidate = if offset === 0 then username else Username(s"${username.name}-$offset")
    if usernames.contains(candidate) then reserveUsername(username, usernames, offset + 1)
    else candidate

object Server:

  def createClient(
      socket: MessageSocket[ClientCommand, ServerCommand],
      clients: Clients
  ): Stream[IO, Nothing] = {
    def waitForUsername(s: Stream[IO, ClientCommand]): Pull[IO, Nothing, Unit] =
      s.pull.uncons1.flatMap {
        case Some((ClientCommand.RequestUsername(name), rest)) => Pull.eval(
            clients.register(name, socket).flatTap(client =>
              socket.address.flatMap { address =>
                IO.println(s"Accepted client ${client.username} on $address") >>
                  socket.write(ServerCommand.Alert("Welcome to FS2 Chat!")) >>
                  socket.write(ServerCommand.SetUsername(client.username)) >>
                  clients.broadcast(ServerCommand.Alert(s"${client.username} connected"))
              }
            )
          ).flatMap(c => handle(rest, c, clients).pull.echo)

        case Some((ClientCommand.SendMessage(_), rest)) => Pull.eval(
            socket.write(ServerCommand.Alert("You can't chat if don't request an username first!"))
          ) >> waitForUsername(rest)
        case None                                       => Pull.done
      }

    waitForUsername(socket.read).stream
  }

  // TODO! error handling
  def handle(
      stream: Stream[IO, ClientCommand],
      client: Client,
      clients: Clients
  ): Stream[IO, Nothing] = stream.evalMap {
    case ClientCommand.RequestUsername(name) =>
      Console[IO].errorln(s"ERROR: The client ${client.username} requested a new username: $name")
    case ClientCommand.SendMessage(message)  =>
      clients.broadcast(ServerCommand.Message(client.username, message))
  }.drain

  def start(clients: Clients, port: Port): Stream[IO, Nothing] = Network[IO].server(port =
    port.some
  ).map(socket =>
    Stream.eval(MessageSocket[ClientCommand, ServerCommand](socket)).flatMap(createClient(
      _,
      clients
    ))
  ).parJoinUnbounded

end Server
