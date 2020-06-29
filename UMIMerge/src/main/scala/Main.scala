  import collapse.{TwoPassUMIs, UMIProcessing}
  import com.typesafe.scalalogging.LazyLogging
  import eventcalling.DeepSeq
  import org.slf4j.LoggerFactory
  import picocli.CommandLine
  import picocli.CommandLine.Command

@Command(name = "MergeAndCall", version = Array("1.1.0"), sortOptions = false,
  description = Array("either call UMI sequences from reads, or call CRISPR events from aligned sequences"))
class Main() extends Runnable with LazyLogging {

  def run(): Unit = {
  }
}
object Main {
  def main(args: Array[String]) {
    val main = new Main()
    val commandLine = new CommandLine(main)
    val logger = LoggerFactory.getLogger("Main")

    val initialTime = System.nanoTime()

    commandLine.addSubcommand("UMIMerge", new UMIProcessing())
    commandLine.addSubcommand("DeepSeq", new DeepSeq())

    commandLine.parseWithHandler(new CommandLine.RunLast, args)

    // check to make sure we ran a subcommand
    if (!commandLine.getParseResult.hasSubcommand)
      System.err.println(commandLine.getUsageMessage)

    logger.info("Total runtime " + "%.2f".format((System.nanoTime() - initialTime) / 1000000000.0) + " seconds")
  }
}
