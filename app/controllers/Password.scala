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

package controllers

import config.{ApplicationConfig, ErrorHandler}
import connectors.ThirdPartyDeveloperConnector
import domain._
import javax.inject.Inject
import play.api.Logger
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.twirl.api.HtmlFormat
import service.AuditAction.PasswordChangeFailedDueToInvalidCredentials
import service.{AuditService, SessionService}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import views.html._

import scala.concurrent.{ExecutionContext, Future}

trait PasswordChange {

  import ErrorFormBuilder.GlobalError
  import Results._

  val connector: ThirdPartyDeveloperConnector
  val auditService: AuditService

  def processPasswordChange(email: String, success: Result, error: Form[ChangePasswordForm] => HtmlFormat.Appendable)
                           (implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext) = {
    ChangePasswordForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(error(errors.currentPasswordGlobal().passwordGlobal().passwordNoMatchField()))),
      data => {
        val payload = ChangePassword(email, data.currentPassword, data.password)
        connector.changePassword(payload) map {
          _ => success
        } recover {
          case _: UnverifiedAccount => Forbidden(error(ChangePasswordForm.accountUnverified(ChangePasswordForm.form, email)))
            .withSession("email" -> email)
          case _: InvalidCredentials =>
            auditService.audit(PasswordChangeFailedDueToInvalidCredentials(email))
            Unauthorized(error(ChangePasswordForm.invalidCredentials(ChangePasswordForm.form)))
          case _: LockedAccount => Redirect(controllers.routes.UserLoginAccount.accountLocked())
        }
      }
    )
  }
}

class Password @Inject()(val auditService: AuditService,
                         val sessionService: SessionService,
                         val connector: ThirdPartyDeveloperConnector,
                         val errorHandler: ErrorHandler,
                         val messagesApi: MessagesApi,
                         implicit val appConfig: ApplicationConfig)(implicit ec: ExecutionContext)
  extends LoggedOutController with PasswordChange {

  import ErrorFormBuilder.GlobalError

  def showForgotPassword() = loggedOutAction { implicit request =>
    Future.successful(Ok(forgotPassword(ForgotPasswordForm.form)))
  }

  def requestReset() = Action.async { implicit request =>
    val requestForm = ForgotPasswordForm.form.bindFromRequest
    requestForm.fold(
      formWithErrors => {
        Future.successful(BadRequest(forgotPassword(formWithErrors.emailaddressGlobal())))
      },
      data => connector.requestReset(data.emailaddress) map {
        _ => Ok(checkEmail(data.emailaddress))
      } recover {
        case _: UnverifiedAccount => Forbidden(forgotPassword(ForgotPasswordForm.accountUnverified(requestForm, data.emailaddress)))
          .withSession("email" -> data.emailaddress)
        case _: NotFoundException => Ok(checkEmail(data.emailaddress))
      }
    )
  }

  def validateReset(email: String, code: String) = Action.async { implicit request =>
    connector.fetchEmailForResetCode(code) map {
      email => Redirect(routes.Password.resetPasswordChange()).addingToSession("email" -> email)
    } recover {
      case _: InvalidResetCode => Redirect(routes.Password.resetPasswordError()).flashing(
        "error" -> "InvalidResetCode")
      case _: UnverifiedAccount => Redirect(routes.Password.resetPasswordError()).flashing(
        "error" -> "UnverifiedAccount",
        "email" -> email
      )
    }
  }

  def resetPasswordChange = Action.async { implicit request =>
    request.session.get("email") match {
      case None => Logger.warn("email not found in session")
        Future.successful(Redirect(routes.Password.resetPasswordError()).flashing("error" -> "Error"))
      case Some(_) => Future.successful(Ok(reset(PasswordResetForm.form)))
    }
  }

  def resetPasswordError = Action(implicit request =>
    request.flash.get("error").getOrElse("error") match {
      case "UnverifiedAccount" =>
        val email = request.flash.get("email").getOrElse("").toString
        Forbidden(forgotPassword(ForgotPasswordForm.accountUnverified(ForgotPasswordForm.form, email))).withSession("email" -> email)
      case "InvalidResetCode" =>
        BadRequest(resetInvalid())
      case _ =>
        BadRequest(resetError())
    }
  )

  def resetPassword = Action.async { implicit request =>
    PasswordResetForm.form.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(reset(formWithErrors.passwordGlobal().passwordNoMatchField()))),
      data => {
        val email = request.session.get("email").getOrElse(throw new RuntimeException("email not found in session"))
        connector.reset(PasswordReset(email, data.password)) map {
          _ => Ok(signIn("You have reset your password", LoginForm.form, endOfJourney = true))
        } recover {
          case _: UnverifiedAccount => Forbidden(reset(PasswordResetForm.accountUnverified(PasswordResetForm.form, email)))
            .withSession("email" -> email)
        }
      }
    )
  }
}
