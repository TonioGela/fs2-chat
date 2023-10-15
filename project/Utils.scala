import sbt.*
import sbtcrossproject.*
import spray.revolver.RevolverPlugin
import scala.scalanative.sbtplugin.ScalaNativePlugin

object Utils {

  implicit class RichProject(private val p: Project) extends AnyVal {

    def root(c: CompositeProject*): Project = p.in(file(".")).disablePlugins(
      RevolverPlugin
    ).aggregate(c.flatMap(_.componentProjects).map(_.project): _*)

    def native: Project = p.enablePlugins(ScalaNativePlugin).disablePlugins(RevolverPlugin)
  }

  implicit class RichCrossProject(private val p: CrossProject.Builder) extends AnyVal {
    def pure: CrossProject = p.crossType(CrossType.Pure).disablePlugins(RevolverPlugin)
  }
}
