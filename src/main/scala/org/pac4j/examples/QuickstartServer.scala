package org.pac4j.examples

import java.io.File

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.{ Failure, Success }

import akka.actor.{ ActorRef, ActorSystem }

import akka.stream.ActorMaterializer

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.{ Route, RouteResult }
import akka.http.scaladsl.model.StatusCodes._

import com.stackstate.pac4j.{ AkkaHttpSecurity, AkkaHttpWebContext }
import com.stackstate.pac4j.http.AkkaHttpSessionStore
import com.stackstate.pac4j.store.InMemorySessionStorage

import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.HttpConstants

import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.exception.http.HttpAction

import org.pac4j.saml.client.SAML2Client
import org.pac4j.saml.config.SAML2Configuration

import org.pac4j.http.client.indirect.FormClient

import com.stackstate.pac4j.http.AkkaHttpActionAdapter

object QuickstartServer extends App with UserRoutes {

  import scala.concurrent.duration

  // set up ActorSystem and other dependencies here
  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  lazy val securityConfig = new SampleAuthConfiguration().buildConfig
  import scala.concurrent.duration._
  val sessionStorage = new InMemorySessionStorage(sessionLifetime = 10.minutes) // FIXME: make configurable
  lazy val security: AkkaHttpSecurity = new AkkaHttpSecurity(securityConfig, sessionStorage, sessionCookieName = "SessionId")

  // from the UserRoutes trait
  lazy val routes: Route = userRoutes

  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 9000)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}

class SampleAuthConfiguration() {
  def buildConfig = {

    def saml2Client: SAML2Client = {
      // Configured in Keycloak admin,
      // also matches md:EntitiesDescriptor/@entityID in SPSSODescriptor
      val clientId = "pac4-demo-client"

      // Different keystore and key passwords
      val cfg = new SAML2Configuration("resource:keystore.jks", "pac4-store-pass", "pac4-key-pass", "resource:idpssodescriptor.xml")

      cfg.setMaximumAuthenticationLifetime(3600)
      cfg.setServiceProviderEntityId(clientId)
      cfg.setServiceProviderMetadataResourceFilepath("resource:sp-metadata.xml")

      new SAML2Client(cfg)
    }

    lazy val securityConfig: Config = {
      // val callbackBaseUrl = "/auth"
      // SAML v2 requires a full baseUrl (with host)
      val callbackBaseUrl = "http://localhost:9000"

      val loginUrl = "/auth/login"

      // modify here to any other Auth method supported by pac4j
      val clients = new Clients(callbackBaseUrl + "/callback", saml2Client)
      val config = new Config(clients)

      // make non-authorized not redirect!
      config.setHttpActionAdapter(new ForbiddenWithoutRedirectActionAdapter())
      config
    }

    securityConfig

  }
}

class ForbiddenWithoutRedirectActionAdapter extends HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext] {
  override def adapt(action: HttpAction, context: AkkaHttpWebContext): Future[Complete] = {
    action.getCode match {
      // Prevent FormClient to redirect to loginUrl when page is unaccessible
      // FIXME: this is a bit hacky, but works OK :)
      case HttpConstants.TEMPORARY_REDIRECT if context.getChanges.headers.find(_.name == "Location").exists(_.value().contains("login")) =>
        // context.addResponseSessionCookie()
        val ct = ContentTypes.`text/html(UTF-8)`
        val entity = HttpEntity(contentType = ct, "Forbidden".getBytes("UTF-8"))
        Future.successful(Complete(HttpResponse(Forbidden, entity = entity)))

      case _ =>
        AkkaHttpActionAdapter.adapt(action, context)
    }
  }
}
