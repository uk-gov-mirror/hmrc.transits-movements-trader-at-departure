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

import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToXml
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import generators.ModelGenerators
import models.BoxId
import models.DepartureId
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class PushNotificationConnectorSpec
    extends AnyFreeSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with WiremockSuite
    with ScalaCheckPropertyChecks
    with ModelGenerators
    with OptionValues {
  override protected def portConfigKey: String = "microservice.services.push-notification.port"

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val testClientId    = "ZrxDm7xXJ4kyPL4sOAj7DTfC8lQV"
  val testDepartureId = DepartureId(1)

  "PushNotifcationConnector" - {
    "createOrGetBox" - {
      "Return Future.successful(Left(HttpResponse)) when status is not 200 Ok or 201 created" in {
        val leftStatuses = Seq(
          Status.INTERNAL_SERVER_ERROR,
          Status.NOT_IMPLEMENTED,
          Status.SERVICE_UNAVAILABLE,
          Status.GATEWAY_TIMEOUT,
          Status.UNAUTHORIZED,
          Status.FORBIDDEN,
          Status.NOT_FOUND,
          Status.BAD_REQUEST
        )
        leftStatuses.foreach(s => {
          server.stubFor(
            put(urlEqualTo("/box"))
              .willReturn(
                aResponse()
                  .withStatus(s)
              )
          )
          val app = appBuilder.build()
          running(app) {
            val connector = app.injector.instanceOf[PushNotificationConnector]

            val result = connector.createOrGetBox(testClientId, testDepartureId)
            result.futureValue mustBe a[Left[HttpResponse, _]]
          }
        })
      }

      "Return Future.successful(Right(BoxId)) when status is 200 or 201" in {
        val validStatuses = Seq(Status.OK, Status.CREATED)
        validStatuses.foreach(s => {
          server.stubFor(
            put(urlEqualTo("/box"))
              .willReturn(
                aResponse()
                  .withBody(Json.obj("boxId" -> "1c5b9365-18a6-55a5-99c9-83a091ac7f26").toString())
                  .withStatus(s)
              )
          )
          val app = appBuilder.build()
          running(app) {
            val connector = app.injector.instanceOf[PushNotificationConnector]

            val result = connector.createOrGetBox(testClientId, testDepartureId)
            result.futureValue mustBe a[Right[_, BoxId]]
          }
        })
      }
      "Return Future.unsuccessful() when exception is thrown" in {}
    }
  }
}
