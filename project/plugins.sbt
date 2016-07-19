// Comment to get more information during initialization
logLevel := Level.Info

// The zeroturnaround.com repository
resolvers += "zeroturnaround repository" at "https://repos.zeroturnaround.com/nexus/content/repositories/zt-public/"

// Temporary repo for querio
resolvers += Resolver.bintrayRepo("citrum", "maven")

// For publishing to bintray
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
