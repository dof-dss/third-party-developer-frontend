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
import uk.gov.hmrc.play.json.Union

case class Session(sessionId: String, sessionLoggedInState: LoggedInState, developer: Developer)

object Session {
//  implicit val format = Union.from[LoggedInState]("sessionLoggedInState")
//    .and[LOGGED_IN](LoggedInState.LOGGED_IN.toString)
//    .and[PART_LOGGED_IN_ENABLING_MFA](LoggedInState.PART_LOGGED_IN_ENABLING_MFA.toString)
//    .format

 // implicit val format: OFormat[LoggedInState] = Json.format[LoggedInState]

  implicit val formatSession: OFormat[Session] = Json.format[Session]
}



class SessionInvalid extends Throwable
