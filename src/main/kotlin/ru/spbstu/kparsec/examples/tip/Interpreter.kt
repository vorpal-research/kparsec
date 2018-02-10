package ru.spbstu.kparsec.examples.tip

data class Stack<E>(val storage: MutableList<E> = mutableListOf()) {
    fun push(value: E) = storage.add(value)
    fun top() = storage.last()
    fun pop(): E = storage.removeAt(storage.lastIndex)
}

abstract class Interpreter {

    abstract fun input(): Int
    abstract fun output(value: Int)

    var allocaPointer: Int = 0
    val memory: MutableMap<Int, Int> = mutableMapOf()
    val naming: MutableMap<String, Int> = mutableMapOf()

    fun load(ptr: Int) = memory[ptr] ?: throw IllegalStateException("Invalid address: 0x${ptr.toString(16)}")
    fun valueOf(vname: String) = load(naming[vname] ?: throw IllegalStateException("No variable named $vname"))

    fun interpret(expr: Expr): Int = when(expr) {
        is Input -> input()
        is Null -> 0
        is Alloc -> {
            ++allocaPointer
            allocaPointer
        }
        is Constant -> expr.value
        is Variable -> valueOf(expr.name)
        is Load -> load(interpret(expr.ptr))
        is Binary -> {
            val lhv = interpret(expr.lhv)
            val rhv = interpret(expr.rhv)
            when(expr.op) {
                BinaryOperator.PLUS -> lhv + rhv
                BinaryOperator.MINUS -> lhv - rhv
                BinaryOperator.MULT -> lhv * rhv
                BinaryOperator.DIV -> lhv / rhv
                BinaryOperator.GT -> if(lhv > rhv) 1 else 0
                BinaryOperator.EQ -> if(lhv == rhv) 1 else 0
            }
        }
        is TakePtr -> naming[expr.variable] ?: throw IllegalStateException("No variable named ${expr.variable}")
        is Call -> TODO()
        is InCall -> TODO()
    }

    fun interpret(stmt: Stmt): Unit = TODO()

}