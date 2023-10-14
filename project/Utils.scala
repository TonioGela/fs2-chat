import sbt.Project
import spray.revolver.RevolverPlugin
import sbtcrossproject.CrossProject

object Utils {

  implicit class RichProject(private val p: Project) extends AnyVal {
    def noRevolver: Project = p.disablePlugins(RevolverPlugin)
  }

  implicit class RichCrossProject(private val p: CrossProject.Builder) extends AnyVal {
    def noRevolver: CrossProject = p.disablePlugins(RevolverPlugin)
  }
}
