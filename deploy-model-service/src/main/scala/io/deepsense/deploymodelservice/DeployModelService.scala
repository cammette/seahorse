/**
 * Copyright (c) 2015, CodiLime Inc.
 */

package io.deepsense.deploymodelservice

import java.util.UUID

import scala.collection.mutable

import akka.actor.Actor
import spray.http.HttpHeaders
import spray.routing._

import io.deepsense.deploymodelservice.DeployModelJsonProtocol._

class DeployModelServiceActor extends Actor with DeployModelService {

  override def actorRefFactory = context

  override def receive = runRoute(myRoute)

  override val repository: ModelRepository = new ModelRepository()
}

trait DeployModelService extends HttpService {

  val path = "regression"
  val repository: ModelRepository

  val AccessControlAllowAll = HttpHeaders.RawHeader(
    "Access-Control-Allow-Origin", "*"
  )
  val AccessControlAllowHeadersAll = HttpHeaders.RawHeader(
    "Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept"
  )
  val myRoute = pathPrefix(path) {
    path(JavaUUID) { id =>
      options {
        respondWithHeaders(AccessControlAllowAll, AccessControlAllowHeadersAll) {
          complete("")
        }
      } ~
      post {
        respondWithHeaders(AccessControlAllowAll) {
          entity(as[GetScoringRequest]) { request =>
            println("get " + id.toString())
            val model = repository(id)
            val score = model.score(request)
            complete(ScoreResult(score))
          }
        }
      }
    } ~
    post {
      entity(as[Model]) { model =>
        println("post " + model.toString())
        val uuid = UUID.randomUUID()
        repository.put(uuid, model)
        println(model)
        println(CreateResult(uuid.toString))
        complete(CreateResult(uuid.toString))
      }
    }
  }
}

case class ModelRepository() extends mutable.HashMap[UUID, Model]

case class CreateResult(id: String)

case class GetScoringRequest(features: Seq[Double])

case class ScoreResult(score: Double)
