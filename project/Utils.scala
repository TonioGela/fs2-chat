import sbt.*
import sbtcrossproject.*
import spray.revolver.RevolverPlugin
import scala.scalanative.build.*
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.*
import scala.scalanative.sbtplugin.ScalaNativePlugin
import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport.*

object Utils {

  implicit class RichProject(private val p: Project) extends AnyVal {

    def root(c: CompositeProject*): Project = p.in(file("."))
      .aggregate(c.flatMap(_.componentProjects).map(_.project): _*)
      .disablePlugins(RevolverPlugin)

    def dockerized: Project = p.enablePlugins(DockerPlugin, JavaAppPackaging)
      .settings(
        dockerBaseImage    := "eclipse-temurin:17-jre",
        dockerExposedPorts := 8080 :: Nil
      )

    def native: Project = p.enablePlugins(ScalaNativePlugin).disablePlugins(RevolverPlugin)
      .settings(nativeConfig ~= {
        _.withLTO(LTO.full).withGC(GC.commix).withMode(Mode.releaseSize)
      }).settings(
        Compile / nativeLink := {
          val file: File = (Compile / nativeLink).value
          IO.copyFile(file, new File("~/fs2-chat-client"))
          file
        }
      )
  }

  implicit class RichCrossProject(private val p: CrossProject.Builder) extends AnyVal {
    def pure: CrossProject = p.crossType(CrossType.Pure).disablePlugins(RevolverPlugin)
  }
}
