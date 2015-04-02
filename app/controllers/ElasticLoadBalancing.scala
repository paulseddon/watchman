package controllers

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.{Region, Regions, ServiceAbbreviations}
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient

import scala.collection.JavaConverters._
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

object ElasticLoadBalancing {

  def get(region: Regions)(implicit cp: AWSCredentialsProvider) = {
    Future {
      if (Region.getRegion(region).isServiceSupported(ServiceAbbreviations.ElasticLoadbalancing)) {
        val lb = Region.getRegion(region).createClient(classOf[AmazonElasticLoadBalancingClient],
          cp, new ClientConfiguration)

        val result = lb.describeLoadBalancers

        result.getLoadBalancerDescriptions.asScala
      } else Nil
    }
  }
}
