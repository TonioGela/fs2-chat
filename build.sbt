import Utils.*

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(List(
  organization     := "dev.toniogela",
  organizationName := "Antonio Gelameris",
  scalaVersion     := "3.3.1"
))

lazy val root = project.in(file(".")).noRevolver
  .aggregate(protocol.jvm, protocol.native, server, client)

lazy val protocol = crossProject(JVMPlatform, NativePlatform).crossType(CrossType.Pure).noRevolver
  .in(file("protocol"))
  .settings(
    libraryDependencies ++=
      List("co.fs2" %%% "fs2-io" % "3.9.2", "co.fs2" %%% "fs2-scodec" % "3.9.2")
  )

lazy val server = project.in(file("server"))
  .dependsOn(protocol.jvm)
  .settings(name := "fs2-chat-server")

lazy val client = project.in(file("client")).enablePlugins(ScalaNativePlugin)
  .dependsOn(protocol.native)
  .settings(
    name := "fs2-chat-client",
    libraryDependencies ++= List(
      "com.monovore"   %%% "decline-effect" % "2.4.1",
      "com.armanbilge" %%% "epollcat"       % "0.1.6"
    )
  )

// .settings(nativeConfig ~= { c =>
//   c
//     // .withLTO(LTO.none)
//     .withLinkingOptions("-L/opt/homebrew/opt/openssl@3/lib" +: c.linkingOptions)
//   // .withMode(Mode.debug)
// })
