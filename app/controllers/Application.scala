package controllers

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.{Region, Regions, ServiceAbbreviations}
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.cloudformation.model.{Parameter, Stack}
import com.amazonaws.services.ecs.model.{Cluster, ContainerInstance}
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.typesafe.config.ConfigFactory
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.collection.JavaConversions._

case class Dashboard(route53: Option[Route53],
                     elasticLoadBalancing: Seq[LoadBalancerDescription],
                     autoScaling: Seq[AutoScalingGroup],
                     ec2ContainerService: Map[_ <: Cluster, Seq[ContainerInstance]],
                     cloudFormation: Seq[Stack])

object Application extends Controller {

  val awsRegionCookie = "aws-region"
  val conf = ConfigFactory.load
  val cred = new BasicAWSCredentials(conf.getString("aws.key.access"), conf.getString("aws.key.secret"))

  implicit val cp = new StaticCredentialsProvider(cred)

  val system = ActorSystem("awsSystem")
  val stackMgr = system.actorOf(Props[StackManager], "stackManager")
  implicit val timeout = Timeout(5 seconds)

//  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
//    WebSocketActor.props(out)
//  }

  implicit def nameToRegion(region: String): Regions = {
    Regions.fromName(region)
  }

  def dashboard = Action.async { implicit request =>
    //todo enhance request to include region
    val region = findRegion

    val dash = for {
      r53 <- Route53.get(region)
      elb <- ElasticLoadBalancing.get(region)
      as <- AutoScaling.get(region)
      ecs <- EC2ContainerService.get(region)
      cf <- CloudFormation.get(region)
    } yield (Dashboard(r53, elb, as, ecs, cf))

    dash.map(d => Ok(views.html.dashboard(conf.getString("application.name"), region, d)).withCookies(Cookie(awsRegionCookie, region)))
  }

  case class EnvRequest(stackName: String, stackType: String, enableAlarms: Boolean, notificationEmail: String)

  val envRequestForm = Form(
    mapping(
      "stackName" -> text(3, 15),
      "stackType" -> text,
      "enableAlarms" -> boolean,
      "notificationEmail" -> email
    )(EnvRequest.apply)(EnvRequest.unapply)
  )

  def stacks = Action.async { implicit request =>
    val region = findRegion

    // todo websocket and tell only
    val response = (stackMgr ? DescribeStacksRequest(region)).mapTo[DescribeStacksResponse]

    response.map(dsr =>
      Ok(views.html.stacks(conf.getString("application.name"), region,
        request.flash.get("info").getOrElse(""), envRequestForm, dsr.result.getStacks)))
  }

  def createStack = Action { implicit request =>
    val region = findRegion

    envRequestForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.stacks(conf.getString("application.name"), region, "Please fix errors", formWithErrors, null))
      },
      envRequest => {
        if (Region.getRegion(region).isServiceSupported(ServiceAbbreviations.CloudFormation)) {
          stackMgr ! RegionStackRequest(region, envRequest.stackName, envRequest.stackType, envRequest.enableAlarms, envRequest.notificationEmail)
          Redirect(routes.Application.stacks()).flashing(("info", "Stack request successful"))
        } else {
          Redirect(routes.Application.stacks()).flashing(("info", "Not supported in region"))
        }
      }
    )
  }

  private def findRegion(implicit request: Request[AnyContent]) = {
    request.getQueryString("region") match {
      case Some(region) => region
      case None => {
        val maybeCookie = request.cookies.find(_.name == awsRegionCookie)
        maybeCookie match {
          case Some(cookie) => cookie.value
          case None => conf.getString("aws.region.default")
        }
      }
    }
  }

  object Parameter {
    def apply(key: String, value: String) = {
      val param = new Parameter
      param.setParameterKey(key)
      param.setParameterValue(value)
      param
    }
  }

  object WebSocketActor {
    def props(out: ActorRef) = Props(new WebSocketActor(out))
  }

  class WebSocketActor(out: ActorRef) extends Actor {
    override def receive: Receive = {
      case msg: String => {
        println(s"Received: $msg")
        out ! s"Received: msg"
      }
    }

    override def postStop() = println("Clean resources here")
  }

}