package coin.util

import cats.data.ReaderT
import cats.effect.{ContextShift, Effect, IO}
import coin.Block.BlockNotFoundException
import coin.Hash
import coin.proto.block.{Block => PBlock}
import coin.serialize.{Serialize, StreamsStorage}
import coin.util.GenIso.Convertible
import com.google.protobuf.ByteString
import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Selector

import scala.language.higherKinds

object instances {
  object protobuf {
    implicit val convBytes: Convertible[Array[Byte],ByteString] = ByteString.copyFrom(_)
    implicit val convByteStr: Convertible[ByteString,Array[Byte]] = _.toByteArray
    implicit val convHashBStr: Convertible[Hash,ByteString] = hash => ByteString.copyFrom(hash.bytes.toArray  )
    implicit val convBStrHash: Convertible[ByteString,Hash] = bStr => Hash(bStr.toByteArray)
    implicit val convBStrToStr: Convertible[ByteString,String] = _.toStringUtf8
    implicit val convStrToBStr: Convertible[String,ByteString] = ByteString.copyFromUtf8 _
  }

  trait ConvertibleInstances {
    implicit def genConvertableId[A]: Convertible[A, A] = identity[A]

    implicit def genConvertableHNil[HL <: HList]: Convertible[HL, HNil] = _ => HNil

    implicit def genConvertibleCons[K, V, T <: HList, L <: HList, X](implicit
      select: Selector.Aux[L, K, X],
      convHead: Convertible[X, V],
      convTail: Convertible[L, T]
    ): Convertible[L, FieldType[K, V] :: T] = l =>
      labelled.field[K](convHead(select(l))) :: convTail(l)

    implicit def genConvertableHListRepresentible[A, B, ReprA <: HList, ReprB <: HList](implicit
      lgA: LabelledGeneric.Aux[A, ReprA],
      lgB: LabelledGeneric.Aux[B, ReprB],
      conv: Lazy[Convertible[ReprA, ReprB]]
    ): Convertible[A, B] = { a: A =>
      val reprA = lgA.to(a)
      val reprB = conv.value(reprA)
      lgB.from(reprB)
    }
  }

  trait SerializeInstances {
    implicit val serializeProtoBlock = new Serialize[StreamsStorage, PBlock] {
      override def write[F[_]](block: => PBlock)(implicit F: Effect[F]) =
        ReaderT[F, StreamsStorage, Unit] { store =>
          F.delay {
            block.writeDelimitedTo(store.os)
          }
        }
      override def read[F[_]](implicit F: Effect[F]) =
        ReaderT[F, StreamsStorage, PBlock] { store =>
          val value = F.delay(PBlock.parseDelimitedFrom(store.is))
          F.flatMap(value) {
            case Some(x) => F.pure(x)
            case None => F.raiseError[PBlock](BlockNotFoundException)
          }
        }
    }

    implicit def serializeConvertible[A,B,S](implicit
      sB: Serialize[S,B],
      c1: Convertible[A,B],
      c2: Convertible[B,A]
    ) = new Serialize[S,A] {
      override def write[F[_] : Effect](a: => A): ReaderT[F, S, Unit] = sB.write(c1(a))
      override def read[F[_] : Effect]: ReaderT[F, S, A] = sB.read[F].map(c2.apply)
    }
  }

  trait RaceInstances {
    implicit def raceIO(implicit cs: ContextShift[IO]) = new Race[IO] {
      override def race[A, B](lh: IO[A], rh: IO[B]): IO[Either[A,B]] = IO.race(lh, rh)
    }
  }

  trait CancelableInstances {
    implicit val cancelableIO = new Cancelable[IO] {
      override def cancelBoundary: IO[Unit] = IO.cancelBoundary
    }
  }
}