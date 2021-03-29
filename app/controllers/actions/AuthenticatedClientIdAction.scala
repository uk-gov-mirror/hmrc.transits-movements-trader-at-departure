package controllers.actions

import config.AppConfig
import models.request.{AuthenticatedClientRequest, AuthenticatedRequest}
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedClientIdAction @Inject()(override val authConnector: AuthConnector)(implicit val executionContext: ExecutionContext) extends ActionRefiner[AuthenticatedRequest, AuthenticatedClientRequest] with AuthorisedFunctions {
  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedClientRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    authorised()
      .retrieve(Retrievals.clientId) {
        case Some(clientId) => Future.successful(Right(AuthenticatedClientRequest(request, request.channel, request.eoriNumber, clientId)))
        case None => Future.successful(Left(BadRequest("Unable to retrieve clientId")))
      }
  }
}
