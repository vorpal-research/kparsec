package ru.spbstu.kparsec.examples

import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.Success
import ru.spbstu.kparsec.parse
import ru.spbstu.kparsec.parsers.*
import java.io.File
import kotlin.reflect.KProperty

data class Token<R>(val type: String, val result: R)

fun<T,R> Parser<T, R>.manyOneToString() = manyOne().map { it.joinToString("") }
fun<T,R> Parser<T, R>.manyToString() = many().map { it.joinToString("") }

infix fun<T> Parser<T, String>.concat(that: Parser<T, String>) = zip(this, that) { x, y -> x + y  }

class Autowired<T, R>(body: () -> Parser<T, R>) {
    companion object {
        private object NOT_INITIALIZED
    }

    private var initializer: (() -> Parser<T, R>)? = body
    private var lazyValue: Any? = NOT_INITIALIZED

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Parser<T, Token<R>> = synchronized(this){
        return when(lazyValue) {
            null -> {
                (initializer!!().map { Token(property.name, it) } named property.name).also { lazyValue = it }
            }
            else -> lazyValue as Parser<T, Token<R>>
        }
    }
}

fun <T, R> autowire(body: () -> Parser<T, R>) = Autowired(body)

object KotlinLexer : StringsAsParsers, DelegateParser<Char, List<String>> {

    override val ignored: Parser<Char, Unit> = success(Unit)

    const val CR = '\r'
    const val LF = '\n'
    const val WS = ' '
    const val TAB = '\t'
    const val FF = '\u000A'
    const val ASTERISK = '*'
    const val SLASH = '/'
    const val BACKTICK = '`'
    const val BACKLASH = '\\'
    const val SINGLE_QUOTE = '\''
    const val DOUBLE_QUOTE = '"'
    const val DOLLAR = '$'
    const val LBRACE = '{'
    const val RBRACE = '}'

    val shebang by autowire {
        -"#!" + inputCharacters
    }
    val inputCharacters by lazy {
        inputCharacter.manyOneToString()
    }
    val inputCharacter by lazy {
        char { it !in "$CR$LF" }
    }
    val newLineCharacter by lazy {
        oneOf("$CR$LF")
    }
    val newLine by lazy {
        +"$LF" or (+"$CR" concat (+"$LF").orElse(""))
    }
    val inputElement by lazy {
        whitespace or comment or token
    }
    val whitespace by lazy {
        newLine or oneOf("$WS$TAB$FF").map { "$it" }
    }
    val comment by lazy {
        eolComment or delimitedComment
    }
    val eolComment by lazy {
        +"//" concat inputCharacters.orElse("")
    }
    val delimitedComment: Parser<Char, String> by lazy {
        +"/*" concat delimitedCommentParts.orElse("") concat asterisks concat +"/"
    }
    val delimitedCommentParts by lazy {
        delimitedCommentPart.manyToString()
    }
    val delimitedCommentPart by lazy {
        defer { delimitedComment } or
                notAsterisk.map { it.toString() } or
                (asterisks + notSlashOrAsterisk).map { it.joinToString("") }
    }
    val asterisks by lazy {
        char(ASTERISK).manyOneToString()
    }
    val notAsterisk by lazy {
        char { it != ASTERISK }
    }
    val notSlashOrAsterisk by lazy {
        char { it !in "$SLASH$ASTERISK" }
    }
    val token by lazy {
        oneOf(
                keyword,
                identifier,
                floatLiteral,
                integerLiteral,
                charLiteral,
                stringLiteral,
                operator,
                whitespace,
                comment
        )
    }
    val identifier by lazy {
        regularIdentifier or escapedIdentifier
    }
    val fieldIdentifier by lazy {
        -"$" + identifier
    }
    val regularIdentifier by lazy {
        identifierStart concat identifierParts.orElse("")
    }
    val identifierStart by lazy { letter.map { "$it" } }
    val letter by lazy { char { Character.isAlphabetic(it.toInt()) } }
    val identifierParts by lazy { identifierPart.manyOneToString() }
    val identifierPart by lazy { identifierStart or digit  }
    val digit by lazy { char { it.isDigit() } }

