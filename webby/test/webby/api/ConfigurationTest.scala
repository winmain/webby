package webby.api

import org.scalatest.{Matchers, WordSpec}

class ConfigurationTest extends WordSpec with Matchers {

  def exampleConfig = Configuration.from(Map("foo.bar1" -> "value1", "foo.bar2" -> "value2", "blah" -> "value3"))

  "Configuration" should {
    "be accessible as an entry set" in {
      val map = Map(exampleConfig.entrySet.toList: _*)
      map.keySet should contain allOf("foo.bar1", "foo.bar2", "blah")
    }

    "make all paths accessible" in {
      exampleConfig.keys should contain allOf("foo.bar1", "foo.bar2", "blah")
    }

    "make all sub keys accessible" in {
      exampleConfig.subKeys should contain allOf("foo", "blah")
    }
  }
}
