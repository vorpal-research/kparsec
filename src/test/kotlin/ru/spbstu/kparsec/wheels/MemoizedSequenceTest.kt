package ru.spbstu.kparsec.wheels

import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class MemoizedSequenceTest {
    @Test
    fun `calculating fib 100`() {
        /* you cannot recurse a value into itself locally without an object =( */
        val fibs= object  {
            val interm: Sequence<BigInteger> by lazy {
                (sequenceOf(BigInteger.ONE, BigInteger.ONE) +
                        Sequence{ interm.zip(interm.drop(1)){ a, b -> a + b }.iterator() }).memoized()
            }
        }.interm

        // naively calculatin' 100 fibonacci numbers without memoization takes forever
        assertEquals("354224848179261915075", fibs.drop(50).take(50).last().toString())
        // and it works twice!
        assertEquals("354224848179261915075", fibs.drop(50).take(50).last().toString())
    }

    @Test
    fun `calculating fib 100 with external storage`() {
        val storage: MutableList<BigInteger> = mutableListOf()
        /* you cannot recurse a value into itself locally without an object =( */
        val fibs= object  {
            val interm: Sequence<BigInteger> by lazy {
                (sequenceOf(BigInteger.ONE, BigInteger.ONE) +
                        Sequence{ interm.zip(interm.drop(1)){ a, b -> a + b }.iterator() }).memoizedTo(storage)
            }
        }.interm

        // naively calculatin' 100 fibonacci numbers without memoization takes forever
        assertEquals("354224848179261915075", fibs.drop(50).take(50).last().toString())
        assertEquals(100, storage.size)
        assertEquals("354224848179261915075", storage[99].toString())

        // and it works twice!
        assertEquals("354224848179261915075", fibs.drop(50).take(50).last().toString())
        assertEquals(100, storage.size)
        assertEquals("354224848179261915075", storage[99].toString())
    }
}