    val escapedIdentifier by lazy {
        -backtick + escapeIdentifierCharacters + -backtick
    }
    val backtick = char(BACKTICK)
    val escapeIdentifierCharacters by lazy {
        escapeIdentifierCharacter.manyOneToString()
    }
    val escapeIdentifierCharacter by lazy {
        inputCharacter.filter { it != BACKTICK }
    }
    val keywordList = listOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
            "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "val", "var",
            "when", "while"
    )
    val keyword by lazy {
        oneOfCollection(keywordList.map { constant(it) } + char('_').manyOneToString())
    }
    val decimalDigit by lazy { range('0'..'9') }
    val integerLiteral by lazy {
        (decimalDigits or hexLiteral) concat integerLiteralSuffix.orElse("")
    }
    val integerLiteralSuffix by lazy { +"L" }
    val decimalDigits by lazy { decimalDigit.manyOneToString() }
    val floatLiteral: Parser<Char, String> by lazy {
        floatLiteralBase concat floatLiteralExponent.orElse("") concat floatLiteralSuffix.orElse("")
    }
    val floatLiteralBase by lazy {
        (decimalDigits concat (+"." concat decimalDigits).orElse("")) or
                (+"." concat decimalDigits)
    }
    val floatLiteralExponent by lazy {
        oneOf("eE").map { "$it" } concat
                oneOf("+-").map { "$it" } concat
                decimalDigits
    }
    val floatLiteralSuffix by lazy { oneOf("fF").map { "$it" } }
    val hexDigit by lazy {
        decimalDigit or oneOf("abcdefABCDEF")
    }
    val hexDigits by lazy { hexDigit.manyOneToString() }
    val hexLiteral by lazy {
        -"0" + -oneOf("xX") + hexDigits
    }
    val operatorList = listOf(
            "+", "-", "*", "/", "%", "=", "+=", "-=", "*=", "/=", "%=", "++", "--",
            "&&", "||", "!", "==", "!=", "===", "!==", "<", ">", "<=", ">=", "[", "]",
            "!!", ".", "?.", "?:", "::", "..", ":", "?", "->", "@", ";", "(", ")", ",", "{", "}"
    )
    val operator: Parser<Char, String> by lazy {
        oneOfCollection(operatorList.sorted().reversed().map { constant(it) })
    }

    val escapes = listOf('n', 'r', 't', 'b', SINGLE_QUOTE, DOUBLE_QUOTE, DOLLAR, BACKLASH)
    val charLiteral: Parser<Char, String> by lazy {
        char(SINGLE_QUOTE).map { "${it}" } concat
                (literalChar.filter { it != "$SINGLE_QUOTE" } or constant("$DOLLAR")) concat
                char(SINGLE_QUOTE).map { "${it}" }
    }
    val literalChar: Parser<Char, String> by lazy {
        char { it !in "$CR$LF$BACKLASH$TAB\$" }.map { "$it" } or
                simpleCharEscape or
                unicodeEscape
    }
    val simpleCharEscape by lazy {
        char(BACKLASH).map { "${it}" } concat oneOfCollection(escapes.map { constant("$it") })
    }
    val unicodeEscape by lazy {
        char(BACKLASH).map { "${it}" } concat constant("u") concat (hexDigit * 4).map { it.joinToString("") }
    }
    val stringLiteral: Parser<Char, String> by lazy {
        inlineStringLiteral or rawStringLiteral
    }
    val inlineStringLiteral by lazy {
        char(DOUBLE_QUOTE).map { "${it}" } concat inlineStringLiteralPart.manyToString() concat char(DOUBLE_QUOTE).map { "${it}" }
    }
    val inlineStringLiteralPart by lazy {
        literalChar.filter { it != "$DOUBLE_QUOTE" } or interpolation or constant("$DOLLAR")
    }
    val interpolation by lazy {
        constant("$DOLLAR") concat interpolatedCode
    }
    val interpolatedCode by lazy {
        identifier or (constant("$LBRACE") concat defer{ token }.manyToString() concat constant("$RBRACE"))
    }
    val rawStringLiteral by lazy {
        (char(DOUBLE_QUOTE) * 3).map { it.joinToString("") } concat
                rawStringLiteralPart.manyToString() concat
                (char(DOUBLE_QUOTE) * 3).map { it.joinToString("") }
    }
    val rawLiteralChar by lazy { char { it != '$' } }
    val rawStringLiteralPart by lazy {
        rawLiteralChar.manyToString() or interpolation or constant("$DOLLAR")
    }

    val nullLiteral = +"null"
    val booleanLiteral = +"true" or +"false"

    override val self: Parser<Char, List<String>> = token.many()

}

object KotlinParser: StringsAsParsers, DelegateParser<Char, String> {
    val lex = KotlinLexer
    val semi = -";" or lex.newLine

