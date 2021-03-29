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
        library.zioTest    % Test,
        library.zioTestSbt % Test
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
      val zio = "1.0.5"
    }
    val zio = "dev.zio" %% "zio" % Version.zio
    val zioTest    = "dev.zio" %% "zio-test"     % Version.zio
    val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zio
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
    scalafmtSettings ++
    scalafixSettings ++
    commandAliases

lazy val commonSettings =
  Seq(
    name := "Grah DB Performance",
    scalaVersion := "2.13.5",
    organization := "pl.stswn.graph_db"
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
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
