package coin

import cats.data.ReaderT
import cats.effect.IO
import coin.Block.BlockNotFoundException
import org.scalatest.{FlatSpec, Matchers}
import coin.serialize.{MemoryStorage, Serialize, StreamsStorage}
import coin.util.instances.protobuf._

class SerializationSpec extends FlatSpec with Matchers {
  val store = implicitly[Serialize[StreamsStorage,Block[String]]]

  behavior of "A protobuf Block serializer"

  it should "read second serialized block" in {

    val header1 = Header(0, Hash(Seq.fill(64)(1)), 0, 0)
    val block1 = Block(header1, "hello, world!")
    val header2 = Header(1, Hash(Seq.fill(64)(1)), 0, 0)
    val block2 = Block(header2, "world, hello!")

    val storage = MemoryStorage()

    val io: ReaderT[IO,StreamsStorage,Block[String]] =
      for {
        _ <- store.write[IO](block1)
        _ <- store.write[IO](block2)
        _ <- store.read[IO]
        r <- store.read[IO]
      } yield r

    io.run(storage).unsafeRunSync() shouldBe block2
  }


  it should "read block with negative nonce value" in {
    val header1 = Header(0, Hash(Seq.fill(64)(1)), -1337, 0)
    val block1 = Block(header1, "hello, world!")

    val storage = MemoryStorage()

    val io: ReaderT[IO,StreamsStorage,Block[String]] =
      for {
        _ <- store.write[IO](block1)
        r <- store.read[IO]
      } yield r

    io.run(storage).unsafeRunSync() shouldBe block1
  }


  it should "return BlockNotFoundException when it can't read block" in {
    val storage = MemoryStorage()

    val io: ReaderT[IO,StreamsStorage,Block[String]] =
      for {
        r <- store.read[IO]
      } yield r

    io.run(storage).attempt.unsafeRunSync() shouldBe Left(BlockNotFoundException)
  }
}
