package dev.toniogela.chat

import cats.Eq
import scodec.Codec
import scodec.codecs.*
import scala.util.control.NoStackTrace

case class Username(name: String) extends Ordered[Username]:
  def compare(that: Username): Int = name.compare(that.name)
  override def toString: String    = name

object Username:
  given Eq[Username] = Eq.fromUniversalEquals[Username]

final class UserQuit extends Exception with NoStackTrace

object Protocol:
  private val username: Codec[Username] = utf8_32.as[Username]

  enum ClientCommand:
    case RequestUsername(name: Username)
    case SendMessage(value: String)

  object ClientCommand:
    given Codec[ClientCommand] = discriminated[ClientCommand]
      .by(uint8)
      .typecase(1, username.as[RequestUsername])
      .typecase(2, utf8_32.as[SendMessage])

  enum ServerCommand:
    case SetUsername(name: Username)
    case Alert(text: String)
    case Message(name: Username, text: String)
    case Disconnect

  object ServerCommand:
    given Codec[ServerCommand] = discriminated[ServerCommand]
      .by(uint8)
      .typecase(129, username.as[SetUsername])
      .typecase(130, utf8_32.as[Alert])
      .typecase(131, (username :: utf8_32).as[Message])
      .typecase(132, provide(Disconnect))
