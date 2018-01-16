package ru.spbstu.kparsec.examples

import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.parsers.*

sealed class AstNode
data class Block(val nodes: List<AstNode>): AstNode() {
    constructor(vararg nodes: AstNode): this(nodes.asList())
}
data class If(val condition: AstNode, val truePath: AstNode, val falsePath: AstNode?): AstNode()
data class While(val condition: AstNode, val body: AstNode): AstNode()
data class Declaration(val name: String): AstNode()
data class Assignment(val lhv: AstNode, val rhv: AstNode): AstNode()

sealed class Expression: AstNode()
data class Variable(val name: String): Expression()
data class DoubleLiteral(val value: Double): Expression()
object TrueLiteral: Expression()
object FalseLiteral: Expression()
enum class BinaryOperator{ ADD, SUB, MUL, DIV }
data class BinaryExpression(val op: BinaryOperator, val lhv: Expression, val rhv: Expression): Expression()
enum class CompareOperator{ EQ, NE, GE, GT, LE, LT }
data class CompareExpression(val op: CompareOperator, val lhv: Expression, val rhv: Expression): Expression()

operator fun BinaryOperator.invoke(lhv: Expression, rhv: Expression) =
        BinaryExpression(this, lhv, rhv)
operator fun CompareOperator.invoke(lhv: Expression, rhv: Expression) =
        CompareExpression(this, lhv, rhv)

/**
 * Note: this class is not optimized for speed in any way
 */
object SimpleImperativeLanguageParser: StringsAsParsers, DelegateParser<Char, AstNode> {

    val comment =
            -constant("/*") +
            -(char { it != '*' } or (char('*') notFollowedBy char('/'))).many() +
            -constant("*/")

    override val ignored = Literals.lexeme(-comment.many())

    val dliteral= Literals.FLOAT.map(::DoubleLiteral)
    val bliteral = (-"true").map { TrueLiteral } or (-"false").map { FalseLiteral }
    val variable = Common.IDENTIFIER.map(::Variable)
    val parens = (-'(' + defer { expr } + -')')
    val atom= lexeme(bliteral or dliteral or variable or parens)

    val op: Parser<Char, Expression> = operatorTable(atom) {
        (-'+')(priority = 8){ a, b -> BinaryOperator.ADD(a, b) }
        (-'-')(priority = 8){ a, b -> BinaryOperator.SUB(a, b) }
        (-'*')(priority = 10){ a, b -> BinaryOperator.MUL(a, b) }
        (-'/')(priority = 10){ a, b -> BinaryOperator.DIV(a, b) }

        (-"==")(priority = 6){ a, b -> CompareOperator.EQ(a, b) }
        (-"!=")(priority = 6){ a, b -> CompareOperator.NE(a, b) }
        (-">=")(priority = 6){ a, b -> CompareOperator.GE(a, b) }
        (-">")(priority = 6){ a, b -> CompareOperator.GT(a, b) }
        (-"<=")(priority = 6){ a, b -> CompareOperator.LE(a, b) }
        (-"<")(priority = 6){ a, b -> CompareOperator.LT(a, b) }
    }

    val expr: Parser<Char, Expression> = op

    val declaration = (-"var" + variable).map { Declaration(it.name) }
    val assignment = (expr + -"=" + expr).map { (l, r) -> Assignment(l, r) }
    val block = (-"{" + (defer { statement } joinedBy -";") + -"}").map(::Block)
    val `if`     =
            zip(
                    -"if" + -"(" + expr + -")",
                    block or defer { statement },
                    (-"else" + (block or defer { statement })).orNot()
            ) { c, t, f -> If(c, t, f) }
    val `while`     =
            zip(
                    -"while" + -"(" + expr + -")",
                    block or defer { statement }
            ) { c, b -> While(c, b) }

    val statement: Parser<Char, AstNode> = block or `if` or `while` or declaration or assignment or expr

    val program = statement + eof()
    override val self = program
}
