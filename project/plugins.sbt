// Comment to get more information during initialization
logLevel := Level.Info

resolvers += Resolver.bintrayIvyRepo("citrum", "sbt-plugins")

addSbtPlugin("com.github.citrum" % "sbt-haxe-idea" % "0.2.3")

// For publishing to bintray
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
