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

object UserQuit extends Exception with NoStackTrace

object Protocol:
  private val username: Codec[Username] = utf8_32.as[Username]

  enum ClientCommand:
    case RequestUsername(name: Username)
    case SendMessage(value: String)
    case DirectMessage(name: Username, message: String)

  object ClientCommand:
    given Codec[ClientCommand] = discriminated[ClientCommand]
      .by(uint8)
      .typecase(1, username.as[RequestUsername])
      .typecase(2, utf8_32.as[SendMessage])
      .typecase(3, (username :: utf8_32).as[DirectMessage])

  enum ServerCommand:
    case SetUsername(name: Username)
    case Alert(text: String)
    case Message(name: Username, text: String)
    case DirectMessage(name: Username, message: String)
    case Disconnect

  object ServerCommand:
    given Codec[ServerCommand] = discriminated[ServerCommand]
      .by(uint8)
      .typecase(129, username.as[SetUsername])
      .typecase(130, utf8_32.as[Alert])
      .typecase(131, (username :: utf8_32).as[Message])
      .typecase(132, (username :: utf8_32).as[DirectMessage])
      .typecase(133, provide(Disconnect))
