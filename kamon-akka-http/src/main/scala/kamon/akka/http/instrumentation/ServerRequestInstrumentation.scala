/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.akka.http.instrumentation

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.http.scaladsl.ConnectionContext
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{Around, Aspect}

import scala.concurrent.Future

@Aspect
class ServerRequestInstrumentation {

  @Around("execution(* akka.http.scaladsl.HttpExt.bindAndHandle(..)) && args(handler, interface, port, connectionContext, settings, log, materializer)")
  def onBindAndHandle(pjp: ProceedingJoinPoint, handler: Flow[HttpRequest, HttpResponse, Any], interface: String,
      port: Int, connectionContext: ConnectionContext, settings: ServerSettings, log: LoggingAdapter, materializer: Materializer): AnyRef = {

    val originalFLow = handler.asInstanceOf[Flow[HttpRequest, HttpResponse, NotUsed]]
    pjp.proceed(Array(ServerFlowWrapper(originalFLow, interface, port), interface, Int.box(port), connectionContext, settings, log, materializer))
  }

  @Around("execution(* akka.http.scaladsl.HttpExt.bindAndHandleAsync(..)) && args(handler, interface, port, connectionContext, settings, parallelism, log, materializer)")
  def onBindAndHandleAsync(pjp: ProceedingJoinPoint, handler: HttpRequest => Future[HttpResponse], interface: String,
      port: Int, connectionContext: ConnectionContext, settings: ServerSettings, parallelism: Int, log: LoggingAdapter, materializer: Materializer): AnyRef = {

    val wrapper = ServerFlowWrapper.async(interface, port)(handler)(materializer)
    pjp.proceed(Array(wrapper, interface, Int.box(port), connectionContext, settings, Int.box(parallelism), log, materializer))
  }

}