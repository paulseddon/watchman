package controllers

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.{Region, Regions, ServiceAbbreviations}
import com.amazonaws.services.route53.AmazonRoute53Client
import com.amazonaws.services.route53.model.{GetHostedZoneRequest, ListResourceRecordSetsRequest, ResourceRecordSet}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Route53(hostedZone: String, recordSets: Seq[ResourceRecordSet])

object Route53 {

  val hostedZoneId = ConfigFactory.load.getString("aws.route53.hostedzone.id")

  def get(region: Regions)(implicit cp: AWSCredentialsProvider) = {
    Future {
      if (Region.getRegion(region).isServiceSupported(ServiceAbbreviations.Route53)) {

        val r = Region.getRegion(region).createClient(classOf[AmazonRoute53Client],
          cp, new ClientConfiguration)

        val name = r.getHostedZone(new GetHostedZoneRequest(hostedZoneId)).getHostedZone.getName

        val req = new ListResourceRecordSetsRequest(hostedZoneId)
        val rs = r.listResourceRecordSets(req).getResourceRecordSets.asScala

        Some(Route53(name, rs))

      } else None
    }
  }
}
