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
        library.zioTest,
        library.postgresContainer,
        library.tranzactIO,
        library.doobie,
        library.doobiePostgres,
        library.neo4JContainer,
        library.neo4JDriver,
        library.neotypes
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
      val zio            = "1.0.5"
      val testContainers = "0.39.4"
      val tranzactIO     = "2.0.0"
      val doobie         = "0.12.1"
      val neotypes       = "0.17.0"
      val neo4JDriver    = "4.2.5"
    }
    val zio               = "dev.zio"              %% "zio"                             % Version.zio
    val zioTest           = "dev.zio"              %% "zio-test"                        % Version.zio
    val postgresContainer = "com.dimafeng"         %% "testcontainers-scala-postgresql" % Version.testContainers
    val tranzactIO        = "io.github.gaelrenoux" %% "tranzactio"                      % Version.tranzactIO
    val doobie            = "org.tpolecat"         %% "doobie-core"                     % Version.doobie
    val doobiePostgres    = "org.tpolecat"         %% "doobie-postgres"                 % Version.doobie
    val neo4JContainer    = "com.dimafeng"         %% "testcontainers-scala-neo4j"      % Version.testContainers
    val neotypes          = "com.dimafeng"         %% "neotypes-zio"                    % Version.neotypes
    val neo4JDriver       = "org.neo4j.driver"      % "neo4j-java-driver"               % Version.neo4JDriver
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
    scalafixSettings ++
    commandAliases

lazy val commonSettings =
  Seq(
    name := "Grah DB Performance",
    scalaVersion := "2.13.5",
    organization := "pl.stswn.graph_db"
  )

lazy val scalafixSettings =
  inThisBuild(
    Seq(
      scalafixDependencies ++= Seq(
        "com.github.liancheng" %% "organize-imports" % "0.4.4",
        "com.github.vovapolu"  %% "scaluzzi"         % "0.1.16"
      ),
      scalafixScalaBinaryVersion := "2.13",
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision
    )
  )

lazy val commandAliases =
  addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt") ++
    addCommandAlias("fix", "; all compile:scalafix test:scalafix; all scalafmtSbt scalafmtAll") ++
    addCommandAlias("check", "; scalafmtSbtCheck; scalafmtCheckAll; compile:scalafix --check; test:scalafix --check")
