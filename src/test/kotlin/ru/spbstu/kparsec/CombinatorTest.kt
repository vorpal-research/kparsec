package ru.spbstu.kparsec

import org.junit.Test
import ru.spbstu.kparsec.parsers.*
import ru.spbstu.kparsec.parsers.Literals.CINTEGER
import ru.spbstu.kparsec.parsers.Literals.SPACES
import ru.spbstu.kparsec.parsers.Literals.FLOAT
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
}