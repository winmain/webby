// Comment to get more information during initialization
logLevel := Level.Info

resolvers += Resolver.bintrayIvyRepo("citrum", "sbt-plugins")

// For publishing to bintray
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
