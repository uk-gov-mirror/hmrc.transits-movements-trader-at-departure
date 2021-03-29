/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import com.google.inject.Inject
import config.AppConfig
import models.BoxId
import models.DepartureId
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.json.Reads
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PushNotificationConnector @Inject()(config: AppConfig, http: HttpClient)(implicit ec: ExecutionContext) extends Status {

  protected def extractIfSuccessful[T](response: HttpResponse)(implicit reads: Reads[T]): Either[HttpResponse, T] =
    if (is2xx(response.status)) {
      response.json.asOpt[T] match {
        case Some(instance) => Right(instance)
        case _              => Left(response) // TODO Reevaluate behaviour, should this be an internal server error
      }
    } else Left(response)

  def createOrGetBox(clientId: String, departureId: DepartureId)(implicit headerCarrier: HeaderCarrier): Future[Either[HttpResponse, BoxId]] = {

    val url     = s"${config.pushNotificationUrl}box"
    val boxName = s"Departure messages for ${departureId.index}" // TODO Make sure UrlEncoded
    val body    = Json.obj("clientId" -> clientId, "boxName" -> boxName)

    val newHeaders = headerCarrier
      .copy(authorization = None)

    http
      .PUTString[HttpResponse](url, body.toString())(readRaw, hc = newHeaders, implicitly)
      .map(r => extractIfSuccessful[BoxId](r))
  }
}
