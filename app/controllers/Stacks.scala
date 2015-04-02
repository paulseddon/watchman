package controllers

import akka.actor.{Actor, ActorLogging}
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Region
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.{CreateStackRequest, DescribeStacksResult}
import controllers.Application._

import scala.collection.JavaConversions._
import scala.io.Source

case class RegionStackRequest(region: String, name: String, envType: String, enableAlarms: Boolean, notificationEmail: String)

case class DescribeStacksRequest(region: String)

case class DescribeStacksResponse(result: DescribeStacksResult)

class StackManager extends Actor with ActorLogging {

  def receive: Receive = {
    case rsr: RegionStackRequest => {
      log.debug(s"Received request: $rsr")

      val cf = Region.getRegion(rsr.region).createClient(classOf[AmazonCloudFormationClient],
        cp, new ClientConfiguration)

      //todo extract template filename and params
      val template = Source.fromURL(getClass.getResource("/aws-initialise-region.json")).getLines.mkString
      val csr = new CreateStackRequest()
      csr.setStackName(rsr.name)
      csr.setParameters(List(
        Parameter("EnvironmentType", rsr.envType),
        Parameter("EnableAlarmActions", rsr.enableAlarms.toString),
        Parameter("NotificationEmail", rsr.notificationEmail)))
      csr.setTemplateBody(template)
      val result = cf.createStack(csr)

      log.debug(s"Stack creating: $result")
    }
      //todo have this called by a scheduler that then pushes out result to a websocket!!!!
    case ds: DescribeStacksRequest => {
      val cf = Region.getRegion(ds.region).createClient(classOf[AmazonCloudFormationClient],
        cp, new ClientConfiguration)

      val dsr = cf.describeStacks()

      sender ! DescribeStacksResponse(dsr)
    }
  }
}