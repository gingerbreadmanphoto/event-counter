import sbt._
import sbt.librarymanagement.Configuration

object Dependencies {
  object versions {
    val http4s: String        = "0.21.3"
    val circe: String         = "0.13.0"
    val fs2: String           = "2.4.2"
    val fs2Kafka: String      = "1.0.0"
    val telegramBot: String   = "0.5.0"
    val slf4j: String         = "1.7.28"

    object test {
      val uTest: String          = "0.7.2"
      val testContainers: String = "0.38.3"
    }
  }

  val circeGeneric: ModuleID = "io.circe"         %% "circe-generic"        % versions.circe
  val circeCore: ModuleID    = "io.circe"         %% "circe-core"           % versions.circe
  val http4sCore: ModuleID   = "org.http4s"       %% "http4s-core"          % versions.http4s
  val http4sCirce: ModuleID  = "org.http4s"       %% "http4s-circe"         % versions.http4s
  val http4sDsl: ModuleID    = "org.http4s"       %% "http4s-dsl"           % versions.http4s
  val http4sServer: ModuleID = "org.http4s"       %% "http4s-blaze-server"  % versions.http4s
  val http4sClient: ModuleID = "org.http4s"       %% "http4s-blaze-client"  % versions.http4s
  val fs2: ModuleID          = "co.fs2"           %% "fs2-core"             % versions.fs2
  val fs2Kafka: ModuleID     = "com.github.fd4s"  %% "fs2-kafka"            % versions.fs2Kafka
  val telegramBot: ModuleID  = "org.augustjune"   %% "canoe"                % versions.telegramBot
  val slf4jSimple: ModuleID  = "org.slf4j"        %  "slf4j-simple"         % versions.slf4j
  val slf4jApi: ModuleID     = "org.slf4j"        %  "slf4j-api"            % versions.slf4j

  object test {
    def uTest(configuration: Configuration*): ModuleID = "com.lihaoyi"  %% "utest"                      % versions.test.uTest          % configuration.map(_.name).mkString(", ")
    val testContainers: ModuleID                       = "com.dimafeng" %% "testcontainers-scala-core"  % versions.test.testContainers % IntegrationTest
    val testContainersKafka: ModuleID                  = "com.dimafeng" %% "testcontainers-scala-kafka" % versions.test.testContainers % IntegrationTest
  }
}