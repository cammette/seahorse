/**
 * Copyright (c) 2015, CodiLime Inc.
 */

package io.deepsense.workflowmanager.rest.actions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport

import io.deepsense.commons.auth.usercontext.UserContext
import io.deepsense.deeplang.doperables.{DOperableLoader, Deployable, DeployableLoader}
import io.deepsense.deeplang.{ExecutionContext, Model}
import io.deepsense.deploymodelservice.CreateModelResponse
import io.deepsense.deploymodelservice.DeployModelJsonProtocol._
import io.deepsense.entitystorage.EntityStorageClient
import io.deepsense.models.entities.Entity

class DeployModel {

  def deploy(
      id: Entity.Id,
      uc: UserContext,
      ec: ExecutionContext,
      entityStorageClient: EntityStorageClient): Future[CreateModelResponse] = {
    val retrieved: Deployable = DOperableLoader.load(
      entityStorageClient)(
        DeployableLoader.loadFromFs(ec.fsClient))(
        uc.tenantId, id)
    val toService = (model: Model) => {
      implicit val system = ActorSystem()
      import SprayJsonSupport._
      val pipeline = sendReceive ~> unmarshal[CreateModelResponse]
      val response: Future[CreateModelResponse] = pipeline {
        // TODO: this should not be hardcoded.
        // If this mock is going to live this needs to be replaced.
        Post("http://localhost:8082/regression", model)
      }
      response.map(_.id)
    }

    retrieved.deploy(toService).map(CreateModelResponse)
  }
}
