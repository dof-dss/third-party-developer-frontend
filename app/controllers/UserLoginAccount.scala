/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import domain._
import jp.t2v.lab.play2.auth.LoginLogout
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import service.AuditAction.{LoginFailedDueToInvalidEmail, LoginFailedDueToInvalidPassword, LoginFailedDueToLockedAccount, LoginSucceeded}
import service._
import uk.gov.hmrc.http.HeaderCarrier
import views.html._

import scala.concurrent.Future

trait Auditing {
  val auditService: AuditService

  def audit(auditAction: AuditAction, data: Map[String, String])(implicit hc: HeaderCarrier) = {
    auditService.audit(auditAction, data)
  }

  def audit(auditAction: AuditAction, developer: Developer)(implicit hc: HeaderCarrier) = {
    auditService.audit(auditAction, Map("developerEmail" -> developer.email, "developerFullName" -> developer.displayedName))
  }
}

trait UserLoginAccount extends LoggedOutController with LoginLogout with Auditing {

  import play.api.data._

  val loginForm: Form[LoginForm] = LoginForm.form
  val changePasswordForm: Form[ChangePasswordForm] = ChangePasswordForm.form
  val applicationService: ApplicationService

  def login = loggedOutAction { implicit request =>
    Future.successful(Ok(signIn("Sign in", loginForm)))
  }

  def accountLocked = Action.async  { implicit request =>
    for {
      _ <- tokenAccessor.extract(request)
        .map(sessionService.destroy)
        .getOrElse(Future.successful(()))
    } yield Locked(views.html.accountLocked())
  }

  def authenticate = Action.async {
    implicit request =>
      val requestForm = loginForm.bindFromRequest
      requestForm.fold(
        errors => Future.successful(BadRequest(signIn("Sign in", errors))),
        login => sessionService.authenticate(login.emailaddress, login.password) flatMap { session => {
          audit(LoginSucceeded, session.developer)
            gotoLoginSucceeded(session.sessionId)
        }
        } recover {
          case e: InvalidEmail =>
            audit(LoginFailedDueToInvalidEmail, Map("developerEmail" -> login.emailaddress))
            Unauthorized(signIn("Sign in", LoginForm.invalidCredentials(requestForm, login.emailaddress)))
          case e: InvalidCredentials =>
            audit(LoginFailedDueToInvalidPassword, Map("developerEmail" -> login.emailaddress))
            Unauthorized(signIn("Sign in", LoginForm.invalidCredentials(requestForm, login.emailaddress)))
          case e: LockedAccount =>
            audit(LoginFailedDueToLockedAccount, Map("developerEmail" -> login.emailaddress))
            Locked(signIn("Sign in", LoginForm.accountLocked(requestForm)))
          case e: UnverifiedAccount => Forbidden(signIn("Sign in", LoginForm.accountUnverified(requestForm, login.emailaddress)))
            .withSession("email" -> login.emailaddress)
        }
      )
  }


  def get2SVHelpConfirmationPage() = loggedOutAction { implicit request =>
    Future.successful(Ok(views.html.protectAccountNoAccessCode(Help2SVConfirmForm.form)))
  }

  def get2SVHelpCompletionPage() = loggedOutAction { implicit request =>
    Future.successful(Ok(views.html.protectAccountNoAccessCodeComplete()))
  }

  def confirm2SVHelp() = loggedOutAction { implicit request =>
    Help2SVConfirmForm.form.bindFromRequest.fold(form => {
      Future.successful(BadRequest(views.html.protectAccountNoAccessCode(form)))
    },
      form => {
        form.helpRemoveConfirm match {
          case Some("Yes") => applicationService.request2SVRemoval("", "").map(_ => Ok(protectAccountNoAccessCodeComplete()))

          case _ => Future.successful(Ok(signIn("Sign in", loginForm)))
        }
      })
  }

}

object UserLoginAccount extends UserLoginAccount with WithAppConfig {
  override val sessionService = SessionService
  override val auditService = AuditService
  override val applicationService = ApplicationServiceImpl

}
