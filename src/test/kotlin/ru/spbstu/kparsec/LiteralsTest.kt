package ru.spbstu.kparsec

import org.junit.Test
import ru.spbstu.kparsec.parsers.Literals
import kotlin.test.assertEquals

class LiteralsTest {
    @Test
    fun `spaces should work`() {
        assertEquals("", Literals.SPACES.parse("").assertResult())
        assertEquals(" ", Literals.SPACES.parse(" ").assertResult())
        assertEquals("  ", Literals.SPACES.parse("  ").assertResult())
        assertEquals("\t\t\t", Literals.SPACES.parse("\t\t\t").assertResult())
        assertEquals("\t\n\t  ", Literals.SPACES.parse("\t\n\t  ").assertResult())
    }

    @Test
    fun `float literals should work`() {
        assertEquals(3.14, Literals.FLOAT.parse("3.14").assertResult())
        assertEquals(-3.14, Literals.FLOAT.parse("-3.14").assertResult())
        assertEquals(0.0, Literals.FLOAT.parse("0").assertResult())
        assertEquals(0.0, Literals.FLOAT.parse("0.").assertResult())
        assertEquals(-0.0, Literals.FLOAT.parse("-0.").assertResult())
        assertEquals(9.87654321, Literals.FLOAT.parse("9.87654321").assertResult())
        assertEquals(1e28, Literals.FLOAT.parse("1e28").assertResult())
        assertEquals(1e28, Literals.FLOAT.parse("1E+28").assertResult())
        assertEquals(1e-28, Literals.FLOAT.parse("1e-28").assertResult())
        assertEquals(-3.14e-28, Literals.FLOAT.parse("-3.14e-28").assertResult())
    }

    @Test
    fun `decimal literals should work`() {
        assertEquals(0L, Literals.DECIMAL.parse("0").assertResult())
        assertEquals(50L, Literals.DECIMAL.parse("50").assertResult())
        assertEquals(500000000L, Literals.DECIMAL.parse("500000000").assertResult())
        Literals.DECIMAL.parse("").assertFail()
    }

    @Test
    fun `octal literals should work`() {
        assertEquals(0L, Literals.OCTAL.parse("0").assertResult())
        assertEquals(40L, Literals.OCTAL.parse("50").assertResult())
        assertEquals(83886080L, Literals.OCTAL.parse("500000000").assertResult())
        Literals.OCTAL.parse("").assertFail()
        Literals.OCTAL.parse("900").assertFail()
    }

    @Test
    fun `hex literals should work`() {
        assertEquals(0L, Literals.HEXADECIMAL.parse("0").assertResult())
        assertEquals(80L, Literals.HEXADECIMAL.parse("50").assertResult())
        assertEquals(21474836480L, Literals.HEXADECIMAL.parse("500000000").assertResult())
        assertEquals(0xcafebeef, Literals.HEXADECIMAL.parse("cafebeef").assertResult())
        assertEquals(0xcafebeef, Literals.HEXADECIMAL.parse("CaFeBeEf").assertResult())
        assertEquals(0xFEE1, Literals.HEXADECIMAL.parse("FEE1").assertResult())
        Literals.HEXADECIMAL.parse("").assertFail()
    }

    @Test
    fun `boolean literals should work`() {
        assertEquals(true, Literals.BOOLEAN.parse("true").assertResult())
        assertEquals(false, Literals.BOOLEAN.parse("false").assertResult())
        Literals.BOOLEAN.parse("").assertFail()
        Literals.BOOLEAN.parse("tru").assertFail()
    }

    @Test
    fun `c integer literals should work`() {
        assertEquals(0L, Literals.CINTEGER.parse("0").assertResult())
        assertEquals(40L, Literals.CINTEGER.parse("050").assertResult())
        assertEquals(50L, Literals.CINTEGER.parse("50").assertResult())
        assertEquals(80L, Literals.CINTEGER.parse("0x50").assertResult())
        assertEquals(80L, Literals.CINTEGER.parse("0x50").assertResult())
        Literals.CINTEGER.parse("").assertFail()
        assertEquals(0L, Literals.CINTEGER.parse("090").assertResult())
    }

    @Test
    fun `string literals should work`() {
        Literals.CSTRING.parse("abcdef").assertFail()
        Literals.CSTRING.parse("\"abcdef").assertFail()
        Literals.CSTRING.parse("abcdef\"").assertFail()
        assertEquals("", Literals.CSTRING.parse("\"\"").assertResult())
        assertEquals("abcdef", Literals.CSTRING.parse("\"abcdef\"").assertResult())
        assertEquals("abc", Literals.CSTRING.parse("\"abc\"def\"").assertResult())
        assertEquals("\u00ff", Literals.CSTRING.parse("\"\\u00ff\"").assertResult())
        assertEquals("\u00ff", Literals.CSTRING.parse("\"\\xff\"").assertResult())
        Literals.CSTRING.parse("\"\\u0ff\"").assertFail()
        assertEquals("\n\t\t", Literals.CSTRING.parse("\"\\n\\t\\t\"").assertResult())
        assertEquals(0.toChar().toString(), Literals.CSTRING.parse("\"\\0\"").assertResult())
        assertEquals(40.toChar().toString(), Literals.CSTRING.parse("\"\\50\"").assertResult())
        assertEquals(40.toChar().toString(), Literals.CSTRING.parse("\"\\050\"").assertResult())
        assertEquals(40.toChar().toString(), Literals.CSTRING.parse("\"\\0500\"").assertResult())
    }
}
