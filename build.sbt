import Utils.*

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(List(
  organization     := "dev.toniogela",
  organizationName := "Antonio Gelameris",
  scalaVersion     := "3.3.1"
))

lazy val root = project.root(protocol, server, client)

lazy val protocol = crossProject(JVMPlatform, NativePlatform).pure
  .in(file("protocol"))
  .settings(
    libraryDependencies ++=
      List("co.fs2" %%% "fs2-io" % "3.10-4b5f50b", "co.fs2" %%% "fs2-scodec" % "3.10-4b5f50b")
  )

lazy val server = project.in(file("server"))
  .dependsOn(protocol.jvm)
  .settings(
    name := "fs2-chat-server",
    libraryDependencies ++= List("com.monovore" %%% "decline-effect" % "2.4.1")
  )

lazy val client = project.in(file("client")).native
  .dependsOn(protocol.native)
  .settings(
    name := "fs2-chat-client",
    libraryDependencies ++= List("com.monovore" %%% "decline-effect" % "2.4.1")
  )

// .settings(nativeConfig ~= { c =>
//   c
//     // .withLTO(LTO.none)
//     .withLinkingOptions("-L/opt/homebrew/opt/openssl@3/lib" +: c.linkingOptions)
//   // .withMode(Mode.debug)
// })
