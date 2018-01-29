package ru.spbstu.kparsec.wheels

interface TokenSequence<out T> {
    val length: Int
    operator fun get(index: Int): T
    fun subSequence(startIndex: Int, endIndex: Int): TokenSequence<T>
}

data class TokenSubSequence<out T>(val seq: TokenSequence<T>, val from: Int, val to: Int): AbstractTokenSequence<T>() {
    override val length: Int
        get() = to - from

    override fun get(index: Int): T = when {
        index !in 0..length -> throw IndexOutOfBoundsException()
        else -> seq[from + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int) = when {
        endIndex !in 0..length -> throw IndexOutOfBoundsException()
        else -> TokenSubSequence(seq, from + startIndex, from + endIndex)
    }

    /* this is NOT redundant, as data classes override base classes' toString by default */
    override fun toString() = super.toString()
}

abstract class AbstractTokenSequence<out T>: TokenSequence<T> {
    override fun subSequence(startIndex: Int, endIndex: Int): TokenSequence<T> =
            TokenSubSequence(this, startIndex, endIndex)

    override fun toString(): String {
        val sb = StringBuilder()
        for(i in 0 until length) {
            sb.append(get(i))
        }
        return sb.toString()
    }
}

internal data class CharSequenceAsTokenSequence(val inner: CharSequence): AbstractTokenSequence<Char>() {
    override val length: Int get() = inner.length
    override fun get(index: Int): Char = inner.get(index)
    /* this is NOT redundant, as data classes override base classes' toString by default */
    override fun toString() = super.toString()
}

internal data class TokenSequenceAsCharSequence(val inner: TokenSequence<Char>): AbstractCharSequence() {
    override val length: Int get() = inner.length
    override fun get(index: Int): Char = inner.get(index)
    /* this is NOT redundant, as data classes override base classes' toString by default */
    override fun toString() = super.toString()
}

fun CharSequence.asTokenSequence(): TokenSequence<Char> = when(this) {
    is TokenSequenceAsCharSequence -> inner
    else -> CharSequenceAsTokenSequence(this)
}

fun TokenSequence<Char>.asCharSequence(): CharSequence = when(this) {
    is CharSequenceAsTokenSequence -> inner
    else -> TokenSequenceAsCharSequence(this)
}

data class ListAsTokenSequence<out T>(val inner: List<T>): AbstractTokenSequence<T>() {
    override val length: Int get() = inner.size
    override fun get(index: Int): T = inner.get(index)
    /* this is NOT redundant, as data classes override base classes' toString by default */
    override fun toString() = super.toString()
}

fun<T> List<T>.asTokenSequence(): TokenSequence<T> = ListAsTokenSequence(this)

data class SequenceAsTokenSequence<out T>(val inner: MemoizedSequence<T>,
                                          val emptyToken: T): AbstractTokenSequence<T>() {
    constructor(inner: Sequence<T>, emptyToken: T): this(when (inner) {
        is MemoizedSequence -> inner
        else -> MemoizedSequence(inner)
    }, emptyToken)

    override val length: Int get() = Int.MAX_VALUE

    override fun get(index: Int): T = try {
        inner[index]
    } catch (ignore: IndexOutOfBoundsException) {
        emptyToken
    }
    /* this is NOT redundant, as data classes override base classes' toString by default */
    override fun toString() = super.toString()
}

fun<T> Sequence<T>.toTokenSequence(fillToken: T): TokenSequence<T> = SequenceAsTokenSequence(this, fillToken)
fun Sequence<Char>.toCharSequence(fillChar: Char = '\uffff'): CharSequence =
        SequenceAsTokenSequence(this, fillChar).asCharSequence()
