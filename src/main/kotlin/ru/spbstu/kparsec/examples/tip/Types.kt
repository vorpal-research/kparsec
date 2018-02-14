package ru.spbstu.kparsec.examples.tip

import java.util.*

sealed class Type
object IntType : Type() {
    override fun toString() = "Int"
}

data class PointerType(val pointee: Type) : Type()
data class FunctionType(val result: Type, val arguments: List<Type>) : Type()
data class TypeVariable(val id: Int) : Type()

data class Constraint(val lhv: Type, val rhv: Type)

class SimpleTyper : Visitor<List<Constraint>> {
    var currentTyVarId: Int = 0
    fun freshVar(): TypeVariable = TypeVariable(currentTyVarId).also { ++currentTyVarId }

    val mapping: MutableMap<Pair<Int, Expr>, Type> = mutableMapOf()

    override fun default(): List<Constraint> = listOf()
    override fun combine(values: Collection<List<Constraint>>): List<Constraint> = values.flatten()

    fun entry(expr: Expr) = when (expr) {
        is Constant, Input, Alloc, Null -> System.identityHashCode(expr) to expr
        else -> 0 to expr
    }

    override fun visitExpr(expr: Expr): List<Constraint> = run {
        val fresh = mapping.getOrPut(entry(expr)){ freshVar() }
        when (expr) {
            is Constant, Input -> listOf(Constraint(fresh, IntType))
            Alloc, Null -> listOf(Constraint(fresh, PointerType(freshVar())))
            is Variable -> default()
            is Binary -> {
                val sub = super.visitExpr(expr)
                val lhv = mapping[entry(expr.lhv)] ?: throw IllegalStateException("No mapping for child in ${expr.pprint()}")
                val rhv = mapping[entry(expr.rhv)] ?: throw IllegalStateException("No mapping for child in ${expr.pprint()}")
                when (expr.op) {
                    BinaryOperator.EQ ->
                        combine(
                                sub,
                                listOf(
                                        Constraint(fresh, IntType),
                                        Constraint(lhv, rhv)
                                )
                        )
                    else ->
                        combine(
                                sub,
                                listOf(
                                        Constraint(lhv, IntType),
                                        Constraint(rhv, IntType),
                                        Constraint(fresh, IntType)
                                )
                        )
                }
            }
            is Load -> {
                val sub = super.visitExpr(expr)
                val rhv = mapping[entry(expr.ptr)] ?: throw IllegalStateException("No mapping for child in ${expr.pprint()}")
                combine(
                        sub,
                        listOf(Constraint(rhv, PointerType(fresh)))
                )
            }
            is TakePtr -> {
                val fakeVar = Variable(expr.variable)
                val fake = visitExpr(fakeVar)
                val sub = super.visitExpr(expr)
                val rhv = mapping[entry(fakeVar)] ?: throw IllegalStateException("No mapping for child in ${expr.pprint()}")
                combine(
                        fake,
                        sub,
                        listOf(Constraint(fresh, PointerType(rhv)))
                )
            }
            is Call -> {
                val fakeVar = Variable(expr.fname)
                val fake = visitExpr(fakeVar)
                val sub = super.visitExpr(expr)
                val f = mapping[entry(fakeVar)] ?: throw IllegalStateException("No mapping for child in ${expr.pprint()}")
                val args = expr.args.map { mapping[entry(it)] }.filterNotNull()
                combine(
                        fake,
                        sub,
                        listOf(Constraint(f, FunctionType(fresh, args)))
                )
            }
            is InCall ->  {
                val sub = super.visitExpr(expr)
                val f = mapping[entry(expr.function)] ?: throw IllegalStateException("No mapping for child in ${expr.pprint()}")
                val args = expr.args.map { mapping[entry(it)] }.filterNotNull()
                combine(
                        sub,
                        listOf(Constraint(f, FunctionType(fresh, args)))
                )
            }
        }

    }

}
