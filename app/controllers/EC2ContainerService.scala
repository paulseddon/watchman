package controllers

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.{Cluster, DescribeContainerInstancesRequest, ListContainerInstancesRequest}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EC2ContainerService {

  def get(region: Regions)(implicit cp: AWSCredentialsProvider) = {
    Future {
      if (region == Regions.US_EAST_1) {
        val ecs = Region.getRegion(region).createClient(classOf[AmazonECSClient],
          cp, new ClientConfiguration)

        val clusters = ecs.describeClusters.getClusters.map {
          c =>
            val ci = for {
              containerArn <- getContainerArns(ecs, c)
              containerInstance <- getContainerInstances(ecs, c, containerArn)
            } yield (containerInstance)
            (c -> ci)
        }

        clusters.groupBy(_._1).map(g => (g._1, g._2.flatMap(_._2)))

      } else Map.empty
    }
  }

  def getContainerArns(ecs: AmazonECSClient, cluster: Cluster) = {
    val request = new ListContainerInstancesRequest
    request.setCluster(cluster.getClusterName)
    ecs.listContainerInstances(request).getContainerInstanceArns
  }

  def getContainerInstances(ecs: AmazonECSClient, cluster: Cluster, containerArn: String) = {
    val request = new DescribeContainerInstancesRequest
    request.setCluster(cluster.getClusterName)
    request.setContainerInstances(Seq(containerArn))
    ecs.describeContainerInstances(request).getContainerInstances
  }

}
