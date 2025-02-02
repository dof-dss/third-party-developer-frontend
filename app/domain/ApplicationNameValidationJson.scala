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

package domain

import play.api.libs.json.{Json, OFormat}

object ApplicationNameValidationJson {

  case class ApplicationNameValidationRequest(applicationName: String, selfApplicationId: Option[String])

  object ApplicationNameValidationRequest {
    implicit val format: OFormat[ApplicationNameValidationRequest] = Json.format[ApplicationNameValidationRequest]
  }

  case class ApplicationNameValidationResult(errors: Option[Errors])
  case class Errors(invalidName: Boolean, duplicateName: Boolean)

  object ApplicationNameValidationResult {
    def apply(applicationNameValidationResult: ApplicationNameValidationResult) : ApplicationNameValidation  = {
      applicationNameValidationResult.errors match {
        case Some(errors) => Invalid(errors.invalidName,errors.duplicateName)
        case None => Valid
      }
    }

    implicit val formatErrors: OFormat[Errors] = Json.format[Errors]
    implicit val format: OFormat[ApplicationNameValidationResult] = Json.format[ApplicationNameValidationResult]
  }
}

