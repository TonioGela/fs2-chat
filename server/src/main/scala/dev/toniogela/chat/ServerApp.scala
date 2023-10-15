package dev.toniogela.chat

import cats.effect.IOApp
import com.comcast.ip4s.port

object ServerApp extends IOApp.Simple:
  def run = for {
    clients <- Clients.create
    _       <- Server.start(clients, port"5555").compile.drain
  } yield ()
