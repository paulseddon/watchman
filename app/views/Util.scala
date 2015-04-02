package views

import com.amazonaws.services.ecs.model.{ContainerInstance, Resource}

import scala.collection.JavaConverters._

object Util {

  def statusToModifier(status: String) = {
    status match {
      case "ACTIVE" | "Healthy" | "CREATE_COMPLETE" => "success"
      case _ => "default"
    }
  }

  def arnToId(arn: String, token: Int) = {
    arn.split(":")(token)
  }

  def usedResource(ci: ContainerInstance, name: String) = {
    val registered = ci.getRegisteredResources.asScala.find(_.getName.equals(name)).get
    val remaining = ci.getRemainingResources.asScala.find(_.getName.equals(name)).get

    val perc = 100 - ((remaining.getIntegerValue.doubleValue() / registered.getIntegerValue.doubleValue()) * 100)

    perc.formatted("%.0f")
  }

  def ports(ci: ContainerInstance) = {
    val registered = ci.getRegisteredResources.asScala.find(_.getName.equals("PORTS")).get
    registered.getStringSetValue.asScala.mkString(",")
  }

}
