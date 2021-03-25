package connectors

import com.google.inject.Inject
import config.AppConfig
import connectors.MessageConnector.EisSubmissionResult.responseToStatus
import models.{BoxId, DepartureId}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class PushNotificationConnector @Inject()(config: AppConfig, http: HttpClient)(implicit ec: ExecutionContext) {
  def createOrGetBox (clientId: String, departureId: DepartureId): Future[Either[HttpResponse, BoxId]] = {
    http
      .PUTString[HttpResponse]()
      .map(response => responseToStatus(response))
  }
}
