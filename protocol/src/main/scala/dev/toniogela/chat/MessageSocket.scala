package dev.toniogela.chat

import cats.effect.IO
import cats.effect.std.Queue
import fs2.Stream
import fs2.io.net.Socket
import fs2.interop.scodec.{StreamDecoder, StreamEncoder}
import scodec.{Decoder, Encoder}

trait MessageSocket[In, Out]:
  def read: Stream[IO, In]
  def write(out: Out): IO[Unit]

object MessageSocket:

  def apply[In: Decoder, Out: Encoder](
      socket: Socket[IO]
  ): IO[MessageSocket[In, Out]] = Queue.bounded[IO, Out](1024).map: outgoing =>
    new MessageSocket[In, Out] {

      def read: Stream[IO, In]      =
        val readSocket: Stream[IO, In] = socket.reads
          .through(StreamDecoder.many(Decoder[In]).toPipeByte[IO])

        val writeOutput = Stream
          .fromQueueUnterminated(outgoing)
          .through(StreamEncoder.many(Encoder[Out]).toPipeByte)
          .through(socket.writes)

        readSocket.concurrently(writeOutput)
      def write(out: Out): IO[Unit] = outgoing.offer(out)
    }
