package coin

object Main extends App {
  val body = "Thr first block arrives!"
  val block = Block(0, Hash(Array(0,1,2,3,4,5)), body, 0L)
  val firstBlock = ProofOfWork.proof(block)
  println(s"The first block is: \n$firstBlock\nAnd hash is:\n${firstBlock.hash}")

  case class Transaction(from: String, to: String, amount: Int)
  val tr = Transaction("Alex", "Vitas", 10)
  val secondBlock = ProofOfWork.proof(Block.fromBlock(firstBlock, tr))
  println(s"The second block is: \n$secondBlock\nAnd hash is:\n${secondBlock.hash}")
}
