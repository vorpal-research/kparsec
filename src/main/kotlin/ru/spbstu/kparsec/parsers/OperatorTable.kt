package ru.spbstu.kparsec.parsers

import ru.spbstu.kparsec.Input
import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.map

const val DEFAULT_PRIORITY = 7
interface Assoc {
    val index: Int
    companion object {
        val LEFT = BinaryAssoc.LEFT
        val RIGHT = BinaryAssoc.RIGHT
        val NONE = BinaryAssoc.NONE
        val PREFIX = UnaryAssoc.PREFIX
        val POSTFIX = UnaryAssoc.POSTFIX
    }
}

enum class UnaryAssoc(override val index: Int): Assoc { PREFIX(4), POSTFIX(3) }
enum class BinaryAssoc(override val index: Int): Assoc { LEFT(2), RIGHT(1), NONE(0) }

private typealias Mapping<Base, Op> = (Base, Op, Base) -> Base
private data class SortedKey(val priority: Int, val assoc: Assoc): Comparable<SortedKey> {
    override fun compareTo(other: SortedKey): Int {
        val priComp = priority.compareTo(other.priority)
        if(priComp != 0) return priComp
        return assoc.index.compareTo(other.assoc.index)
    }
}

private data class Entry<T, E, K>(
        val op: Parser<T, K>,
        val mapping: Mapping<E, K>
): Parser<T, (E, E) -> E> {
    override fun invoke(input: Input<T>) =
            op(input).map { op -> { a: E, b: E -> mapping(a, op, b) }}
}

class OperatorTableContext<T, Base>(val base: Parser<T, Base>) {
    private val map = mutableMapOf<SortedKey, MutableList<Entry<T, Base, *>>>()

    private fun Parser<T, (Base, Base) -> Base>.apply(element: Parser<T, Base>) =
            zip(element, this, element) { l, f, r -> f(l, r) }

    operator fun<K> Parser<T, K>.invoke(priority: Int = DEFAULT_PRIORITY,
                               assoc: Assoc = Assoc.LEFT,
                               mapping: (Base, K, Base) -> Base) {
        map.getOrPut(SortedKey(priority, assoc)){ mutableListOf() } += Entry( this, mapping)
    }

    operator fun<K> Parser<T, K>.invoke(priority: Int = DEFAULT_PRIORITY,
                               assoc: Assoc = Assoc.LEFT,
                               mapping: (Base, Base) -> Base) {
        map.getOrPut(SortedKey(priority, assoc)){ mutableListOf() } += Entry( this){ a, _, b -> mapping(a, b) }
    }

    internal fun build(): Parser<T, Base> {
        var currentElement = base

        val sortedKeys = map.keys.sortedByDescending{ it }
        for(key in sortedKeys) {
            val op = oneOf(map[key] as List<Parser<T, (Base, Base) -> Base>>)
            currentElement = when(key.assoc) {
                Assoc.LEFT -> zip(currentElement, zip(op, currentElement).many()){ first, rest ->
                    rest.fold(first){ l, (op, r) -> op(l, r) }
                }
                Assoc.RIGHT -> zip(zip(currentElement, op).many(), currentElement){ rest, last ->
                    rest.foldRight(last){ (l, op), r -> op(l, r) }
                }
                Assoc.NONE -> zip(currentElement, op, currentElement) { l, f, r -> f(l, r) }

                Assoc.PREFIX -> zip(op, currentElement) { f, r -> f(r, r) }
                Assoc.POSTFIX -> zip(currentElement, op) { r, f -> f(r, r) }
                else -> /* =( */ error("Unknown associativity type: ${key.assoc}")
            }
        }
        return currentElement
    }
}

fun<T, Base> operatorTable(element: Parser<T, Base>, body: OperatorTableContext<T, Base>.() -> Unit): Parser<T, Base> {
    val ctx = OperatorTableContext(element)
    ctx.body()
    return ctx.build()
}
