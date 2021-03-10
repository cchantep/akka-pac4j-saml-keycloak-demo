organization := "org.pac4j.examples"

name := "akka-http-pac4j-demo"

scalaVersion := "2.12.13"

resolvers ++= Seq(
  "Shibboleth releases" at "https://build.shibboleth.net/nexus/content/repositories/releases/")

libraryDependencies ++= {
  val akkaHttpVersion = "10.2.0"
  val akkaVersion    = "2.6.9"

  val akkaHttpPac4jVersion = "0.6.1"
  val pac4jVersion = "4.4.0"

  Seq(
      "com.stackstate" %% "akka-http-pac4j"         % akkaHttpPac4jVersion,
      // "org.pac4j" % "pac4j"  % pac4jVersion,
      // "org.pac4j" % "pac4j-core" % pac4jVersion,
      "org.pac4j" % "pac4j-jwt" % pac4jVersion exclude("commons-io" , "commons-io"),
      //   "org.pac4j" % "pac4j-sql" % "2.1.0",
      "org.pac4j" % "pac4j-http" % pac4jVersion,
      "org.pac4j" % "pac4j-ldap" % pac4jVersion,
      "org.pac4j" % "pac4j-saml-opensamlv3" % pac4jVersion,

      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion
  )
}
