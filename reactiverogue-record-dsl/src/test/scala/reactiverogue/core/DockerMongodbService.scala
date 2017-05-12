package reactiverogue.core

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.{DockerContainer, DockerReadyChecker}

trait DockerMongodbService extends DockerKitSpotify {

  val DefaultMongodbPort = 27017

  val mongodbContainer = DockerContainer("mongo:3.2.4")
    .withPorts(DefaultMongodbPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}
