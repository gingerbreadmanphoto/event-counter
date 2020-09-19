import DockerSyntax._

name := "event-counter"

version := "0.1"

scalaVersion := "2.12.4"

val commons = (project in file("commons"))
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.circeCore,
      Dependencies.circeGeneric,
      Dependencies.http4sCore,
      Dependencies.http4sCirce,
      Dependencies.fs2Kafka,
      Dependencies.slf4jSimple,
      Dependencies.slf4jApi
    )
  )



val worker = (project in file("worker"))
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.fs2,
      Dependencies.http4sDsl,
      Dependencies.http4sServer
    ),
    libraryDependencies ++= Seq(
      Dependencies.test.uTest(Test, IntegrationTest),
      Dependencies.test.testContainers,
      Dependencies.test.testContainersKafka,
      Dependencies.http4sClient
    ),
    Defaults.itSettings
  )
  .configs(IntegrationTest)
  .withDocker(exposedPorts = Seq(8090))
  .dependsOn(commons % "compile->compile,test->test,it->it")

val bot = (project in file("bot"))
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.telegramBot,
      Dependencies.http4sClient
    ),
    libraryDependencies ++= Seq(
      Dependencies.test.uTest(Test)
    ),
  )
  .withDocker()
  .dependsOn(commons % "compile->compile,test->test,it->it")