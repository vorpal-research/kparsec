package ru.spbstu.kparsec.parsers

import org.junit.Test
import ru.spbstu.kparsec.assertResult
import ru.spbstu.kparsec.examples.*
import ru.spbstu.kparsec.parse
import kotlin.test.assertEquals

class SimpleImperativeLanguageParserTest {
    val parser = SimpleImperativeLanguageParser
    fun parse(input: String) = parser.parse(input).assertResult()

    @Test
    fun `sanity check`() {
        assertEquals(DoubleLiteral(2.0), parse("2.0"))
        assertEquals(BinaryExpression(BinaryOperator.ADD, DoubleLiteral(2.0), Variable("x")),
                parse("2 + x"))
        assertEquals(Block(DoubleLiteral(0.0)), parse("{ 0 }"))
        assertEquals(Block(Block(Block(Variable("x")))), parse("{{{x}}}"))
    }

    @Test
    fun `comments`() {
        assertEquals(DoubleLiteral(2.0), parse("2.0 /* comment */ \n\n"))
    }

    @Test
    fun `bigger examples`() {
        assertEquals(
                Block(
                        Declaration("x"),
                        Declaration("y"),
                        Assignment(Variable("x"), DoubleLiteral(5.0)),
                        Assignment(
                                Variable("y"),
                                BinaryExpression(
                                        BinaryOperator.ADD,
                                        BinaryExpression(
                                                BinaryOperator.ADD,
                                                Variable("x"),
                                                DoubleLiteral(4.0)
                                        ),
                                        DoubleLiteral(6.0)
                                )
                        ),
                        Declaration("z"),
                        Assignment(
                                Variable("z"),
                                BinaryExpression(
                                        BinaryOperator.DIV,
                                        BinaryExpression(
                                                BinaryOperator.MUL,
                                                Variable("x"),
                                                Variable("y")
                                        ),
                                        DoubleLiteral(2.0)
                                )
                        ),
                        If(
                                CompareExpression(
                                        CompareOperator.EQ,
                                        CompareExpression(
                                                CompareOperator.EQ,
                                                Variable("z"),
                                                DoubleLiteral(0.0)
                                        ),
                                        TrueLiteral
                                ),
                                Block(Assignment(Variable("z"), DoubleLiteral(5.0))),
                                null
                        ),
                        If(
                                CompareExpression(
                                        CompareOperator.NE,
                                        Variable("x"),
                                        DoubleLiteral(2.0)
                                ),
                                Block(
                                        Assignment(
                                                Variable("z"),
                                                DoubleLiteral(16.0)
                                        )
                                ),
                                Assignment(Variable("z"), DoubleLiteral(54.0))
                        ),
                        While(
                                Variable("z"),
                                Block(
                                        Assignment(
                                                Variable("z"),
                                                BinaryExpression(
                                                        BinaryOperator.SUB,
                                                        Variable("z"),
                                                        DoubleLiteral(1.0)
                                                )
                                        )
                                )
                        )
                ),
                parse(
                    """
                        {
                            var x;
                            var y;
                            x = 5;
                            y = x + 4 + 6; /* this is an interesting line ok */
                            var z;
                            z = x * y / 2;
                            if ((z == 0) == true) { z = 5 };
                            if(x != 2) { z = 16 } else z = 54;

                            while(z) { z = z - 1 }
                        }
                    """.trimIndent()
                )
        )
    }
}
