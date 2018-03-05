package coin

import cats.effect.IO


object Main extends App {
  val genesisBlock = Block(Header(0,Hash(Seq.empty),-454821241,1520117211495L),"genesis")
  println(genesisBlock.hash)
}
