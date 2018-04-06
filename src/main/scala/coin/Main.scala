package coin

import monocle.macros.GenLens


object Main extends App {
  val genesisBlock = Block(Header(0,Hash(Seq.empty), -1522759259, 1520117211495L),"genesis")
  println(genesisBlock.hash)

  val b = GenLens[Block[String]](_.header.nonce).set(0)(genesisBlock)

  val task = ProofOfWork.proofMultithreadedIo(b)

  val block = task.unsafeRunSync()
  println(s"Result: $block")
}
