package ru.spbstu.kparsec

import org.junit.Test
import ru.spbstu.kparsec.parsers.*
import kotlin.test.assertEquals

class BasicTest {
    @Test
    fun `success sanity check`() {
        assertEquals(2, success<Char, Int>(2).parse("").assertResult())
    }

    @Test
    fun `failure sanity check`() {
        fail<Char>("Error!").parse("").assertFail()
    }

    @Test
    fun `constant sanity check`() {
        assertEquals("Moo", constant("Moo").parse("Moo").assertResult())
        constant("Moo").parse("Meoo").assertFail()
    }

    @Test
    fun `regex sanity check`() {
        val re = regex("""
            (?:[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+)*|"
            (?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@
            (?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[
            (?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}
            (?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:
            (?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])
""".trimIndent().replace("\n","").trim().toRegex())

        assertEquals("toropyzhko@aivt.ftk.spbstu.ru",
                re.parse("toropyzhko@aivt.ftk.spbstu.ru!!").assertResult())
        assertEquals("toropyzhko@aivt.ftk.spbstu.ru",
                re.parse("toropyzhko@aivt.ftk.spbstu.ru").assertResult())

        val re2 = regex("""
            (?:[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+)*|"
            (?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@
            (?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[
            (?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}
            (?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:
            (?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])
""".trimIndent().replace("\n","").trim())

        assertEquals("toropyzhko@aivt.ftk.spbstu.ru",
                re2.parse("toropyzhko@aivt.ftk.spbstu.ru!!").assertResult())
        assertEquals("toropyzhko@aivt.ftk.spbstu.ru",
                re2.parse("toropyzhko@aivt.ftk.spbstu.ru").assertResult())
    }

    @Test
    fun `char parsers sanity check`() {
        assertEquals('a', char('a').parse("aa").assertResult())
        char('b').parse("aaa").assertFail()

        assertEquals('A', char { it.isUpperCase() }.parse("AAA").assertResult())
        char { it.isUpperCase() }.parse("aaa").assertFail()

        assertEquals('1', anyChar().parse("123").assertResult())

        assertEquals('4', range('3'..'8').parse("4").assertResult())
        range('3'..'8').parse("9").assertFail()

        assertEquals('4', oneOf("01234").parse("4").assertResult())
        oneOf("01234").parse("9").assertFail()
    }

    @Test
    fun `token parsers sanity check`() {
        assertEquals(1, token(1).parse(listOf(1, 2, 3)).assertResult())
        token(1).parse(listOf(9, 8, 7)).assertFail()

        assertEquals(2, token<Int> { it % 2 == 0 }.parse(listOf(2, 3, 4)).assertResult())
        token<Int> { it % 2 == 0 }.parse(listOf(7, 8, 9)).assertFail()

        assertEquals(1, anyToken<Int>().parse(listOf(1, 2, 3)).assertResult())

        assertEquals(4, range(3..8).parse(listOf(4, 15, 23)).assertResult())
        range(3..8).parse(listOf(9)).assertFail()

        assertEquals(4, oneOf(listOf(0, 1, 2, 3, 4)).parse(listOf(4)).assertResult())
        oneOf(listOf(0, 1, 2, 3, 4)).parse(listOf(9)).assertFail()

        assertEquals(4, oneOf(0, 1, 2, 3, 4).parse(listOf(4)).assertResult())
        oneOf(0, 1, 2, 3, 4).parse(listOf(9)).assertFail()
    }
}
