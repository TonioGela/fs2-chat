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
  given Codec[Username] = utf8_32.as[Username]

  enum ClientCommand derives Codec:
    case RequestUsername(name: Username)
    case SendMessage(value: String)
    case DirectMessage(name: Username, message: String)

  enum ServerCommand derives Codec:
    case SetUsername(name: Username)
    case Alert(text: String)
    case Message(name: Username, text: String)
    case DirectMessage(name: Username, message: String)
    case Disconnect
