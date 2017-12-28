package ru.spbstu.kparsec

import org.junit.Test
import ru.spbstu.kparsec.parsers.*
import ru.spbstu.kparsec.parsers.Literals.CINTEGER
import ru.spbstu.kparsec.parsers.Literals.SPACES
import ru.spbstu.kparsec.parsers.Literals.FLOAT
import ru.spbstu.kparsec.parsers.Literals.lexeme
import kotlin.test.assertEquals

class CombinatorTest {
    @Test
    fun `zip2 sanity check`() {
        assertEquals(2L to 'z', zip(CINTEGER, anyChar()).parse("2z").assertResult())
        assertEquals(42L,
                zip(CINTEGER, -SPACES + CINTEGER){ x, y -> x + y }
                        .parse("20 22")
                        .assertResult())

        zip(anyChar(), char('a')).parse("ab").assertFail()
        zip(char('b'), anyChar()).parse("ab").assertFail()

        val contra = zip(anyChar(), char('a')) or char { it.isLowerCase() }
        assertEquals('a', contra.parse("ab").assertResult())
    }

    @Test
    fun `zip3 sanity check`() {
        assertEquals(Triple(2L, 'z', 'x'),
                zip(CINTEGER, anyChar(), char { it.isLowerCase() })
                        .parse("2zx").assertResult())

        assertEquals(108L,
                zip(CINTEGER,
                        -SPACES + CINTEGER,
                        -SPACES + CINTEGER){ x, y, z -> x + y * z }
                        .parse("20 22 4")
                        .assertResult())

        zip(anyChar(), char('a'), anyChar()).parse("abc").assertFail()
        zip(char('b'), anyChar(), anyChar()).parse("ab").assertFail()
        zip(anyChar(), anyChar(), char('a')).parse("abc").assertFail()

        val contra = zip(anyChar(), anyChar(), char('a')) or zip(char { it.isLowerCase() }, anyChar(), success('z'))
        assertEquals(Triple('a', 'b', 'z'), contra.parse("abc").assertResult())
    }

    @Test
    fun `sequence sanity check`() {
        /* empty sequence is essentially success */
        assertEquals(listOf(), sequence<Char, Int>().parse("anything").assertResult())
        assertEquals(listOf(2L, 'a', 5.0), sequence(CINTEGER, anyChar(), FLOAT).parse("2a5").assertResult())

        sequence(anyChar(), char('a'), anyChar()).parse("abc").assertFail()
        sequence(char('b'), anyChar(), anyChar()).parse("ab").assertFail()
        sequence(anyChar(), anyChar(), char('a')).parse("abc").assertFail()

        val contra = sequence(anyChar(), anyChar(), char('a')) or
                sequence(char { it.isLowerCase() }, anyChar(), success('z'))
        assertEquals(listOf('a', 'b', 'z'), contra.parse("abc").assertResult())
    }

    @Test
    fun `sequence(iterable) sanity check`() {
        /* empty sequence is essentially success */
        assertEquals(listOf(), sequence<Char, Int>(listOf()).parse("anything").assertResult())
        assertEquals(listOf(2L, 'a', 5.0), sequence(listOf(CINTEGER, anyChar(), FLOAT)).parse("2a5").assertResult())

        sequence(listOf(anyChar(), char('a'), anyChar())).parse("abc").assertFail()
        sequence(listOf(char('b'), anyChar(), anyChar())).parse("ab").assertFail()
        sequence(listOf(anyChar(), anyChar(), char('a'))).parse("abc").assertFail()

        val contra = sequence(listOf(anyChar(), anyChar(), char('a'))) or
                sequence(listOf(char { it.isLowerCase() }, anyChar(), success('z')))
        assertEquals(listOf('a', 'b', 'z'), contra.parse("abc").assertResult())
    }

    @Test
    fun `oneOf sanity check`() {
        /* empty oneOf is essentially fail */
        oneOf<Char, Int>().parse("aaa").assertFail()

        assertEquals('a', oneOf(char('z'), char { it.isUpperCase() }, anyChar()).parse("a").assertResult())
        assertEquals('a', oneOf(char('a'), char { it.isUpperCase() }, fail("")).parse("a").assertResult())
        oneOf(char('z'), char { it.isUpperCase() }, char { it.isLowerCase() }).parse("2").assertFail()
    }

    @Test
    fun `oneOf(iterable) sanity check`() {
        /* empty oneOf is essentially fail */
        oneOfCollection<Char, Int>(listOf()).parse("aaa").assertFail()

        assertEquals('a',
                oneOfCollection(listOf(char('z'), char { it.isUpperCase() }, anyChar())).parse("a").assertResult())
        assertEquals('a',
                oneOfCollection(listOf(char('a'), char { it.isUpperCase() }, fail(""))).parse("a").assertResult())
        oneOfCollection(listOf(char('z'), char { it.isUpperCase() }, char { it.isLowerCase() })).parse("2").assertFail()
    }

