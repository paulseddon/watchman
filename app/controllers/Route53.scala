package controllers

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.{Region, Regions, ServiceAbbreviations}
import com.amazonaws.services.route53.AmazonRoute53Client
import com.amazonaws.services.route53.model.{GetHostedZoneRequest, ListResourceRecordSetsRequest, ResourceRecordSet}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Route53(hostedZone: String, recordSets: Seq[ResourceRecordSet])

object Route53 {

  def get(region: Regions)(implicit cp: AWSCredentialsProvider) = {
    Future {
      if (Region.getRegion(region).isServiceSupported(ServiceAbbreviations.Route53)) {

        val r = Region.getRegion(region).createClient(classOf[AmazonRoute53Client],
          cp, new ClientConfiguration)

        //todo get whole list
        val maybeHostedZone = r.listHostedZones.getHostedZones.asScala.headOption
        maybeHostedZone match {
          case Some(hostedZone) => {
            val name = r.getHostedZone(new GetHostedZoneRequest(hostedZone.getId)).getHostedZone.getName

            val req = new ListResourceRecordSetsRequest(hostedZone.getId)
            val rs = r.listResourceRecordSets(req).getResourceRecordSets.asScala

            Some(Route53(name, rs))
          }
          case None => None
        }

      } else None
    }
  }
}
