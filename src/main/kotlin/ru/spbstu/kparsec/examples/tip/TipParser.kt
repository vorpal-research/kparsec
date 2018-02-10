package ru.spbstu.kparsec.examples.tip

import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.parsers.*

object TIPParser : StringsAsParsers, DelegateParser<Char, AstNode> {

    val constant = lexeme(Literals.CINTEGER).map { Constant(it.toInt()) }
    val id = lexeme(Common.IDENTIFIER)
    val variable = id.map { Variable(it) }
    val input = (-"input").map { Input }
    val nul = (-"null").map { Null }
    val alloc = (-"alloc").map { Alloc }

    val call =
            zip(
                    Common.IDENTIFIER,
                    -"(" + (defer { expr } joinedBy -",") + -")"
            ) { f, args -> Call(f, args) }
    val icall =
            zip(
                    -"(" + defer { expr } + -")",
                    -"(" + (defer { expr } joinedBy -",") + -")"
            ) { f, args -> InCall(f, args) }

    val dereference = (-"*" + defer { expr }).map{ Load(it) }
    val takePointer = (-"&" + id).map { TakePtr(it) }

    val atom = input or
            nul or
            alloc or
            constant or
            call or
            icall or
            dereference or
            takePointer or
            variable or (-"(" + defer { expr } + -")")

    val ops = operatorTable(atom) {
        (-"+")(priority = 3){ l, r -> Binary(l, BinaryOperator.PLUS, r) }
        (-"-")(priority = 3){ l, r -> Binary(l, BinaryOperator.MINUS, r) }
        (-"*")(priority = 5){ l, r -> Binary(l, BinaryOperator.MULT, r) }
        (-"/")(priority = 5){ l, r -> Binary(l, BinaryOperator.DIV, r) }
        (-">")(priority = 1){ l, r -> Binary(l, BinaryOperator.GT, r) }
        (-"==")(priority = 1){ l, r -> Binary(l, BinaryOperator.EQ, r) }
    }

    val expr: Parser<Char, Expr> = ops

    val assignment = zip(id + -"=", expr + -";"){ id, e -> Assignment(id, e) }
    val output = -"output" + expr.map(::Output) + -";"

    val ifHead = -"if" + -"(" + expr + -")"
    val ifTrue =  -"{" + defer { stmts } + -"}"
    val ifFalse = -"else" + -"{" + defer { stmts } + -"}"

    val ifp = zip(ifHead, must(ifTrue), maybe(ifFalse), ::If)

    val whileHead = -"while" + -"(" + expr + -")"
    val whileBody = -"{" + defer{ stmts } + -"}"
    val whilep = zip(whileHead, must(whileBody), ::While)

    val store = zip(-"*" + id + -"=", expr + -";"){ id, e -> Store(id, e) }

    val stmt: Parser<Char, Stmt> = ifp or whilep or output or store or assignment
    val stmts = stmt.many().map(::Block)

    val functionHead =
            id + -"(" zipTo (id joinedBy -",").orElse(emptyList()) + -")"
    val functionBody =
            -"{"+
            zip(
                maybe(-"var" + (id joinedBy -",") + -";"),
                stmts,
            -"return" + expr + -";"
            ) +
            -"}"


    val function: Parser<Char, Function> = zip(functionHead, functionBody) {
        (name, args), (locals, body, ret) ->
        Function(name, args, locals ?: emptyList(), body, ret)
    }
    val program = function.many().map { Program(it) } + eof()
    override val self: Parser<Char, Program> = program
}
