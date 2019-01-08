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

package unit.views

import config.ApplicationConfig
import domain._
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.DefaultMessagesApi
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils
import utils.ViewHelpers.elementExistsByText


class DeleteApplicationCompleteSpec extends UnitSpec with GuiceOneServerPerSuite with MockitoSugar {

  val appConfig = mock[ApplicationConfig]

  "delete application complete page" should {
    "render with no errors" in {

      val request = FakeRequest().withCSRFToken
      implicit val messages = new DefaultMessagesApi().preferred(request)

      val appId = "1234"
      val clientId = "clientId123"
      val loggedInUser = Developer("developer@example.com", "John", "Doe")
      val application = Application(appId, clientId, "App name 1", DateTimeUtils.now, Environment.PRODUCTION, Some("Description 1"),
        Set(Collaborator(loggedInUser.email, Role.ADMINISTRATOR)), state = ApplicationState.production(loggedInUser.email, ""),
        access = Standard(redirectUris = Seq("https://red1", "https://red2"), termsAndConditionsUrl = Some("http://tnc-url.com")))


      val page = views.html.deleteApplicationComplete.render(application, request, loggedInUser, messages, appConfig, "details")
      page.contentType should include("text/html")

      val document = Jsoup.parse(page.body)

      elementExistsByText(document, "h1", "Request submitted") shouldBe true
    }
  }

}
