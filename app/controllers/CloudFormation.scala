package controllers

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.{Region, Regions, ServiceAbbreviations}
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CloudFormation {

  def get(region: Regions)(implicit cp: AWSCredentialsProvider) = {
    Future {
      if (Region.getRegion(region).isServiceSupported(ServiceAbbreviations.CloudFormation)) {
        val cf = Region.getRegion(region).createClient(classOf[AmazonCloudFormationClient],
          cp, new ClientConfiguration)

        val result = cf.describeStacks

        result.getStacks.asScala
      } else Nil
    }
  }
}
