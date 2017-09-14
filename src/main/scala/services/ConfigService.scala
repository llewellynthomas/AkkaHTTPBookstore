package services

import com.typesafe.config.ConfigFactory


trait ConfigService {

  private val config = ConfigFactory.load()

  private val httpConfig = config.getConfig("http")

  private val databaseConfig = config.getConfig("database")

  val httpHost = httpConfig.getString("interface")

  val httpPort = httpConfig.getInt("port")

  val jdbcUrl = databaseConfig.getString("url")

  val dbUser = databaseConfig.getString("user")

  val dbPassword = databaseConfig.getString("password")
}
