package ru.spbstu.kparsec.examples.tip

import java.util.concurrent.atomic.AtomicInteger

interface PrettyPrintable {
    fun pprint(): String
}

sealed class AstNode : PrettyPrintable
sealed class Expr : AstNode()
sealed class Stmt : AstNode()

data class Constant(val value: Int): Expr() {
    override fun pprint() = "$value"
}
data class Variable(val name: String): Expr() {
    override fun pprint() = name
}
enum class BinaryOperator(val rep: String): PrettyPrintable {
    PLUS("+"), MINUS("-"), MULT("*"), DIV("/"), GT(">"), EQ("==");

    override fun pprint(): String = rep
}
data class Binary(val lhv: Expr, val op: BinaryOperator, val rhv: Expr): Expr() {
    override fun pprint() = "(${lhv.pprint()} ${op.pprint()} ${rhv.pprint()})"
}
object Input : Expr() {
    override fun pprint() = "input"
    override fun toString() = "Input"
}
data class Load(val ptr: Expr): Expr() {
    override fun pprint() = "*(${ptr.pprint()})"
}
object Alloc : Expr() {
    override fun pprint() = "alloc"
    override fun toString() = "Null"
}
data class TakePtr(val variable: String): Expr() {
    override fun pprint() = "&($variable)"
}
object Null : Expr() {
    override fun pprint() = "null"
    override fun toString() = "Null"
}

data class Call(val fname: String, val args: List<Expr>): Expr() {
    override fun pprint() = "$fname(${args.joinToString(", ")})"
}
data class InCall(val function: Expr, val args: List<Expr>): Expr() {
    override fun pprint() = "(${function.pprint()})(${args.joinToString(", ")})"
}

data class Block(val statements: List<Stmt>): AstNode() {
    override fun pprint() = "{\n" + statements.joinToString("\n") { it.pprint() }.prependIndent("   ") + "}"
}

data class Assignment(val name: String, val value: Expr): Stmt() {
    override fun pprint() = "$name = ${value.pprint()};"
}
data class Output(val expr: Expr): Stmt() {
    override fun pprint() = "output ${expr.pprint()};"
    override fun toString() = "Output"
}
data class If(val condition: Expr, val trueBranch: Block, val falseBranch: Block? = null): Stmt() {
    override fun pprint() = "if (${condition.pprint()}) ${trueBranch.pprint()} ${falseBranch?.pprint().orEmpty()}"
}
data class While(val condition: Expr, val body: Block): Stmt() {
    override fun pprint() = "while (${condition.pprint()}) ${body.pprint()}"
}
data class Store(val name: String, val value: Expr): Stmt() {
    override fun pprint() = "*$name = ${value.pprint()}"
}

data class Function(val name: String,
                    val args: List<String>,
                    val vars: List<String>,
                    val body: Block,
                    val ret: Expr): AstNode() {
    override fun pprint(): String {
        val sb = StringBuilder()
        sb.appendln("$name (${args.joinToString(", ")}) {")
        run {
            val sbody = StringBuilder()
            if(vars.isNotEmpty()) sbody.appendln(vars.joinToString(prefix = "var ", postfix = ";", separator = ","))
            body.statements.forEach {
                sbody.appendln(it.pprint())
            }
            sbody.append("return ${ret.pprint()};")
            sb.append("$sbody".prependIndent("    "))
            sb.appendln()
        }
        sb.appendln("}")
        return "$sb"
    }
}

data class Program(val functions: List<Function>): AstNode() {
    override fun pprint() = functions.joinToString("\n") { it.pprint() }
}
