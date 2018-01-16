package ru.spbstu.kparsec.parsers

import org.junit.Assert
import org.junit.Test
import ru.spbstu.kparsec.assertFail
import ru.spbstu.kparsec.assertResult
import ru.spbstu.kparsec.examples.ComplexCalculatorParser
import ru.spbstu.kparsec.parse
import kotlin.test.assertEquals

class ComplexCalculatorTest {
    val parser = ComplexCalculatorParser

    fun assertCalc(input: String, result: Double) {
        assertEquals(result, parser.parse(input).assertResult())
    }

    fun assertCalc(input: String, result: Double, delta: Double) {
        Assert.assertEquals(result, parser.parse(input).assertResult(), delta)
    }

    fun assertFails(input: String) = parser.parse(input).assertFail()

    @Test
    fun `sanity checks`() {
        assertCalc("2", 2.0)
        assertCalc("(2)", 2.0)
        assertCalc("((2.0))", 2.0)
        assertFails("(2")
        assertFails("((2)")
    }

    @Test
    fun `simple calculations`() {
        assertCalc("2 + 3 * 5", 17.0)
        assertCalc("2! * 3 + 4", 10.0)
        assertCalc("-3!", -6.0)
        assertCalc("sqrt(sqrt(81)) - 3", 0.0)
        assertCalc("sqrt(80 + 1)", 9.0)
        assertCalc("sin(3) - sin(2+1)", 0.0)
    }

    @Test
    fun `more complex calculations`() {
        assertCalc("2 + -3! * 3 + cos(1)", -15.45969769413186, 1e-5)
        assertCalc("2 +   (2 + 3!) - 4 * 3 *2 /5 + cos(1) + 4.12 * 2 - cos(1.1) * 2 * sin(sin(sin(1)))",
                13.364835439492671, 1e-5)
        assertCalc("----2!!!", 2.0)
    }
}
