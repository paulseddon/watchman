package controllers

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.{Region, Regions, ServiceAbbreviations}
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AutoScaling {

  def get(region: Regions)(implicit cp: AWSCredentialsProvider) = {
    Future {
      if (Region.getRegion(region).isServiceSupported(ServiceAbbreviations.Autoscaling)) {
        val as = Region.getRegion(region).createClient(classOf[AmazonAutoScalingClient],
          cp, new ClientConfiguration)

        val result = as.describeAutoScalingGroups

        result.getAutoScalingGroups.asScala
      } else Nil
    }
  }
}
