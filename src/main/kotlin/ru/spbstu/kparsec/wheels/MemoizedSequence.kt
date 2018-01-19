package ru.spbstu.kparsec.wheels

interface IterableSequence<out T>: Sequence<T>, Iterable<T>

data class MemoizedSequence<T>(val input: Iterator<T>, val storage: MutableList<T>): IterableSequence<T> {

    private val finished get() = !input.hasNext()
    private fun step() = input.next().also { storage.add(it) }
    private fun forceEverything() { while(!finished) step() }

    private inner class TheIterator: Iterator<T> {
        var position = 0

        val inside get() = position < storage.size

        private fun checkInvariant() {
            if(position > storage.size) throw IllegalStateException("memoized sequence storage concurrent modification")
        }

        override fun hasNext(): Boolean = inside || input.hasNext()

        override fun next(): T = when {
            inside -> storage[position]
            else -> step()
        }.also { ++position }.also { checkInvariant() }
    }

    override fun iterator(): Iterator<T> = TheIterator()
}

fun<T> Sequence<T>.memoized(): IterableSequence<T> = MemoizedSequence(this.iterator(), mutableListOf())
fun<T> Sequence<T>.memoizedTo(storage: MutableList<T>): IterableSequence<T> = MemoizedSequence(this.iterator(), storage)
