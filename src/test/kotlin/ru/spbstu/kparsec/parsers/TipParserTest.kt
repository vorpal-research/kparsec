package ru.spbstu.kparsec.parsers

import org.intellij.lang.annotations.Language
import org.junit.Test
import ru.spbstu.kparsec.assertResult
import ru.spbstu.kparsec.examples.tip.*
import ru.spbstu.kparsec.examples.tip.Function
import ru.spbstu.kparsec.examples.tip.constraint.Equality
import ru.spbstu.kparsec.examples.tip.constraint.Node
import ru.spbstu.kparsec.examples.tip.constraint.Solver
import ru.spbstu.kparsec.examples.tip.constraint.Var
import ru.spbstu.kparsec.parse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TipParserTest {
    @Test
    fun `test parsing`() {
        run {
            val code = """
                main() {
                    var x,y,z;
                    x = input;
                    y = alloc;
                    *x = y;
                    z = *y;
                    return x;
                }

            """.trimIndent()

            assertEquals(
                    Program(listOf(
                            Function(
                                    "main",
                                    listOf(),
                                    listOf("x", "y", "z"),
                                    Block(listOf(
                                            Assignment("x", Input),
                                            Assignment("y", Alloc),
                                            Store("x", Variable("y")),
                                            Assignment("z", Load(Variable("y")))
                                    )),
                                    Variable("x")
                            )
                    )),
                    TIPParser.parse(code).assertResult()
            )


        }
    }

    @Test
    fun `testInterpreter`() {
        @Language("C")
        val prog = """
                swap(x,y) {
                    var tmp;
                    tmp = *x;
                    *x = *y;
                    *y = tmp;
                    return x;
                }

                main() {
                    var x,y,z;
                    x = alloc;
                    *x = input;
                    y = 56;
                    z = swap(&y, x);
                    output (y + 5);
                    return y;
                }
            """.trimIndent()

        val program = TIPParser.parse(prog).assertResult()
        assertEquals(listOf(47), ListInterpreter(listOf(42)).apply { interpret(program) }.outputs)

    }

    @Test
    fun `testSolver`() {
        val tmp = Var("[[swap.tmp]]")
        val x = Var("[[swap.x]]")
        val y = Var("[[swap.y]]")
        val Int = ru.spbstu.kparsec.examples.tip.constraint.Fun("Int", listOf())
        fun Ptr(x: Node) = ru.spbstu.kparsec.examples.tip.constraint.Fun("Ptr", listOf(x))
        fun Fun(res: Node, vararg args: Node) = ru.spbstu.kparsec.examples.tip.constraint.Fun("Fun", listOf(res) + args)
        infix fun Node.eq(that: Node) = Equality(this, that)

        val solver = Solver(listOf(
                Var("[[swap.x]]") eq Ptr(Var("[[swap.tmp]]")),
                Var("[[swap.x]]") eq Ptr(Var("[[*swap.y]]")),
                Var("[[swap.y]]") eq Ptr(Var("[[*swap.y]]")),
                Var("[[swap.y]]") eq Ptr(Var("[[swap.tmp]]")),
                Var("[[swap]]") eq Fun(x, x, y),
                Var("[[main.x]]") eq Ptr(Var("\\alpha")),
                Var("[[main.x]]") eq Ptr(Int),
                Var("[[main.y]]") eq Int,
                Var("[[&main.y]]") eq Ptr(Var("[[main.y]]")),
                Var("[[swap]]") eq Fun(Var("[[swap(&y,x)]]"), Var("[[main.&y]]"), Var("[[main.x]]")),
                Var("[[main.z]]") eq Var("[[swap(&y,x)]]"),
                Var("[[main.y+5]]") eq Int,
                Var("[[main.y]]") eq Int

        ))
        solver.solve()
        assertTrue(
                solver.result().toSet().containsAll(setOf(
                        Var("[[swap.x]]") eq Ptr(Int),
                        Var("[[swap.y]]") eq Ptr(Int),
                        Var("[[swap.tmp]]") eq Int,
                        Var("[[swap]]") eq Fun(Ptr(Int), Ptr(Int), Ptr(Int)),
                        Var("[[main.x]]") eq Ptr(Int),
                        Var("[[main.y]]") eq Int,
                        Var("[[main.z]]") eq Ptr(Int)
                ))
        )
    }
}