    val expressionOrBlock by lazy {
        -expression or -assignment or -block
    }
    val expression by lazy { -disjunction }
    val assignment by lazy {
        -assignableExpression + -assignmentOperator + expression
    }
    val assignmentOperator =
        -"=" or -"+=" or -"-=" or -"*=" or -"/=" or -"%="
    val disjunction by lazy {
        conjunction joinedBy -"||"
    }
    val conjunction by lazy {
        equality joinedBy -"&&"
    }
    val equality by lazy {
        comparison joinedBy equalityOperator
    }
    val equalityOperator=
        -"==" or -"!=" or -"===" or -"!=="
    val comparison by lazy {
        infixOperation joinedBy comparisonOperator
    }
    val comparisonOperator=
        -">" or -"<" or -">=" or -"<="
    val infixOperation by lazy {
        (elvisExpression + maybe(isOperator + type)) or
                (elvisExpression joinedBy inOperator)
    }
    val isOperator = -"is" or -"!is"
    val inOperator = -"in" or -"!in"
    val elvisExpression by lazy {
        infixFunctionCall joinedBy -"?:"
    }
    val infixFunctionCall by lazy {
        rangeExpression joinedBy -lex.identifier
    }
    val rangeExpression by lazy {
        additiveExpression joinedBy -".."
    }
    val additiveExpression by lazy {
        multiplicativeExpression joinedBy additiveOperator
    }
    val additiveOperator = -"+" or -"-"
    val multiplicativeExpression by lazy {
        asExpression joinedBy multiplicativeOperator
    }
    val multiplicativeOperator = -"*" or -"/" or -"%"
    val asExpression by lazy {
        prefixUnaryExpression + (asOperator + type).many()
    }
    val asOperator = -"as?" or -"as"
    val prefixUnaryExpression: Parser<Char, Any> by lazy {
        oneOf(
                postfixUnaryExpression,
                prefixUnaryOperator + defer{ prefixUnaryExpression },
                annotation + prefixUnaryExpression,
                label + prefixUnaryExpression
        )
    }
    val prefixUnaryOperator = -"++" or -"--" or -"+" or -"-" or -"!"
    val annotation: Parser<Char, String> = TODO()
    val label: Parser<Char, String> = TODO()
    val postfixUnaryExpression: Parser<Char, Unit> by lazy {
        (-assignableExpression or -invocationExpression) + -postfixUnaryOperator.many()
    }
    val postfixUnaryOperator = -"++" or -"--" or -"!!"
    val assignableExpression by lazy {
        primaryExpression or
                indexingExpression or
                memberAccess
    }
    val primaryExpression by lazy {
        parenthesizedExpression or
                literal or
                functionLiteral or
                thisExpression or
                superExpression or
                objectLiteral or
                lex.identifier or
                fieldName or
                callableReference or
                packageExpression or
                jumpExpression or
                conditionalExpression or
                loopExpression or
                tryExpression
    }
    val callableReference by lazy {
        -"::" + -lex.identifier
    }
    val parenthesizedExpression: Parser<Char, Unit> by lazy {
        -"(" + defer{ expression } + -")"
    }
    val literal by lazy {
         -lex.booleanLiteral or
                 -lex.integerLiteral or
                 -lex.floatLiteral or
                 -lex.charLiteral or
                 -lex.stringLiteral or
                 -lex.nullLiteral
    }
    val thisExpression by lazy {
        +"this" + maybe(labelReference)
    }
    val superExpression by lazy {
        +"super" + maybe(superTypeReference) + maybe(labelReference)
    }
    val superTypeReference by lazy {
        -"<" + type + -">"
    }
    val labelReference by lazy {
        -constant("@") + -lex.identifier
    }
    val invocationExpression: Parser<Char, Unit> by lazy {
        -oneOf(
                -postfixUnaryExpression + maybe(typeArguments) + maybe(argumentList) + trailingLambda,
                postfixUnaryExpression + maybe(typeArguments) + argumentList,
                postfixUnaryExpression + typeArguments
        )
    }

    val block: Parser<Char, Unit> = TODO()
    val type: Parser<Char, Unit> = TODO()
    val indexingExpression: Parser<Char, Unit> = TODO()
    val memberAccess: Parser<Char, Unit> = TODO()
    val functionLiteral: Parser<Char, Unit> = TODO()
    val objectLiteral: Parser<Char, Unit> = TODO()
    val fieldName: Parser<Char, Unit> = TODO()
    val packageExpression: Parser<Char, Unit> = TODO()
    val jumpExpression: Parser<Char, Unit> = TODO()
    val conditionalExpression: Parser<Char, Unit> = TODO()
    val loopExpression: Parser<Char, Unit> = TODO()
    val tryExpression: Parser<Char, Unit> = TODO()
    val typeArguments: Parser<Char, Unit> = TODO()
    val argumentList: Parser<Char, Unit> = TODO()
    val trailingLambda: Parser<Char, Unit> = TODO()

    override val self: Parser<Char, String> = TODO()
}

fun main(args: Array<String>) {
    val whaa = 4.1
    val res = KotlinLexer.parse(File("src/main/kotlin/ru/spbstu/kparsec/examples/MetaParser.kt").readText())
    when(res) {
        is Success -> println(res.result.joinToString("\n") { "\"$it\"" })
    }
}
