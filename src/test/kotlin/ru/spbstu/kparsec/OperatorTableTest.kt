package ru.spbstu.kparsec

import org.junit.Test
import ru.spbstu.kparsec.parsers.Assoc
import ru.spbstu.kparsec.parsers.Literals
import ru.spbstu.kparsec.parsers.StringsAsParsers
import ru.spbstu.kparsec.parsers.operatorTable
import kotlin.test.assertEquals

class OperatorTableTest: StringsAsParsers {
    @Test
    fun `sanity check`() {
        val parser = operatorTable(Literals.FLOAT) {
            (-'+')(priority = 2){ a, b -> a + b }
            (-'-')(priority = 2){ a, b -> a - b }
            (-"~-")(priority = 2, assoc = Assoc.RIGHT){ a, b -> b - a }
            (-'*')(priority = 4){ a, b -> a * b }
            (-'/')(priority = 4){ a, b -> a / b }
        }

        assertEquals(10.0, parser.parse("1 + 2.0   * 6 /   3   +   5 ~- 10 ~- 20").assertResult())
    }
}
