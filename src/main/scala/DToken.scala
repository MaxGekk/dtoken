import com.databricks.Shard
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import org.libnetrc.{Machine, NetRcFile}

import scala.util.Try

case class Config(
                   isInteractive: Boolean = false,
                   shard: Option[String],
                   list: Boolean = true
                 )

object DToken extends StrictLogging {
  val argsParser = new scopt.OptionParser[Config]("Databricks Token Management tool") {
    head("dToken", "0.1")

    opt[String]('s', "shard")
      .text("shard's url to connect to")
      .valueName("string")
      .action((x, c) => c.copy(shard = Some(x)))

    opt[Unit]('l', "list").action( (_, c) =>
      c.copy(list = true) ).text("list all tokens")

    help("help")
    version("version")
  }

  def run(config: Config): Unit = {
    logger.debug(s"Running dToken with the config: $config")

    if (config.isInteractive) {
      throw new NotImplementedError("The interactive mode hasn't implemented yet")
    } else if (config.shard.isEmpty) {
      throw new IllegalArgumentException("Unknown shard name")
    } else {
      val shardName = config.shard.get
      logger.debug(s"Looking for credentials for $shardName")
      val netrc = NetRcFile.read
      val Machine(_, login, password, _) = netrc.find(shardName).get
      logger.debug(s"Found login = $login for the shard: $shardName")

      val shard = Shard(shardName)
        .username(login).password(password)
        .connect

      if (config.list) {
        val tokens = shard.token.list
        println(s"Tokens at $shardName")
        tokens foreach println
        println(s"Total tokens: ${tokens.size}")
      }
    }
  }

  def appConf: Config = {
    val conf = ConfigFactory.load()

    Config(
      shard = Try { conf.getString("default.shard") }.toOption
    )
  }

  def main(args: Array[String]): Unit = {
    argsParser.parse(args, appConf) match {
      case Some(config) => run(config)
      case _ => System.err.println("Fix params and try again")
    }
  }
}
