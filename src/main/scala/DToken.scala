import com.databricks.{Shard, ShardClient}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import org.libnetrc._

import scala.util.Try

case class Config(
                   isInteractive: Boolean = false,
                   shard: Option[String] = None,
                   tokenToDelete: Option[String] = None,
                   tokenToCreate: Option[String] = None,
                   lifeTime: Option[Long] = None,
                   list: Boolean = false
                 )

object DToken extends StrictLogging {
  val argsParser = new scopt.OptionParser[Config]("Databricks Token Management tool") {
    head("dToken", "0.1")

    opt[String]('s', "shard")
      .text("shard's url to connect to")
      .valueName("string")
      .action((x, c) => c.copy(shard = Some(x)))

    opt[String]('d', "delete")
      .text("id of the token to delete")
      .valueName("string")
      .action((x, c) => c.copy(tokenToDelete = Some(x)))

    opt[String]('c', "create")
      .text("create new token with the comment")
      .valueName("string")
      .action((x, c) => c.copy(tokenToCreate = Some(x)))

    opt[Unit]('l', "list").action( (_, c) =>
      c.copy(list = true) ).text("list all tokens")

    help("help")
    version("version")
  }

  def shardClient(shardName: String): ShardClient = {
    val netrc = NetRcFile.read
    val Machine(_, login, password, _) = netrc.find(shardName).get
    logger.debug(s"Connecting to $shardName with login = $login")

    Shard(shardName)
      .username(login).password(password)
      .connect
  }

  def list(shardName: String): Unit = {
    val tokens = shardClient(shardName).token.list
    println(s"Tokens registered at $shardName")
    tokens foreach println
    println(s"Total tokens: ${tokens.size}")

    println(s"Credentials in .netrc for the shard: $shardName")
    NetRcFile.read.items foreach {_ match {
      case m:Machine if m.name == shardName => println(m)
      case d: Default => println(d)
      case _ => ()
    }}
  }

  def createToken(shardName: String, comment: String, lifeTime: Option[Long]): Unit = {
    val lt: Long = lifeTime.getOrElse(60 * 60)
    logger.debug(s"Creating new token: lifeTime=$lifeTime comment=$comment")
    val com.databricks.NewToken(value, info) =
      shardClient(shardName).token.create(lt, comment)
    println(info)
    NetRcFile.read
      .upsert(Machine(shardName, "token", value))
      .save()
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

      config.tokenToDelete foreach {tokenId =>
        logger.debug(s"Deleting the token: $tokenId")
        shardClient(shardName).token.delete(tokenId)
      }

      config.tokenToCreate foreach {comment =>
        createToken(shardName, comment, config.lifeTime)
      }

      if (config.list)
        list(shardName)
    }
  }

  def appConf: Config = {
    val conf = ConfigFactory.load()

    Config(
      shard = Try { conf.getString("default.shard") }.toOption,
      lifeTime = Try { conf.getLong("default.token-lifetime") }.toOption
    )
  }

  def main(args: Array[String]): Unit = {
    argsParser.parse(args, appConf) match {
      case Some(config) => run(config)
      case _ => System.err.println("Fix params and try again")
    }
  }
}
