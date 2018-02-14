package ru.spbstu.kparsec.examples.tip

data class Stack<E>(val storage: MutableList<E> = mutableListOf()) {
    fun push(value: E) = storage.add(value)
    fun top() = storage.last()
    fun pop(): E = storage.removeAt(storage.lastIndex)
}

data class StackFrame(val callee: Function, val locals: MutableMap<String, Int> = mutableMapOf()): Map<String, Int> by locals

abstract class Interpreter {

    abstract fun input(): Int
    abstract fun output(value: Int)

    var allocaPointer: Int = 1

    val memory: MutableMap<Int, Int> = mutableMapOf()
    val stack: Stack<StackFrame> = Stack()

    val globals: MutableMap<String, Int> = mutableMapOf()
    val fpointers: MutableMap<Int, Function> = mutableMapOf()

    val nextPointer: Int get() = allocaPointer.also { ++allocaPointer }
    val currentFrame: StackFrame get() = stack.top()

    inner class StackFrame(val callee: Function, val locals: MutableMap<String, Int>): Map<String, Int> by locals {
        constructor(callee: Function, arguments: List<Int>): this(callee, mutableMapOf()) {
            assert(callee.args.size == arguments.size)
            callee.args.forEachIndexed { ix, value ->
                val ptr = nextPointer
                locals[value] = ptr
                memory[ptr] = arguments[ix]
            }
            callee.vars.forEach { value ->
                locals[value] = nextPointer
            }
        }
    }

    fun addressOf(vname: String) =
            currentFrame[vname]
    fun valueOf(vname: String) =
            addressOf(vname)?.let(this::load) ?:
                    globals[vname] ?:
                    throw IllegalStateException("No variable named $vname")
    fun load(ptr: Int) =
            memory[ptr] ?: throw IllegalStateException("Invalid address: 0x${ptr.toString(16)}")

    fun interpret(function: Function, args: List<Int>): Int {
        stack.push(StackFrame(function, args))
        interpret(function.body)
        val result = interpret(function.ret)
        stack.pop()
        return result
    }

    fun interpret(expr: Expr): Int = when(expr) {
        is Input -> input()
        is Null -> 0
        is Alloc -> nextPointer
        is Constant -> expr.value
        is Variable -> valueOf(expr.name)
        is Load -> load(interpret(expr.ptr))
        is Binary -> {
            val lhv = interpret(expr.lhv)
            val rhv = interpret(expr.rhv)
            when (expr.op) {
                BinaryOperator.PLUS -> lhv + rhv
                BinaryOperator.MINUS -> lhv - rhv
                BinaryOperator.MULT -> lhv * rhv
                BinaryOperator.DIV -> lhv / rhv
                BinaryOperator.GT -> if (lhv > rhv) 1 else 0
                BinaryOperator.EQ -> if (lhv == rhv) 1 else 0
            }
        }
        is TakePtr -> addressOf(expr.variable) ?: throw IllegalStateException("No variable named ${expr.variable}")
        is Call -> interpret(
                fpointers[globals[expr.fname] ?: throw IllegalStateException("No such function: ${expr.fname}")]!!,
                expr.args.map(this::interpret)
        )
        is InCall -> interpret(
                fpointers[interpret(expr.function)] ?: throw IllegalStateException("No such function: (${expr.function.pprint()})"),
                expr.args.map(this::interpret)
        )
    }

    fun interpret(block: Block) { block.statements.forEach(this::interpret) }
    fun interpret(stmt: Stmt): Unit = when(stmt) {
        is Assignment -> {
            val ptr = currentFrame[stmt.name] ?: throw IllegalStateException("No variable named ${stmt.name}")
            memory[ptr] = interpret(stmt.value)
        }
        is Output -> output(interpret(stmt.expr))
        is If -> {
            val cond = interpret(stmt.condition)
            when {
                cond != 0 -> interpret(stmt.trueBranch)
                stmt.falseBranch != null -> interpret(stmt.falseBranch)
                else -> {}
            }
        }
        is While -> {
            while(true) {
                val cond = interpret(stmt.condition)
                if(cond == 1) break
                interpret(stmt.body)
            }
        }
        is Store -> {
            val variable = currentFrame[stmt.name] ?: throw IllegalStateException("No variable named ${stmt.name}")
            val ptr = memory[variable] ?: throw IllegalStateException("Illegal address: $variable")
            memory[ptr] = interpret(stmt.value)
        }
    }

    fun interpret(function: Function) {
        val fpointer = nextPointer
        fpointers[fpointer] = function
        globals[function.name] = fpointer
    }

    fun interpret(program: Program): Int {
        program.functions.forEach { interpret(it) }
        val mainFun = program.functions.find { it.name == "main" }
        mainFun ?: throw IllegalArgumentException("Program does not contain main function")
        return interpret(mainFun, listOf())
    }
}

object ConsoleInterpreter: Interpreter() {
    override fun input() = System.`in`.bufferedReader().readLine().toInt()
    override fun output(value: Int){ System.out.println(value) }
}

data class ListInterpreter(val inputs: List<Int>, val outputs: MutableList<Int> = mutableListOf()): Interpreter() {
    var inputIndex = 0
    override fun input(): Int = inputs[inputIndex].also { ++inputIndex }
    override fun output(value: Int) { outputs.add(value) }
}
