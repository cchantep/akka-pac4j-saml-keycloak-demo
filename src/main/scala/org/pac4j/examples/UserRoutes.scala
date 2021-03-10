package org.pac4j.examples

import scala.util.Try
import scala.collection.JavaConverters

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.server.Directives._

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }, StatusCodes.SeeOther
import akka.http.scaladsl.server.{ Route, StandardRoute }

import com.stackstate.pac4j.{
  AkkaHttpSecurity,
  AuthenticatedRequest
}, AkkaHttpSecurity._

import org.pac4j.core.config.Config

object SimplePages {
  def index: StandardRoute = {
    val html = s"""
                  |<html>
                  |<body>
                  |  <a href="/users">protected page</a><br>
                  |
                  |  <a href="/auth/login">login</a><br>
                  |
                  |  <a href="/auth/logout">logout</a><br>
                  |
                  |</form>
                  |</body>
                  |</html>
               """.stripMargin

    complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
  }
}

trait UserRoutes extends JsonSupport {
  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  def security: AkkaHttpSecurity

  def securityConfig: Config

  lazy val defaultUrl = "/"

  lazy val userRoutes: Route =
    concat(
      path("") {
        SimplePages.index
      },
      path("callback") {
        // Handle the SAML response POST'ed back by the IDP
        security.callback(defaultUrl = defaultUrl, setCsrfCookie = false)
      },
      pathPrefix("auth") {
        concat(
          path("logout") {
            security.logout(defaultUrl)
          },
          path("login") {
            get {
              security.withAuthentication(clients = "SAML2Client") { _ =>
                redirect(defaultUrl, StatusCodes.TemporaryRedirect)
              }
            }
          })
      },
      path("users") {
        security.withAuthentication(clients = "SAML2Client") { req =>
          concat(
            get(complete {
              Users(req.profiles.collect {
                case p: org.pac4j.core.profile.BasicUserProfile =>
                  def attrs: Map[String, String] =
                    JavaConverters.mapAsScalaMap(p.getAttributes).collect {
                      case (k, v) => k -> v.toString
                    }.toMap

                  User(name = p.getId, attributes = attrs)
              })
            }))
        }
      })
}

case class User(
  name: String,
  attributes: Map[String, String])

case class Users(users: Seq[User])
