package dev.toniogela.chat

import scodec.Codec
import scala.util.control.NoStackTrace

object UserQuit extends Exception with NoStackTrace

object Protocol:
  enum ClientCommand derives Codec:
    case RequestUsername(name: String)
    case SendMessage(value: String)

  enum ServerCommand derives Codec:
    case SetUsername(name: String)
    case Alert(text: String)
    case Message(name: String, text: String)
    case Disconnect
