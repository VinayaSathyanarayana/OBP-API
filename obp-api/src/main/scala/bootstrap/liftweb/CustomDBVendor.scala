package bootstrap.liftweb

import code.api.util.APIUtil
import com.zaxxer.hikari.pool.ProxyConnection
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.sql.Connection
import net.liftweb.common.{Box, Full, Logger}
import net.liftweb.db.ConnectionManager
import net.liftweb.util.ConnectionIdentifier
import net.liftweb.util.Helpers.tryo

/**
 * The Custom DB vendor.
 *
 * @param driverName the name of the database driver
 * @param dbUrl the URL for the JDBC data connection
 * @param dbUser the optional username
 * @param dbPassword the optional db password
 */
class CustomDBVendor(driverName: String,
                     dbUrl: String,
                     dbUser: Box[String],
                     dbPassword: Box[String]) extends CustomProtoDBVendor {

  private val logger = Logger(classOf[CustomDBVendor])

  object HikariDatasource {
    val config = new HikariConfig()
    
    val connectionTimeout = APIUtil.getPropsAsLongValue("hikari.connectionTimeout")
    val maximumPoolSize = APIUtil.getPropsAsLongValue("hikari.maximumPoolSize")
    val idleTimeout = APIUtil.getPropsAsLongValue("hikari.idleTimeout")
    val keepaliveTime = APIUtil.getPropsAsLongValue("hikari.keepaliveTime")
    val maxLifetime = APIUtil.getPropsAsLongValue("hikari.maxLifetime")
    val metricRegistry = APIUtil.getPropsAsLongValue("hikari.metricRegistry")
    
    if(connectionTimeout.isDefined){
      config.setConnectionTimeout(connectionTimeout.head)
    }
    if(maximumPoolSize.isDefined){
      config.setConnectionTimeout(maximumPoolSize.head)
    }
    if(idleTimeout.isDefined){
      config.setConnectionTimeout(idleTimeout.head)
    }
    if(keepaliveTime.isDefined){
      config.setConnectionTimeout(keepaliveTime.head)
    }
    if(maxLifetime.isDefined){
      config.setConnectionTimeout(maxLifetime.head)
    }
    if(metricRegistry.isDefined){
      config.setConnectionTimeout(connectionTimeout.head)
    }

    (dbUser, dbPassword) match {
      case (Full(user), Full(pwd)) =>
        config.setJdbcUrl(dbUrl)
        config.setUsername(user)
        config.setPassword(pwd)
      case _ =>
        config.setJdbcUrl(dbUrl)
    }
    
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

    val ds: HikariDataSource = new HikariDataSource(config)
  }

  def createOne: Box[Connection] =  {
    tryo{t:Throwable => logger.error("Cannot load database driver: %s".format(driverName), t)}{Class.forName(driverName);()}
    tryo{t:Throwable => logger.error("Unable to get database connection. url=%s".format(dbUrl),t)}(HikariDatasource.ds.getConnection())
  }

  def closeAllConnections_!(): Unit = HikariDatasource.ds.close()
}

trait CustomProtoDBVendor extends ConnectionManager {
  private val logger = Logger(classOf[CustomProtoDBVendor])

  def createOne: Box[Connection]

  def newConnection(name: ConnectionIdentifier): Box[Connection] = {
    createOne
  }

  def releaseConnection(conn: Connection): Unit = {conn.asInstanceOf[ProxyConnection].close()}

}