    @Test
    fun `parser map() sanity check`() {
        val dig = char { it.isDigit() }.map { it - '0' }

        assertEquals(9, dig.parse("9").assertResult())
        dig.parse("a").assertFail()
    }

    @Test
    fun `parser filter() sanity check`() {
        val dig = char { it.isDigit() }.map { it - '0' }.filter { it > 4 }

        assertEquals(9, dig.parse("9").assertResult())
        dig.parse("3").assertFail()
        dig.parse("a").assertFail()

    }

    @Test
    fun `recursive parser sanity check`() {
        val p: Parser<Char, Char> = recursive { rep ->
            (-lexeme('[') + rep + -lexeme(']')) or lexeme('$')
        }

        assertEquals('$', p.parse("$").assertResult())
        assertEquals('$', p.parse("[[[$]]]").assertResult())
        p.parse("[[$]").assertFail()
    }

    @Test
    fun `lazy parser sanity check`() {
        val p = defer { constant("azaza") }
        assertEquals("azaza", p.parse("azaza").assertResult())
        p.parse("dazaza").assertFail()
    }

    @Test
    fun `orElse sanity check`() {
        assertEquals(
                2,
                char { it.isDigit() }.map { it - '0' }.orElse(2)
                        .parse("azaza").assertResult()
        )
        assertEquals(
                4,
                char { it.isDigit() }.map { it - '0' }.orElse(2)
                        .parse("4").assertResult()
        )
    }

    @Test
    fun `orNot sanity check`() {
        assertEquals(
                null,
                char { it.isDigit() }.map { it - '0' }.orNot()
                        .parse("azaza").assertResult()
        )
        assertEquals(
                4,
                char { it.isDigit() }.map { it - '0' }.orNot()
                        .parse("4").assertResult()
        )
    }

    @Test
    fun `many() sanity check`() {
        val digits = char { it.isDigit() }.many()

        assertEquals(listOf(), digits.parse("azzazaaa").assertResult())
        assertEquals(listOf('1'), digits.parse("1asas").assertResult())
        assertEquals(listOf('1'), digits.parse("1").assertResult())
        assertEquals(listOf('1', '2', '3'), digits.parse("123a").assertResult())
        assertEquals(listOf('1', '2', '3'), digits.parse("123").assertResult())
    }

    @Test
    fun `manyOne() sanity check`() {
        val digits = char { it.isDigit() }.manyOne()

        digits.parse("azzazaaa").assertFail()
        assertEquals(listOf('1'), digits.parse("1asas").assertResult())
        assertEquals(listOf('1'), digits.parse("1").assertResult())
        assertEquals(listOf('1', '2', '3'), digits.parse("123a").assertResult())
        assertEquals(listOf('1', '2', '3'), digits.parse("123").assertResult())
    }

    @Test
    fun `repeated() sanity check`() {
        val digits = char { it.isDigit() }.repeated(5)

        digits.parse("azzazaaa").assertFail()
        digits.parse("0").assertFail()
        digits.parse("012").assertFail()
        digits.parse("0123").assertFail()
        assertEquals(listOf('0', '1', '2', '3', '4'),
                digits.parse("01234").assertResult())
        assertEquals(listOf('0', '1', '2', '3', '4'),
                digits.parse("012345").assertResult())
    }

    @Test
    fun `repeated(range) sanity check`() {
        val digits = char { it.isDigit() }.repeated(3..5)

        digits.parse("azzazaaa").assertFail()
        digits.parse("0").assertFail()
        assertEquals(listOf('0', '1', '2'),
                digits.parse("012").assertResult())
        assertEquals(listOf('0', '1', '2', '3'),
                digits.parse("0123").assertResult())
        assertEquals(listOf('0', '1', '2', '3', '4'),
                digits.parse("01234").assertResult())
        assertEquals(listOf('0', '1', '2', '3', '4'),
                digits.parse("012345").assertResult())
    }

    @Test
    fun `chain() sanity check`() {
        val twoEqualSymbols = anyChar() chain { char(it) }

        twoEqualSymbols.parse("an").assertFail()
        assertEquals('a', twoEqualSymbols.parse("aa").assertResult())
        assertEquals('a', twoEqualSymbols.parse("aan").assertResult())

        val sized = CINTEGER chain { anyChar() * it.toInt() }

        sized.parse("6abcad").assertFail()
        assertEquals("abcdef", sized.parse("6abcdef").assertResult().joinToString(""))
        assertEquals("abcdef", sized.parse("6abcdefkj").assertResult().joinToString(""))
    }

}