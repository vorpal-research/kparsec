package ru.spbstu.kparsec.parsers

import org.junit.Test
import ru.spbstu.kparsec.assertResult
import ru.spbstu.kparsec.examples.tip.*
import ru.spbstu.kparsec.examples.tip.Function
import ru.spbstu.kparsec.parse
import kotlin.test.assertEquals

class TipParserTest {
    @Test
    fun `test parsing`() {
        run {
            val code = """
                main() {
                    var x,y,z;
                    x = input;
                    y = alloc;
                    *x = y;
                    z = *(y);
                    return x;
                }

            """.trimIndent()

            assertEquals(
                    Program(listOf(
                            Function(
                                    "main",
                                    listOf(),
                                    listOf("x", "y", "z"),
                                    Block(listOf(
                                            Assignment("x", Input),
                                            Assignment("y", Alloc),
                                            Store("x", Variable("y")),
                                            Assignment("z", Load(Variable("y")))
                                    )),
                                    Variable("x")
                            )
                    )),
                    TIPParser.parse(code).assertResult()
            )


        }
    }
}
