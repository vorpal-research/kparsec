package ru.spbstu.kparsec.examples.tip.constraint

class UnionFind<T> {
    inner class Node(private var value: T, private var parent: Node? = null, private var rank: Int = 0) {
        fun swap(that: Node) {
            val tmp = that.value
            that.value = value
            value = tmp
        }

        val root: Node
            get() = parent?.root?.also { parent = it } ?: this

        infix fun unite(that: Node) = run {
            val lhv = this.root
            val rhv = that.root
            when {
                lhv.rank < rhv.rank -> {
                    lhv.parent = rhv
                    rhv
                }
                rhv.rank > lhv.rank -> {
                    rhv.parent = lhv
                    lhv
                }
                else -> {
                    rhv.parent = lhv
                    lhv.rank++
                    lhv
                }
            }
        }

        override fun equals(other: Any?): Boolean = when(other) {
            !is UnionFind<*>.Node -> false
            other.root === root -> true
            else -> false
        }

        override fun hashCode(): Int = root.value?.hashCode() ?: 0

        override fun toString(): String {
            return "Node(value=$value, rank=$rank)"
        }
    }

    fun single(value: T) = Node(value)
    fun unite(lhv: Node, rhv: Node) = lhv unite rhv
}


