package dev.toniogela.chat

import scodec.Codec

object Protocol:
  enum ClientCommand derives Codec:
    case RequestUsername(name: String)
    case SendMessage(value: String)

  enum ServerCommand derives Codec:
    case SetUsername(name: String)
    case Alert(text: String)
    case Message(name: String, text: String)
