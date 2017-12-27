package ru.spbstu.kparsec

import org.junit.Test
import ru.spbstu.kparsec.parsers.*
import kotlin.test.assertEquals

class OperatorTableTest: StringsAsParsers {
    @Test
    fun `sanity check`() {
        val parser = operatorTable(Literals.FLOAT) {
            (-'!')(priority = 6, assoc = Assoc.POSTFIX){ x -> (1..x.toInt()).fold(1){ a, b -> a * b }.toDouble() }
            (-'-')(priority = 6, assoc = Assoc.PREFIX){ x -> -x }
            (-'+')(priority = 2){ a, b -> a + b }
            (-'-')(priority = 2){ a, b -> a - b }
            (-"~-")(priority = 2, assoc = Assoc.RIGHT){ a, b -> b - a }
            (-'*')(priority = 4){ a, b -> a * b }
            (-'/')(priority = 4){ a, b -> a / b }

        }

        assertEquals(3.0, parser.parse("----3").assertResult())
        assertEquals(-3.0, parser.parse("---3").assertResult())
        assertEquals(2.0, parser.parse("1 + 2.0   * 6 /  - - - 3   +   5").assertResult())
        assertEquals(0.0, parser.parse("10 ~- 10 ~- 20").assertResult())
        assertEquals(30.0, parser.parse("30 - 10 ~- 10 ~- 20").assertResult())

        assertEquals(-6.0, parser.parse("-3!").assertResult())
    }

    object OpsCalculator: StringsAsParsers {
        val atom: Parser<Char, Double> = Literals.FLOAT or (-'(' + defer { expr } + -')')
        val expr = operatorTable(atom) {
            (-'!')(priority = 6, assoc = Assoc.POSTFIX){ x -> (1..x.toInt()).fold(1){ a, b -> a * b }.toDouble() }
            (-'-')(priority = 6, assoc = Assoc.PREFIX){ x -> -x }
            (-'+')(priority = 2){ a, b -> a + b }
            (-'-')(priority = 2){ a, b -> a - b }
            (-'*')(priority = 4){ a, b -> a * b }
            (-'/')(priority = 4){ a, b -> a / b }
        }
    }

    @Test
    fun `complex`() {
        val parser = OpsCalculator.expr

        assertEquals(20.0, parser.parse("(1 + 2 * -4) + 5 * 6 + 18 / ---3 + 2! + 1!!").assertResult())

    }
}
