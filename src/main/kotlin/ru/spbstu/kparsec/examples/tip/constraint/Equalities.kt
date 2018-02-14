package ru.spbstu.kparsec.examples.tip.constraint

import java.util.*

sealed class Node {
    abstract operator fun contains(v: Node): Boolean
}
data class Var(val id: String = dict[currentId].also { ++currentId }) : Node() {
    override fun contains(v: Node): Boolean = v == this

    override fun toString() = id
    companion object {
        val dict = ('a'..'z').map{ it.toString() } + (0..100).map { "t" + it }
        var currentId = 0
    }
}
data class Fun(val name: String, val arguments: List<Node>) : Node() {
    override fun contains(v: Node): Boolean = v == this || arguments.any { it.contains(v) }

    override fun toString() = "$name(${arguments.joinToString()})"
}
data class Equality(val lhv: Node, val rhv: Node) {
    override fun toString() = "$lhv = $rhv"
}

fun collectVariables(n: Node, set: MutableSet<Var>): Unit = when(n) {
    is Var -> set += n
    is Fun -> n.arguments.forEach { collectVariables(it, set) }
}

fun collectVariables(eq: Equality, set: MutableSet<Var>) {
    collectVariables(eq.lhv, set)
    collectVariables(eq.rhv, set)
}

class Solver(input: List<Equality>) {
    data class Exception(val eq: Equality): kotlin.Exception("Unsolvable equation: $eq")

    val que: Queue<Equality> = ArrayDeque(input)
    val subst: MutableMap<Var, Node> = mutableMapOf()

    val Equality.rrhv get() = subst[rhv] ?: rhv
    val Equality.rlhv get() = subst[lhv] ?: lhv

    val unsolvedVariables: MutableSet<Var> = mutableSetOf()

    init {
        input.forEach { collectVariables(it, unsolvedVariables) }
    }

    fun substitute(v: Node): Node = when {
        v is Var && v in subst -> substitute(subst[v]!!)
        else -> v
    }

    fun delete(eq: Equality): Boolean = eq.rlhv != eq.rrhv
    fun decompose(eq: Equality): Boolean {
        val lhv = eq.rlhv
        val rhv = eq.rrhv
        when {
            lhv is Fun && rhv is Fun -> {
                if(lhv.name == rhv.name && lhv.arguments.size == rhv.arguments.size) {
                    lhv.arguments.zip(rhv.arguments) { l, r ->
                        que.add(Equality(substitute(l), substitute(r)))
                    }
                    return false
                } else throw Exception(eq)
            }
            else -> return true
        }
    }
    fun swap(eq: Equality): Boolean {
        val lhv = eq.rlhv
        val rhv = eq.rrhv
        if(rhv is Var && lhv !is Var) {
            que.add(Equality(rhv, lhv))
            return false
        }
        return true
    }
    fun eliminate(eq: Equality): Boolean {
        val lhv = eq.rlhv
        val rhv = eq.rrhv
        if(lhv is Var) {
            if(lhv in rhv) throw Exception(eq)

            unsolvedVariables -= lhv
            subst[lhv] = rhv
            return false
        }
        return true
    }

    fun solve() {
        while(que.isNotEmpty()) {
            val v = que.poll()!!
            when {
                delete(v) && decompose(v) && swap(v) && eliminate(v) -> que.add(v)
            }
        }
    }

    fun expand(witness: Var, n: Node): Node = when(n) {
        witness -> witness
        in unsolvedVariables -> n
        is Var -> expand(witness, substitute(n))
        is Fun -> n.copy(arguments = n.arguments.map { expand(witness, it) })
    }

    fun result(): List<Equality> {
        return subst.entries.map { (k, v) ->
            Equality(k, expand(k, v))
        }
    }

}
