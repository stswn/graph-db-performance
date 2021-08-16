// *****************************************************************************
// Projects
// *****************************************************************************

lazy val root =
  project
    .in(file("."))
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.zio,
        library.zioCatsInterop,
        library.zioTest,
        library.postgresContainer,
        library.doobie,
        library.doobiePostgres,
        library.neo4JContainer,
        library.neo4JDriver
      ),
      publishArtifact := false,
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val zio                   = "1.0.9"
      val zioCatsInteropVersion = "2.5.1.0"
      val testContainers        = "0.39.5"
      val doobie                = "0.13.4"
      val neo4JDriver           = "4.2.5"
    }
    val zio               = "dev.zio"         %% "zio"                             % Version.zio
    val zioTest           = "dev.zio"         %% "zio-test"                        % Version.zio
    val zioCatsInterop    = "dev.zio"         %% "zio-interop-cats"                % Version.zioCatsInteropVersion
    val postgresContainer = "com.dimafeng"    %% "testcontainers-scala-postgresql" % Version.testContainers
    val doobie            = "org.tpolecat"    %% "doobie-core"                     % Version.doobie
    val doobiePostgres    = "org.tpolecat"    %% "doobie-postgres"                 % Version.doobie
    val neo4JContainer    = "com.dimafeng"    %% "testcontainers-scala-neo4j"      % Version.testContainers
    val neo4JDriver       = "org.neo4j.driver" % "neo4j-java-driver"               % Version.neo4JDriver
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
    commandAliases

lazy val commonSettings =
  Seq(
    name         := "Grah DB Performance",
    scalaVersion := "3.0.1",
    organization := "pl.stswn.graph_db"
  )

lazy val commandAliases =
  addCommandAlias("fmt", "all scalafmtSbt scalafmt Test / scalafmt") ++
    addCommandAlias("check", "; scalafmtSbtCheck; scalafmtCheckAll;")
