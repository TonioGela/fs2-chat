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

private type Client = MessageSocket[ClientCommand, ServerCommand]

private case class Clients private (ref: Ref[IO, Map[Username, Client]]):
  def get(username: Username): IO[Option[Client]]                = ref.get.map(_.get(username))
  def all: IO[List[Client]]                                      = ref.get.map(_.values.toList)
  def unregister(name: Username): IO[Option[Client]]             =
    ref.modify(old => (old - name, old.get(name)))
  def broadcast(cmd: ServerCommand): IO[Unit]                    = all.flatMap(_.traverse_(_.write(cmd)))
  def register(username: Username, client: Client): IO[Username] = ref.modify { oldClients =>
    val finalName = Clients.reserveUsername(username, oldClients.keySet)
    (oldClients + (finalName -> client), finalName)
  }

private object Clients:
  def apply(): IO[Clients] = Ref.of[IO, Map[Username, Client]](Map.empty).map(new Clients(_))

  private def reserveUsername(
      username: Username,
      usernames: Set[Username],
      offset: Int = 0
  ): Username =
    val candidate = if offset === 0 then username else Username(s"${username.name}-$offset")
    if usernames.contains(candidate) then reserveUsername(username, usernames, offset + 1)
    else candidate

object Server:

  private def createClient(
      client: Client,
      clients: Clients
  ): Stream[IO, Nothing] = {

    def waitForUsername(s: Stream[IO, ClientCommand]): Pull[IO, Nothing, Unit] =
      s.pull.uncons1.flatMap {
        case Some((ClientCommand.RequestUsername(name), rest)) =>
          Pull.eval(handleRequestUsername(name, client)(clients))
            .flatMap(handleMessages(_, rest)(client, clients).pull.echo)

        case Some((_, rest)) => Pull.eval(client.write(
            ServerCommand.Alert("You can't chat if don't request an username first!")
          )) >> waitForUsername(rest)

        case None => Pull.done
      }

    waitForUsername(client.read).stream
  }

  private def handleRequestUsername(
      desiredName: Username,
      client: Client
  )(clients: Clients): IO[Username] = for {
    username <- clients.register(desiredName, client)
    address  <- client.address
    _        <- IO.println(s"Accepted client $username on $address")
    _        <- client.write(ServerCommand.Alert("Welcome to FS2 Chat!"))
    _        <- client.write(ServerCommand.SetUsername(username))
    _        <- clients.broadcast(ServerCommand.Alert(s"$username connected"))
  } yield username

  private def handleMessages(
      username: Username,
      inStream: Stream[IO, ClientCommand]
  )(client: Client, clients: Clients): Stream[IO, Nothing] = inStream.evalMap {
    case ClientCommand.RequestUsername(name)             =>
      Console[IO].errorln(s"ERROR: The client $username requested a new username: $name")
    case ClientCommand.SendMessage(message)              =>
      clients.broadcast(ServerCommand.Message(username, message))
    case ClientCommand.DirectMessage(recipient, message) => clients.get(recipient).flatMap {
        case Some(recipientClient) =>
          recipientClient.write(ServerCommand.DirectMessage(username, message))
        case None                  => client.write(ServerCommand.Alert(s"There's no user $recipient connected"))
      }

  }.handleErrorWith {
    case _: UserQuit => Stream.exec(Console[IO].println(s"Client quit $username"))
    case err         => Stream.exec(
        Console[IO].errorln(s"Fatal error for $username") >> Console[IO].printStackTrace(err)
      )
  }.onFinalize(
    clients.unregister(username) >>
      clients.broadcast(ServerCommand.Alert(s"$username disconnected"))
  ).drain

  def start(clients: Clients, port: Port): Stream[IO, Nothing] =
    Stream.exec(Console[IO].println(s"Server started on port ${port.value}")) ++
      Network[IO].server(port = port.some).map(socket =>
        Stream.eval(MessageSocket[ClientCommand, ServerCommand](socket))
          .flatMap(createClient(_, clients))
      ).parJoinUnbounded

end Server
