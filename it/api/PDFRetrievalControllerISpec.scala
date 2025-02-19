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

package api

import api.helpers.ApiSpecBase
import cats.data.NonEmptyList
import models.{ChannelType, Departure, DepartureId, DepartureStatus, MessageStatus, MessageType, MessageWithStatus, MessageWithoutStatus}
import play.api.Application
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{BAD_GATEWAY, OK}
import repositories.DepartureRepository
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.Json
import utils.JsonHelper

import scala.concurrent.ExecutionContext.Implicits.global

class PDFRetrievalControllerISpec extends ApiSpecBase with JsonHelper {

  override implicit lazy val app: Application = appBuilder.build()

  lazy val path: Path = Paths.get(getClass.getResource("/test-files/testPDF.pdf").toURI)
  lazy val pdfFile: Array[Byte] = Files.readAllBytes(path)
  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val departureRepo: DepartureRepository = app.injector.instanceOf[DepartureRepository]

  override protected def portConfigKeys: Seq[String] = Seq(
    "microservice.services.auth.port",
    "microservice.services.manage-documents.port"
  )

  "/movements/departures/:departureId/accompanying-document" - {
    "should return a PDF if all data is present" in {

      val requestId: String = UUID.randomUUID().toString

      val departure: Departure = Departure(
        DepartureId(12),
        ChannelType.web,
        "1234567",
        None,
        "SomeReference",
        DepartureStatus.ReleaseForTransit,
        LocalDateTime.now(),
        LocalDateTime.now(),
        3,
        NonEmptyList(
          MessageWithStatus(LocalDateTime.now(), MessageType.DepartureDeclaration, <departure></departure>, MessageStatus.SubmissionSucceeded, 1, convertXmlToJson(<departure></departure>.toString)),
          List(MessageWithoutStatus(LocalDateTime.now(), MessageType.ReleaseForTransit, <released></released>, 2, convertXmlToJson(<released></released>.toString)))
        )
      )

      stubForPostWithResponseBody(
        url = "/auth/authorise",
        body = """{
          | "authorisedEnrolments": [{
          |   "key": "HMCE-NCTS-ORG",
          |   "identifiers": [{
          |     "key": "VatRegNoTURN",
          |     "value": "1234567"
          |   }],
          |   "state": "Active"
          | }]
          |}""".stripMargin.getBytes(),
        requestId = requestId
      )

      database.flatMap(_.drop()).futureValue
      departureRepo.insert(departure).futureValue
      departureRepo.get(departure.departureId).futureValue mustBe Some(departure)

      stubForPostWithResponseBody(
        url = "/transit-movements-trader-manage-documents/transit-accompanying-document",
        body = pdfFile,
        requestId = requestId,
        extraHeaders = Seq("Content-Type" -> "application/xml")
      )

      val response = wsClient
        .url(s"http://localhost:$port/transits-movements-trader-at-departure/movements/departures/12/accompanying-document")
        .withHttpHeaders("channel" -> "web", "X-Request-ID" -> requestId)
        .get().futureValue

      response.status mustBe OK
      response.bodyAsBytes mustBe pdfFile
    }

    "should return a BAD_GATEWAY if the call to the manage document service returns and unexpected result" in {

      val requestId: String = UUID.randomUUID().toString

      val departure: Departure = Departure(
        DepartureId(12),
        ChannelType.web,
        "1234567",
        None,
        "SomeReference",
        DepartureStatus.ReleaseForTransit,
        LocalDateTime.now(),
        LocalDateTime.now(),
        3,
        NonEmptyList(
          MessageWithStatus(LocalDateTime.now(), MessageType.DepartureDeclaration, <departure></departure>, MessageStatus.SubmissionSucceeded, 1, Json.obj()),
          List(MessageWithoutStatus(LocalDateTime.now(), MessageType.ReleaseForTransit, <released></released>, 2, Json.obj()))
        )
      )

      stubForPostWithResponseBody(
        url = "/auth/authorise",
        body = """{
          | "authorisedEnrolments": [{
          |   "key": "HMCE-NCTS-ORG",
          |   "identifiers": [{
          |     "key": "VatRegNoTURN",
          |     "value": "1234567"
          |   }],
          |   "state": "Active"
          | }]
          |}""".stripMargin.getBytes(),
        requestId = requestId
      )

      database.flatMap(_.drop()).futureValue
      departureRepo.insert(departure).futureValue
      departureRepo.get(departure.departureId).futureValue mustBe Some(departure)

      stubForPostWithResponseBody(
        url = "/transit-movements-trader-manage-documents/transit-accompanying-document",
        body = "Error Something Went Wrong".getBytes(),
        requestId = requestId,
        extraHeaders = Seq("Content-Type" -> "application/xml"),
        status = 500
      )

      val response = wsClient
        .url(s"http://localhost:$port/transits-movements-trader-at-departure/movements/departures/12/accompanying-document")
        .withHttpHeaders("channel" -> "web", "X-Request-ID" -> requestId)
        .get().futureValue

      response.status mustBe BAD_GATEWAY
      response.bodyAsBytes mustBe Array.empty[Byte]
    }
  }
}
