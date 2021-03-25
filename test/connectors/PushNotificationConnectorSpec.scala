package connectors

import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToXml, post, put, urlEqualTo}
import generators.ModelGenerators
import models.{BoxId, DepartureId}
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class PushNotificationConnectorSpec extends AnyFreeSpec
  with MockitoSugar
  with ScalaFutures
  with Matchers
  with IntegrationPatience
  with WiremockSuite
  with ScalaCheckPropertyChecks
  with ModelGenerators
  with OptionValues {
  override protected def portConfigKey: String = "microservice.services.push-notification.port"

  val testClientId = "ZrxDm7xXJ4kyPL4sOAj7DTfC8lQV"
  val testDepartureId = DepartureId(1)

  "PushNotifcationConnector" - {
    "createOrGetBox" - {
      "Return Future.successful(Left(HttpResponse)) when status is not 200 Ok or 201 created" in {
        val leftStatuses = Seq(Status.INTERNAL_SERVER_ERROR, Status.NOT_IMPLEMENTED, Status.SERVICE_UNAVAILABLE, Status.GATEWAY_TIMEOUT, Status.UNAUTHORIZED, Status.FORBIDDEN, Status.NOT_FOUND, Status.BAD_REQUEST, Status.ACCEPTED, Status.NO_CONTENT)
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
      "Return Future.unsuccessful() when exception is thrown" in {

      }
    }
  }
}
