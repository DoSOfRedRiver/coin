package coin

import cats.effect.IO


object Main extends App {
  val genesisBlock = Block(Header(0,Hash(Seq.empty), -1522759259, 1520117211495L),"genesis")
  println(genesisBlock.hash)
  val r = ProofOfWork.proofMultithreaded[String,IO](genesisBlock).unsafeRunSync()
  println(r.hash)
}
