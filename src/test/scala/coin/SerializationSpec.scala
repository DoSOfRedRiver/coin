package coin

import cats.data.ReaderT
import cats.effect.IO
import org.scalatest.{FlatSpec, Matchers}
import coin.serialize.{MemoryStorage, Serialize, StreamsStorage}
import coin.util.Instances.protobuf._

class SerializationSpec extends FlatSpec with Matchers {
  behavior of "A protobuf Block serializer"

  it should "read second serialized block" in {

    val header1 = Header(0, Hash(Seq.fill(64)(1)), 0, 0)
    val block1 = Block(header1, "hello, world!")
    val header2 = Header(1, Hash(Seq.fill(64)(1)), 0, 0)
    val block2 = Block(header2, "world, hello!")

    val storage = MemoryStorage()//FileStorage(new java.io.File("block.chain"))

    val store = implicitly[Serialize[StreamsStorage,Block[String]]]

    val io: ReaderT[IO,StreamsStorage,Block[String]] =
      for {
        _ <- store.write[IO](block1)
        _ <- store.write[IO](block2)
        _ <- store.read[IO]
        r <- store.read[IO]
      } yield r

    io.run(storage).unsafeRunSync() shouldBe block2
  }
}
