package ru.spbstu.kparsec.parsers

import org.junit.Test
import ru.spbstu.kparsec.Failure
import ru.spbstu.kparsec.ParseResult
import ru.spbstu.kparsec.Success
import ru.spbstu.kparsec.parse
import kotlin.test.assertEquals
import kotlin.test.assertTrue



class CalculatorTest {
    @Test
    fun `calculator parses simple cases`() {
        assertEquals(0.0, CalculatorParser.whole.parse("0").assertResult())
        assertEquals(0.0, CalculatorParser.whole.parse("0.0").assertResult())
        assertEquals(0.0, CalculatorParser.whole.parse("0.000000").assertResult())
        assertEquals(2.0, CalculatorParser.whole.parse("2").assertResult())
        assertEquals(3.15, CalculatorParser.whole.parse("3.15").assertResult())
        assertEquals(-1e10, CalculatorParser.whole.parse("-1e10").assertResult())
        assertEquals(-1E10, CalculatorParser.whole.parse("-1E10").assertResult())
        assertEquals(0.0, CalculatorParser.whole.parse(".0").assertResult())
    }

    @Test
    fun `calculator fails simple cases`() {
        CalculatorParser.whole.parse("").assertFail()
        CalculatorParser.whole.parse("aaa").assertFail()
        CalculatorParser.whole.parse("1e20e20").assertFail()
    }

    @Test
    fun `calculator parses one level`() {
        assertEquals(4.0, CalculatorParser.whole.parse("2.0 + 2").assertResult())
        assertEquals(0.0, CalculatorParser.whole.parse("2.0 + -2").assertResult())
        assertEquals(2e10, CalculatorParser.whole.parse("2 * 1e10").assertResult())
    }

    @Test
    fun `calculator parses parens`() {
        assertEquals(8.0, CalculatorParser.whole.parse("1.0 + (3.0 + 4.0)").assertResult())
        assertEquals(7.0, CalculatorParser.whole.parse("1.0 * (3.0 + 4.0)").assertResult())
        assertEquals(13.0, CalculatorParser.whole.parse("1.0 + (3.0 * 4.0)").assertResult())
        assertEquals(8.0, CalculatorParser.whole.parse("(3.0 + 4.0) + 1.0").assertResult())
        assertEquals(7.0, CalculatorParser.whole.parse("(3.0 + 4.0) * 1.0").assertResult())
        assertEquals(13.0, CalculatorParser.whole.parse("(3.0 * 4.0) + 1.0").assertResult())
    }

    @Test
    fun `calculator parses precedence`() {
        assertEquals(7.0, CalculatorParser.whole.parse("1.0 * 3.0 + 4.0").assertResult())
        assertEquals(13.0, CalculatorParser.whole.parse("1.0 + 3.0 * 4.0").assertResult())
        assertEquals(14.0, CalculatorParser.whole.parse("1.0 + 3.0 * 4.0 + 1.0").assertResult())
    }

    @Test
    fun `calculator parses complex stuff`() {
        assertEquals(49.07,
                CalculatorParser.whole.parse("1.0 * (3 + (4e1*1)     + 3) + 3.03 + 0.04").assertResult()
        )
    }
}