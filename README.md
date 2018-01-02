[ ![Download](https://api.bintray.com/packages/vorpal-research/kotlin-maven/kparsec/images/download.svg) ](https://bintray.com/vorpal-research/kotlin-maven/kparsec/_latestVersion)
[![Build Status](https://travis-ci.org/belyaev-mikhail/kparsec.svg?branch=master)](https://travis-ci.org/belyaev-mikhail/kparsec)

# kparsec
Parser combinator library written entirely in Kotlin. Inspired by [parsec](https://github.com/haskell/parsec), [jparsec](https://github.com/jparsec/jparsec) and [scala parser combinators](https://github.com/scala/scala-parser-combinators).

# Example
Why not a full-fledged JSON parser for an example?

```kotlin
object SimpleJSONParser: StringsAsParsers {
    val string = Literals.JSTRING
    val number = Literals.FLOAT
    val boolean = Literals.BOOLEAN
    val nully = (+"null").map { null }

    val arrayElements = defer { element } joinedBy -',' orElse emptyList()
    val array = -'[' + arrayElements + -']'

    val entry_ = string + -':' + defer { element }
    val entry = entry_.map { (a, b) -> a to b }

    val objectElements = entry joinedBy -',' orElse emptyList()
    val obj = -'{' + objectElements + -'}' 
    
    val element: Parser<Char, Any?> = nully or string or number or boolean or array or obj
    val whole = element + eof()
}
```

See this and other examples [here](https://github.com/belyaev-mikhail/kparsec/tree/master/src/test/kotlin/ru/spbstu/kparsec/parsers)
