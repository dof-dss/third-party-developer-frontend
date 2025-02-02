/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.connectors

import java.util.UUID

import akka.actor.ActorSystem
import connectors.ProxiedHttpClient
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.ArgumentMatchers.any

class ProxiedHttpClientSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val actorSystem = ActorSystem("test-actor-system")

  trait Setup {
    val apiKey: String = UUID.randomUUID().toString
    val bearerToken: String = UUID.randomUUID().toString
    val url = "http://example.com"
    val mockConfig: Configuration = mock[Configuration]
    val mockAuditConnector: AuditConnector = mock[AuditConnector]
    val mockWsClient: WSClient = mock[WSClient]
    val mockEnvironment: Environment = mock[Environment]
    when(mockEnvironment.mode).thenReturn(Mode.Test)
    when(mockConfig.getString(any(), any())).thenReturn(Some(""))
    when(mockConfig.getInt(any())).thenReturn(Some(0))
    when(mockConfig.getBoolean("Test.proxy.proxyRequiredForThisEnvironment")).thenReturn(Some(true))
    when(mockWsClient.url(url)).thenReturn(mock[WSRequest])

    val underTest = new ProxiedHttpClient(mockConfig, mockAuditConnector, mockWsClient, mockEnvironment, actorSystem)
  }

  "withHeaders" should {

    "creates a ProxiedHttpClient with passed in headers" in new Setup {

      private val result = underTest.withHeaders(bearerToken, apiKey)

      result.authorization shouldBe Some(Authorization(s"Bearer $bearerToken"))
      result.apiKeyHeader shouldBe Some("x-api-key" -> apiKey)
    }

    "when apiKey is empty String, apiKey header is None" in new Setup {

      private val result = underTest.withHeaders(bearerToken, "")

      result.apiKeyHeader shouldBe None
    }

    "when apiKey isn't provided, apiKey header is None" in new Setup {

      private val result = underTest.withHeaders(bearerToken)

      result.apiKeyHeader shouldBe None
    }
  }
}
