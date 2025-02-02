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

package helpers

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import config.ApplicationConfig
import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait Retries {

  protected def actorSystem: ActorSystem

  protected def appConfig: ApplicationConfig

  protected def futureTimeout: FutureTimeoutSupport

  implicit val ec: ExecutionContext

  def retry[A](block: => Future[A]): Future[A] = {
    def loop(previousRetryAttempts: Int = 0)(block: => Future[A]): Future[A] = {
      block.recoverWith {
        case ex: BadRequestException if previousRetryAttempts < appConfig.retryCount => {
          val retryAttempt = previousRetryAttempts + 1
          val delay = FiniteDuration(appConfig.retryDelayMilliseconds, TimeUnit.MILLISECONDS)
          Logger.warn(s"Retry attempt $retryAttempt of ${appConfig.retryCount} in $delay due to '${ex.getMessage}'")
          futureTimeout.after(delay, actorSystem.scheduler)(loop(retryAttempt)(block))
        }
      }
    }

    loop()(block)
  }
}

class FutureTimeoutSupportImpl @Inject() extends FutureTimeoutSupport {}
