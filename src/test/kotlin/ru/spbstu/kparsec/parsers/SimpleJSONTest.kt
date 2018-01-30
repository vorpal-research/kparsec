package ru.spbstu.kparsec.parsers

import org.junit.Test
import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.assertResult
import ru.spbstu.kparsec.examples.SimpleJSONParser
import ru.spbstu.kparsec.parse
import kotlin.test.assertEquals

class SimpleJSONTest {

    val parser: Parser<Char, Any?> = SimpleJSONParser

    @Test
    fun whatever() {
        println(parser.description)
    }

    @Test
    fun `SimpleJSON parser parses simple values`() {
        assertEquals(23.0, parser.parse("23").assertResult())
        assertEquals(33.0, parser.parse("33.0").assertResult())
        assertEquals("hahaha", parser.parse(""""hahaha"""").assertResult())
        assertEquals(true, parser.parse("true").assertResult())
        assertEquals(1e6, parser.parse("1e6").assertResult())
        assertEquals(null, parser.parse("null").assertResult())
    }

    @Test
    fun `SimpleJSON parser parses arrays`() {
        assertEquals(listOf(23.0, "Hello"), parser.parse("""[23, "Hello"] """).assertResult())
        assertEquals(listOf<Any>(), parser.parse(""" [] """).assertResult())
        assertEquals(listOf(1.0), parser.parse(""" [1] """).assertResult())
        assertEquals(listOf(true, false), parser.parse(""" [true, false] """).assertResult())
    }

    @Test
    fun `SimpleJSON parser parses objects`() {
        assertEquals(listOf("a" to 23.0, "b" to "Hello"),
                parser.parse("""{ "a": 23, "b": "Hello" }""").assertResult())

        assertEquals(listOf<Any>(), parser.parse("{}").assertResult())
        assertEquals(listOf("x" to null),
                parser.parse("""{ "x": null }""").assertResult())
        assertEquals(listOf("x" to 0.0, "y" to listOf(3.0, 4.0, true, null)),
                parser.parse("""{ "x": 0, "y" : [3,4,   true,null] }""").assertResult())
    }

    @Test
    fun `SimpleJSON complex cases`() {
        run {
            val inp = /*language=JSON*/ """
                {
                  "a": [1,2,3,  4, null],
                  "b": null,
                  "c": "cafebabe\n\t\u0050",
                  "d": {
                    "x": [1,2,3],
                    "y": 86,
                    "z": {
                      "fffo": 67
                    }
                  }
                }
            """.trimIndent()
            assertEquals(
                    listOf(
                            "a" to listOf(1.0, 2.0, 3.0, 4.0, null),
                            "b" to null,
                            "c" to "cafebabe\n\t\u0050",
                            "d" to listOf(
                                    "x" to listOf(1.0, 2.0, 3.0),
                                    "y" to 86.0,
                                    "z" to listOf("fffo" to 67.0)
                            )
                    ),
                    parser.parse(inp).assertResult()
            )
        }
    }

}
