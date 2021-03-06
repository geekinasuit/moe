package com.google.devtools.moe.client.moshi

import com.google.common.truth.Truth.assertThat
import com.google.devtools.moe.client.InvalidProject
import com.google.devtools.moe.client.moshi.MoshiModule.Companion.provideMoshi
import com.squareup.moshi.JsonDataException
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test

class MoshiProjectConfigParserTest {
  private val configParser = MoshiProjectConfigParser(provideMoshi())

  @Throws(Exception::class)
  @Test fun testValidConfig() {
    val p = configParser.parse("{'name': 'foo', 'repositories': {'public': {'type': 'blah'}}}")
    TestCase.assertEquals("foo", p.name)
  }

  @Throws(Exception::class)
  @Test fun testInvalidConfig() {
    assertInvalidConfig("{}", "Required value 'name' missing at $")
  }

  @Throws(Exception::class)
  @Test fun testInvalidConfig2() {
  val text = "{\"name\": \"foo\", \"repositories\": {}}"
    val e = Assert.assertThrows(InvalidProject::class.java) { configParser.parse(text) }
    assertThat(e).hasMessageThat().contains("Must specify repositories")
  }

  @Test fun testInvalidEditor() {
    assertInvalidConfig(
      "{" +
        " 'name': 'foo'," +
        " 'repositories': {" +
        "   'x': {'type': 'blah'}" +
        " }," +
        " 'editors': {'e1': {}}" +
        "}",
      "Required value 'type' missing at $.editors.e1"
    )
  }

  @Test fun testInvalidTranslators1() {
    assertInvalidConfig(
      "{" +
        " 'name': 'foo'," +
        " 'repositories': {" +
        "   'x': {'type': 'blah'}" +
        " }," +
        " 'translators': [{}]" +
        "}",
      "Required value 'fromProjectSpace' (JSON name 'from_project_space') " +
        "missing at $.translators[1]"
    )
  }

  @Test fun testInvalidTranslators2() {
    assertInvalidConfig(
      "{" +
        " 'name': 'foo'," +
        " 'repositories': {" +
        "   'x': {'type': 'blah'}" +
        " }," +
        " 'translators': [{'from_project_space': 'x'}]" +
        "}",
      "Required value 'toProjectSpace' (JSON name 'to_project_space') missing at $.translators[1]"
    )
  }

  @Test fun testInvalidTranslators3() {
    val text = """
        {
          "name": "foo",
          "repositories": {
            "x": {"type": "blah"}
          },
          "translators": [{
              "from_project_space": "x",
              "to_project_space": "y"
          }]
        }
        """.trimIndent()
    val e = Assert.assertThrows(InvalidProject::class.java) { configParser.parse(text) }
    assertThat(e).hasMessageThat().contains("Translator requires steps")
  }

  @Test fun testInvalidStep1() {
    assertInvalidConfig(
      "{" +
        " 'name': 'foo'," +
        " 'repositories': {" +
        "   'x': {'type': 'blah'}" +
        " }," +
        " 'translators': [{'from_project_space': 'x'," +
        "                  'to_project_space': 'y'," +
        "                  'steps': [{'name': ''}]}]" +
        "}",
      "Required value 'editorConfig' (JSON name 'editor') missing at $.translators[0].steps[1]"
    )
  }

  @Test fun testInvalidStep2() {
    assertInvalidConfig(
      "{" +
        " 'name': 'foo'," +
        " 'repositories': {" +
        "   'x': {'type': 'blah'}" +
        " }," +
        " 'translators': [{'from_project_space': 'x'," +
        "                  'to_project_space': 'y'," +
        "                  'steps': [{'name': 'x'}]}]" +
        "}",
      "Required value 'editorConfig' (JSON name 'editor') missing at $.translators[0].steps[1]"
    )
  }

  @Test fun testMigration1() {
    assertInvalidConfig(
      "{" +
        " 'name': 'foo'," +
        " 'repositories': {" +
        "   'x': {'type': 'blah'}" +
        " }," +
        " 'migrations': [{}]" +
        "}",
      "Required value 'name' missing at $.migrations[1]"
    )
  }

  private fun assertInvalidConfig(
    text: String,
    error: String
  ) {
    try {
      configParser.parse(text)
      TestCase.fail("Expected error")
    } catch (e: InvalidProject) {
      TestCase.assertEquals("Could not parse MOE config: $error", e.message)
    } catch (e: JsonDataException) {
      TestCase.assertEquals("Could not parse MOE config: $error", e.message)
    }
  }

  @Throws(Exception::class)
  @Test fun testConfigWithMultipleRepositories() {
    val text = "{\"name\": \"foo\"," +
      "\"repositories\": {" +
      "\"internal\": {\"type\":\"svn\"}," +
      "\"internal\": {\"type\":\"svn\"}}}"
    val e = Assert.assertThrows(InvalidProject::class.java) { configParser.parse(text) }
    assertThat(e).hasMessageThat().contains("Map key 'internal' has multiple values")
  }

  @Test fun testDatabaseFile() {
    val p = configParser.parse(
      "{" +
        " 'name': 'foo'," +
        " 'database_uri': '/foo/bar/database.json'," +
        " 'repositories': {" +
        "   'x': {'type': 'blah'}" +
        " }" +
        "}"
    )
    assertThat(p.databaseUri).isEqualTo("/foo/bar/database.json")
  }

  @Test fun testDatabaseFileNull() {
    val p = configParser.parse("{'name': 'foo', 'repositories': {'x': {'type': 'blah'} } }")
    assertThat(p.databaseUri).isNull()
  }

  @Test fun testInvalidRepositoryType_NotSet() {
    val text = "{\"name\": \"foo\"," + "\"repositories\": {" + "\"internal\": {}}}"
    val e = Assert.assertThrows(InvalidProject::class.java) { configParser.parse(text) }
    assertThat(e)
      .hasMessageThat()
      .contains("Required value 'type' missing at $.repositories.internal")
  }

  @Test fun testInvalidRepositoryType_SetBlank() {
    val text = """{"name": "foo", "repositories": { "internal": { "type": " "}}}"""
    val e = Assert.assertThrows(InvalidProject::class.java) { configParser.parse(text) }
    assertThat(e).hasMessageThat().contains("Must set repository type.")
  }
}
