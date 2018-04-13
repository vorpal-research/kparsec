package ru.spbstu.kparsec.parsers

import org.junit.Test
import ru.spbstu.kparsec.*
import ru.spbstu.kparsec.parsers.Literals.lexeme
import kotlin.test.assertEquals

class DslTest {

    private fun List<Double>.product() = foldRight(1.0){ a, b -> a * b }

    val parser = grammar<Char, Double> {
        var constant: Parser<Char, Double> by NonTerminal
        var atom: Parser<Char, Double> by NonTerminal
        var mult: Parser<Char, Double> by NonTerminal
        var sum: Parser<Char, Double> by NonTerminal
        var expr: Parser<Char, Double> by NonTerminal

        constant = Literals.FLOAT
        atom = constant or (-lexeme('(') + expr + -lexeme(')'))

        val mult_ = atom joinedBy -lexeme('*')
        mult = mult_.map { it.product() }

        val sum_ = mult + (-lexeme('+') + mult).many()
        sum = sum_.map { it.sum() }

        expr = sum

        expr + eof()
    }

    @Test
    fun `calculator parses simple cases`() {
        assertEquals(0.0, parser.parse("0").assertResult())
        assertEquals(0.0, parser.parse("0.0").assertResult())
        assertEquals(0.0, parser.parse("0.000000").assertResult())
        assertEquals(2.0, parser.parse("2").assertResult())
        assertEquals(3.15, parser.parse("3.15").assertResult())
        assertEquals(-1e10, parser.parse("-1e10").assertResult())
        assertEquals(-1E10, parser.parse("-1E10").assertResult())
        assertEquals(0.0, parser.parse(".0").assertResult())
    }

    @Test
    fun `calculator fails simple cases`() {
        parser.parse("").assertFail()
        parser.parse("aaa").assertFail()
        parser.parse("1e20e20").assertFail()
    }

    @Test
    fun `calculator parses one level`() {
        assertEquals(4.0, parser.parse("2.0 + 2").assertResult())
        assertEquals(0.0, parser.parse("2.0 + -2").assertResult())
        assertEquals(2e10, parser.parse("2 * 1e10").assertResult())
    }

    @Test
    fun `calculator parses parens`() {
        assertEquals(8.0, parser.parse("1.0 + (3.0 + 4.0)").assertResult())
        assertEquals(7.0, parser.parse("1.0 * (3.0 + 4.0)").assertResult())
        assertEquals(13.0, parser.parse("1.0 + (3.0 * 4.0)").assertResult())
        assertEquals(8.0, parser.parse("(3.0 + 4.0) + 1.0").assertResult())
        assertEquals(7.0, parser.parse("(3.0 + 4.0) * 1.0").assertResult())
        assertEquals(13.0, parser.parse("(3.0 * 4.0) + 1.0").assertResult())
    }

    @Test
    fun `calculator parses precedence`() {
        assertEquals(7.0, parser.parse("1.0 * 3.0 + 4.0").assertResult())
        assertEquals(13.0, parser.parse("1.0 + 3.0 * 4.0").assertResult())
        assertEquals(14.0, parser.parse("1.0 + 3.0 * 4.0 + 1.0").assertResult())
    }

    @Test
    fun `calculator parses complex stuff`() {
        assertEquals(49.07,
                parser.parse("1.0 * (3 + (4e1*1)     + 3) + 3.03 + 0.04").assertResult()
        )
    }

    val packratParser = { packratGrammar<Char, Double> {
        var constant: Parser<Char, Double> by NonTerminal
        var atom: Parser<Char, Double> by NonTerminal
        var mult: Parser<Char, Double> by NonTerminal
        var sum: Parser<Char, Double> by NonTerminal
        var expr: Parser<Char, Double> by NonTerminal

        constant = Literals.FLOAT
        atom = constant or (-lexeme('(') + expr + -lexeme(')'))

        val mult_ = atom joinedBy -lexeme('*')
        mult = mult_.map { it.product() }

        val sum_ = mult + (-lexeme('+') + mult).many()
        sum = sum_.map { it.sum() }

        expr = sum

        expr + eof()
    } }

    @Test
    fun `packrat calculator parses simple cases`() {
        assertEquals(0.0, packratParser().parse("0").assertResult())
        assertEquals(0.0, packratParser().parse("0.0").assertResult())
        assertEquals(0.0, packratParser().parse("0.000000").assertResult())
        assertEquals(2.0, packratParser().parse("2").assertResult())
        assertEquals(3.15, packratParser().parse("3.15").assertResult())
        assertEquals(-1e10, packratParser().parse("-1e10").assertResult())
        assertEquals(-1E10, packratParser().parse("-1E10").assertResult())
        assertEquals(0.0, packratParser().parse(".0").assertResult())
    }

    @Test
    fun `packrat calculator fails simple cases`() {
        packratParser().parse("").assertFail()
        packratParser().parse("aaa").assertFail()
        packratParser().parse("1e20e20").assertFail()
    }

    @Test
    fun `packrat calculator parses one level`() {
        assertEquals(4.0, packratParser().parse("2.0 + 2").assertResult())
        assertEquals(0.0, packratParser().parse("2.0 + -2").assertResult())
        assertEquals(2e10, packratParser().parse("2 * 1e10").assertResult())
    }

    @Test
    fun `packrat calculator parses parens`() {
        assertEquals(8.0, packratParser().parse("1.0 + (3.0 + 4.0)").assertResult())
        assertEquals(7.0, packratParser().parse("1.0 * (3.0 + 4.0)").assertResult())
        assertEquals(13.0, packratParser().parse("1.0 + (3.0 * 4.0)").assertResult())
        assertEquals(8.0, packratParser().parse("(3.0 + 4.0) + 1.0").assertResult())
        assertEquals(7.0, packratParser().parse("(3.0 + 4.0) * 1.0").assertResult())
        assertEquals(13.0, packratParser().parse("(3.0 * 4.0) + 1.0").assertResult())
    }

    @Test
    fun `packrat calculator parses precedence`() {
        assertEquals(7.0, packratParser().parse("1.0 * 3.0 + 4.0").assertResult())
        assertEquals(13.0, packratParser().parse("1.0 + 3.0 * 4.0").assertResult())
        assertEquals(14.0, packratParser().parse("1.0 + 3.0 * 4.0 + 1.0").assertResult())
    }

    @Test
    fun `packrat calculator parses complex stuff`() {
        assertEquals(49.07,
                packratParser().parse("1.0 * (3 + (4e1*1)     + 3) + 3.03 + 0.04").assertResult()
        )
    }

    @Test
    fun `packrat parser is actually packrat`() {
        val parser = packratParser()

        assertEquals(2.0, parser.parse("2").assertResult())
        // packrat does not reparse the same location twice
        assertEquals(2.0, parser.parse("whatever").assertResult())
    }

    @Test
    fun `packrat left recursion`() {
        val parser: Parser<Char, Double> = packratGrammar {
            var expr: Parser<Char, Double> by NonTerminal
            expr = expr + -lexeme(':')
            expr
        }

        assertEquals(Error("Left recursion detected", CharLocation("<string>", 1, 0)),
                parser.parse("2:::"))
    }
}