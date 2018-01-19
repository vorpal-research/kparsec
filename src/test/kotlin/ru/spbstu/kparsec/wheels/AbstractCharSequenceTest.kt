package ru.spbstu.kparsec.wheels

import org.junit.Test
import kotlin.test.assertEquals

class AbstractCharSequenceTest {

    @Test
    fun `subSequence test`() {
        assertEquals("el", CharSubSequence("Hello", 1, 3).toString())
        assertEquals("el", StringBuilder().append(CharSubSequence("Hello", 1, 3)).toString())
    }

}
