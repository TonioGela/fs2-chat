package dev.toniogela.chat

import cats.syntax.all.*
import cats.effect.IO
import cats.effect.kernel.Ref
import fs2.{Pull, Stream}
import fs2.io.net.Socket
import Protocol.*

type Client = MessageSocket[ClientCommand, ServerCommand]

final case class State private (ref: Ref[IO, Map[String, Client]]):
  def get(username: String): IO[Option[Client]]            = ref.get.map(_.get(username))
  def names: IO[List[String]]                              = ref.get.map(_.keySet.toList)
  def register(username: String, client: Client): IO[Unit] = ref.update(_ + (username -> client))
  def unregister(name: String): IO[Unit]                   = ref.update(_ - name)
  private def all: IO[List[Client]]                        = ref.get.map(_.values.toList)
  def broadcast(cmd: ServerCommand): IO[Unit]              = all.flatMap(_.traverse_(_.write(cmd)))

object State:
  def create: IO[State] = Ref.of[IO, Map[String, Client]](Map.empty).map(new State(_))

object Server:

  private def createClient(state: State)(client: Client): Stream[IO, Unit] = {

    def waitForUsername(s: Stream[IO, ClientCommand]): Pull[IO, Unit, Unit] =
      s.pull.uncons1.flatMap {
        case Some((ClientCommand.RequestUsername(name), restOfTheCommands)) => for {
            _    <- Pull.eval(registerAndGreet(name, client, state))
            pull <- handleMessages(name, state, client.write, restOfTheCommands).pull.echo
          } yield pull

        case Some((_, rest)) => waitForUsername(rest) // recursion!

        case None => Pull.done
      }

    waitForUsername(client.read).stream
  }

  private def registerAndGreet(username: String, client: Client, state: State): IO[Unit] =
    state.register(username, client) >> client.write(ServerCommand.SetUsername(username)) >>
      state.broadcast(ServerCommand.Alert(s"$username connected"))

  private def handleMessages(
      username: String,
      state: State,
      send: ServerCommand => IO[Unit],
      commands: Stream[IO, ClientCommand]
  ): Stream[IO, Unit] = commands.evalMap {
    case ClientCommand.RequestUsername(name) =>
      IO.println(s"ERROR: The client $username requested a new username: $name")
    case ClientCommand.SendMessage("/users") =>
      state.names.map(_.mkString(", ")).map(ServerCommand.Alert(_)).flatMap(send)
    case ClientCommand.SendMessage(message)  =>
      state.broadcast(ServerCommand.Message(username, message))
  }.onFinalize(
    state.unregister(username) >> state.broadcast(ServerCommand.Alert(s"$username disconnected"))
  )

  def start(state: State, stream: Stream[IO, Socket[IO]]): Stream[IO, Unit] = stream
    .evalMap(MessageSocket[ClientCommand, ServerCommand](_))
    .map(createClient(state))
    .parJoinUnbounded

end Server
