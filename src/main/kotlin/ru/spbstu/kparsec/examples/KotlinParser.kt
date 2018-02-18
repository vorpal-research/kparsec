package ru.spbstu.kparsec.examples

import kotlinx.Warnings
import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.Success
import ru.spbstu.kparsec.parse
import ru.spbstu.kparsec.parsers.*
import java.io.File
import kotlin.reflect.KProperty

data class Token<R> (val type: String, val result: R)

fun <T, R> Parser<T, R>.manyOneToString() = manyOne().map { it.joinToString("") }
fun <T, R> Parser<T, R>.manyToString() = many().map { it.joinToString("") }

infix fun <T> Parser<T, String>.concat(that: Parser<T, String>) = zip(this, that) { x, y -> x + y }
@JvmName("concatChar")
infix fun <T> Parser<T, Char>.concat(that: Parser<T, String>) = zip(this, that) { x, y -> x + y }
@JvmName("concatChar2")
infix fun <T> Parser<T, String>.concat(that: Parser<T, Char>) = zip(this, that) { x, y -> x + y }

class LazyNamed<T, R>(body: () -> Parser<T, R>) {
    companion object {
        val UNINITIALIZED = Any()
    }
    var storage: Any? = UNINITIALIZED
    var generator: (() -> Parser<T, R>)? = body

    @Suppress(Warnings.UNCHECKED_CAST)
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = when(storage) {
        UNINITIALIZED -> {
            storage = generator!!.invoke() named property.name
            generator = null
            storage as Parser<T, R>
        }
        else -> storage as Parser<T, R>
    }
}

fun<T, R> lazyNamed(body: () -> Parser<T, R>) = LazyNamed(body)

object KotlinLexer : StringsAsParsers, DelegateParser<Char, List<String>> {

    override val skippedBefore: Parser<Char, Unit> = empty()

    const val CR = '\r'
    const val LF = '\n'
    const val WS = ' '
    const val TAB = '\t'
    const val FF = '\u000C'
    const val ASTERISK = '*'
    const val SLASH = '/'
    const val BACKTICK = '`'
    const val BACKLASH = '\\'
    const val SINGLE_QUOTE = '\''
    const val DOUBLE_QUOTE = '"'
    const val DOLLAR = '$'
    const val LBRACE = '{'
    const val RBRACE = '}'

    val shebang by lazyNamed {
        -"#!" + inputCharacter.manyToString() + newLine
    }
    val inputCharacter by lazyNamed {
        char { it !in "$CR$LF" }
    }
    val newLine by lazyNamed {
        +"$LF" or (+"$CR" concat (+"$LF").orElse(""))
    }
    val newLines by lazyNamed { newLine.manyToString() }
    val inlineSpace by lazyNamed {
        oneOf("$WS$TAB$FF").map { "$it" }
    }
    val whitespace by lazyNamed {
        newLine or inlineSpace
    }
    val comment by lazyNamed {
        eolComment or delimitedComment
    }
    val eolComment by lazyNamed {
        +"//" concat inputCharacter.manyToString()
    }
    val delimitedComment: Parser<Char, String> by lazyNamed {
        +"/*" concat delimitedCommentParts.orElse("") concat asterisks concat +"/"
    }
    val delimitedCommentParts by lazyNamed {
        delimitedCommentPart.manyToString()
    }
    val delimitedCommentPart by lazyNamed {
        defer { delimitedComment } or
                notAsterisk.map { it.toString() } or
                (asterisks + notSlashOrAsterisk).map { it.joinToString("") }
    }
    val asterisks by lazyNamed {
        char(ASTERISK).manyOneToString()
    }
    val notAsterisk by lazyNamed {
        char { it != ASTERISK }
    }
    val notSlashOrAsterisk by lazyNamed {
        char { it !in "$SLASH$ASTERISK" }
    }
    val identifier by lazyNamed {
        regularIdentifier or escapedIdentifier
    }
    val regularIdentifier by lazyNamed {
        identifierStart concat identifierParts.orElse("")
    }
    val identifierStart by lazyNamed { letter }
    val letter by lazyNamed { char { Character.isAlphabetic(it.toInt()) } }
    val identifierParts by lazyNamed { identifierPart.manyOneToString() }
    val identifierPart by lazyNamed { identifierStart or digit or +'_' }
    val digit by lazyNamed { char { it.isDigit() } }
    val escapedIdentifier by lazyNamed {
        -backtick + escapeIdentifierCharacters + -backtick
    }
    val backtick = char(BACKTICK)
    val escapeIdentifierCharacters by lazyNamed {
        escapeIdentifierCharacter.manyOneToString()
    }
    val escapeIdentifierCharacter by lazyNamed {
        inputCharacter.filter { it != BACKTICK }
    }
    val keywordList = listOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
            "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "val", "var",
            "when", "while"
    )
    val keyword by lazyNamed {
        oneOfCollection(keywordList.map { constant(it) } + char('_').manyOneToString())
    }
    val integerLiteral by lazyNamed {
        (decimalLiteral or hexLiteral or binaryLiteral or +"0") concat integerLiteralSuffix.orElse("")
    }
    val integerSeparator by lazyNamed { +"_" }
    val integerLiteralSuffix by lazyNamed { +"L" }
    val decimalDigit by lazyNamed { range('0'..'9').map { "$it" } }
    val positiveDigit by lazyNamed { range('1'..'9').map { "$it" } }
    val decimalLiteral by lazyNamed {
        positiveDigit concat decimalDigits.orElse("")
    }
    val decimalDigits by lazyNamed { decimalDigit concat (decimalDigit or integerSeparator).manyToString() }
    val hexDigit by lazyNamed {
        decimalDigit or oneOf("abcdefABCDEF").map { "$it" }
    }
    val hexDigits by lazyNamed { hexDigit concat (hexDigit or integerSeparator).manyToString() }
    val hexLiteral by lazyNamed {
        +"0" concat oneOf("xX").map { "$it" } concat hexDigits
    }
    val binaryDigit by lazyNamed { oneOf("01").map { "$it" } }
    val binaryDigits by lazyNamed { binaryDigit concat (binaryDigit or integerSeparator).manyToString() }
    val binaryLiteral by lazyNamed {
        +"0" concat oneOf("bB").map { "$it" } concat binaryDigits
    }
    val floatLiteral: Parser<Char, String> by lazyNamed {
        floatLiteralBase concat floatLiteralExponent.orElse("") concat floatLiteralSuffix.orElse("")
    }
    val floatLiteralBase by lazyNamed {
        (decimalDigits concat (+"." concat decimalDigits).orElse("")) or
                (+"." concat decimalDigits)
    }
    val floatLiteralExponent by lazyNamed {
        oneOf("eE").map { "$it" } concat
                oneOf("+-").map { "$it" } concat
                decimalDigits
    }
    val floatLiteralSuffix by lazyNamed { oneOf("fF").map { "$it" } }
    val operatorList = listOf(
            "+", "-", "*", "/", "%", "=", "+=", "-=", "*=", "/=", "%=", "++", "--",
            "&&", "||", "!", "==", "!=", "===", "!==", "<", ">", "<=", ">=", "[", "]",
            "!!", ".", "?.", "?:", "::", "..", ":", "?", "->", "@", ";", "(", ")", ",", "{", "}"
    )
    val operator: Parser<Char, String> by lazyNamed {
        oneOfCollection(operatorList.sorted().reversed().map { constant(it) })
    }

    val escapes = listOf('n', 'r', 't', 'b', SINGLE_QUOTE, DOUBLE_QUOTE, DOLLAR, BACKLASH)
    val charLiteral: Parser<Char, String> by lazyNamed {
        char(SINGLE_QUOTE).map { "${it}" } concat
                (literalChar.filter { it != "$SINGLE_QUOTE" } or constant("$DOLLAR")) concat
                char(SINGLE_QUOTE).map { "${it}" }
    }
    val literalChar: Parser<Char, String> by lazyNamed {
        char { it !in "$CR$LF$BACKLASH" }.map { "$it" } or
                simpleCharEscape or
                unicodeEscape
    }
    val simpleCharEscape by lazyNamed {
        char(BACKLASH).map { "${it}" } concat oneOfCollection(escapes.map { constant("$it") })
    }
    val unicodeEscape by lazyNamed {
        char(BACKLASH).map { "${it}" } concat constant("u") concat (hexDigit * 4).map { it.joinToString("") }
    }
    val stringLiteral: Parser<Char, String> by lazyNamed {
        inlineStringLiteral or rawStringLiteral
    }
    val inlineStringLiteral by lazyNamed {
        char(DOUBLE_QUOTE).map { "${it}" } concat inlineStringLiteralPart.manyToString() concat char(DOUBLE_QUOTE).map { "${it}" }
    }
    val inlineStringLiteralPart by lazyNamed {
        literalChar.filter { it !in "$DOUBLE_QUOTE$DOLLAR" } or interpolation or constant("$DOLLAR")
    }
    val interpolation by lazyNamed {
        constant("$DOLLAR") concat interpolatedCode
    }
    val interpolatedCode by lazyNamed {
        identifier or (constant("$LBRACE") concat defer { token }.manyToString() concat constant("$RBRACE"))
    }
    val rawStringLiteral by lazyNamed {
        (char(DOUBLE_QUOTE) * 3).map { it.joinToString("") } concat
                rawStringLiteralPart.manyToString() concat
                (char(DOUBLE_QUOTE) * 3).map { it.joinToString("") }
    }
    val rawLiteralChar by lazyNamed { char { it != DOLLAR } }
    val rawStringLiteralPart by lazyNamed {
        rawLiteralChar.manyToString() or interpolation or constant("$DOLLAR")
    }

    val nullLiteral = +"null"
    val booleanLiteral = +"true" or +"false"

    val token by lazyNamed {
        oneOf(
                keyword,
                identifier,
                floatLiteral,
                integerLiteral,
                charLiteral,
                stringLiteral,
                operator
        )
    }
    val inputElement by lazyNamed {
        whitespace or comment or token
    }

    val semi =  inlineSpace.manyToString() concat (+";" or newLine) concat whitespace.manyToString()
    override val self: Parser<Char, List<String>> = inputElement.many() + eof()
};

typealias NoResultParser = Parser<Char, Unit>

object KotlinFileParser : StringsAsParsers, DelegateParser<Char, Unit> {
    val lex = KotlinLexer
    val decls = KotlinDeclParser
    override val skippedBefore: Parser<Char, Any?> = (lex.inlineSpace or lex.comment).many() named "~"
    override val skippedAfter: Parser<Char, Any?> = empty()

    override val self: NoResultParser get() = kotlinFile

    val kotlinFile: NoResultParser by lazyNamed {
        preamble + decls
    }
    val script: NoResultParser by lazyNamed {
        preamble + -(decls.expression joinedBy -lex.semi.manyOneToString())
    }
    val preamble: NoResultParser by lazyNamed {
        -maybe(lex.shebang) + -maybe(fileAnnotations) + -maybe(packageHeader) + -import.many()
    }
    val fileAnnotations: NoResultParser by lazyNamed { -(fileAnnotation joinedBy -lex.newLines.manyToString()) }
    val fileAnnotation: NoResultParser by lazyNamed {
        -"@" + -"file" + -":" + ((-"[" + -decls.unescapedAnnotation.manyOne() + -"]") or decls.unescapedAnnotation)
    }
    val packageHeader: NoResultParser by lazyNamed {
        decls.modifiers + -"package" + -(lex.identifier joinedBy -".") + -lex.semi
    }
    val import: NoResultParser by lazyNamed {
        (-"import" + -(lex.identifier joinedBy -".") +
                -maybe((-"." + -"*") or (-"as" + lex.identifier)) +
                -lex.semi)
    }
}
object KotlinDeclParser : StringsAsParsers, DelegateParser<Char, Unit> {
    val lex = KotlinLexer
    override val skippedBefore: Parser<Char, Any?> = (lex.whitespace or lex.comment).many() named "~"
    override val skippedAfter: Parser<Char, Any?> = empty()

    val semi = -(lex.semi joinedBy lex.inlineSpace.manyOneToString())

    override val self: Parser<Char, Unit> get() = -topLevelObject.many()

    fun keyword(text: String) =
            (-skippedBefore + (-constant(text) notFollowedBy lex.identifierPart) + -skippedBefore) named "keyword($text)"
    val TYPEALIAS = keyword("typealias")
    val CLASS = keyword("class")
    val INTERFACE = keyword("interface")
    val OBJECT = keyword("object")
    val CONSTRUCTOR = keyword("constructor")
    val COMPANION = keyword("companion")
    val BY = keyword("by")
    val VAL = keyword("val")
    val VAR = keyword("var")
    val WHERE = keyword("where")
    val GET = keyword("get")
    val SET = keyword("set")
    val FUN = keyword("fun")
    val INIT = keyword("init")

    val topLevelObject by lazyNamed {
        klass or objekt or function or property or typeAlias or semi
    }
    val typeAlias by lazyNamed {
        modifiers + TYPEALIAS + -lex.identifier + -maybe(typeParameters) + -"=" + type + -lex.semi
    }
    val klass: Parser<Char, Unit> by lazyNamed {
        modifiers + (CLASS or INTERFACE) + -lex.identifier +
                -maybe(typeParameters) + -maybe(primaryConstructor) +
                -maybe(-":" + annotations + (delegationSpecifier joinedBy -",")) +
                typeConstraints + (-maybe(defer{ classBody }) or defer{ enumClassBody }) + -lex.semi
    }
    val primaryConstructor by lazyNamed {
        -maybe(modifiers + CONSTRUCTOR) + -"(" + (functionParameter joinedBy -",") + -")"
    }
    val classBody by lazyNamed {
        -maybe(-"{" + members + -"}")
    }
    val members by lazyNamed {
        memberDeclaration.many()
    }
    val delegationSpecifier by lazyNamed {
        constructorInvocation or (userType + -maybe(BY + defer{ expression }))
    }
    val constructorInvocation by lazyNamed {
        userType + invocationArguments
    }
    val typeParameters by lazyNamed {
        -"<" + -(typeParameter joinedBy -",") + -">"
    }
    val typeParameter by lazyNamed {
        modifiers + -lexeme(lex.identifier) + -maybe(-":" + userType)
    }
    val typeConstraints by lazyNamed {
        -maybe(WHERE + (typeConstraint joinedBy -","))
    }
    val typeConstraint by lazyNamed {
        annotations + lex.identifier + -":" + type
    }
    val memberDeclaration: NoResultParser by lazyNamed {
        oneOf(
                companionObject,
                objekt,
                function,
                property,
                klass,
                typeAlias,
                anonymousInitializer,
                secondaryConstructor
        )
    }
    val anonymousInitializer by lazyNamed {
        INIT + block
    }
    val companionObject by lazyNamed {
        modifiers + COMPANION + OBJECT + -maybe(lex.identifier) +
                -maybe(-":" + -(delegationSpecifier joinedBy -",")) +
                -maybe(defer{ classBody })
    }
    val valueParameters by lazyNamed {
        -"(" + -(functionParameter joinedBy -",") + -")"
    }
    val functionParameter by lazyNamed {
        modifiers + maybe(VAL or VAR) + parameter + -maybe(-"=" + defer{ expression })
    }
    val block by lazyNamed {
        -"{" + statements + -"}"
    }
    val function by lazyNamed {
        modifiers + FUN + -maybe(typeParameters) + -maybe(type + -".") + -lexeme(lex.identifier) +
                -maybe(typeParameters) + valueParameters + -maybe(-":" + type) +
                typeConstraints + -maybe(functionBody)
    }
    val functionBody by lazyNamed {
        block or (-"=" + defer{ expression })
    }
    val variableDeclarationEntry by lazyNamed {
        -lex.identifier + -maybe(-":" + type)
    }
    val multipleVariableDeclarations by lazyNamed {
        -"(" + -(variableDeclarationEntry joinedBy -",") + -")"
    }
    val property by lazyNamed {
        modifiers + (VAL or VAR) + -maybe(typeParameters) +
                -maybe(type + -".") + (multipleVariableDeclarations or variableDeclarationEntry) +
                typeConstraints + -maybe(BY or -"=" + expression + lex.semi) +
                -maybe((getter + maybe(setter)) or (setter + maybe(getter))) + -lex.semi
    }
    val getter by lazyNamed {
        oneOf(
                modifiers + GET,
                modifiers + GET + -"(" + -")" + -maybe(-":" + type) + functionBody
        )
    }
    val setter by lazyNamed {
        oneOf(
                modifiers + SET,
                modifiers + SET + -"(" + modifiers + (-lex.identifier or parameter) + -")" + -maybe(-":" + type) + functionBody
        )
    }
    val parameter by lazyNamed {
        -lex.identifier + -":" + defer{ type }
    }
    val objekt by lazyNamed {
        OBJECT + -lex.identifier + -maybe(-":" + -(delegationSpecifier joinedBy -",")) + -maybe(defer{ classBody })
    }
    val secondaryConstructor by lazyNamed {
        modifiers + keyword("constructor") + valueParameters + -maybe(-":" + constructorDelegationCall) + block
    }
    val constructorDelegationCall by lazyNamed {
        thisExpression + invocationArguments
    }
    val enumClassBody by lazyNamed {
        -"{" + enumEntries + -maybe(-";" + members) + -"}"
    }
    val enumEntries by lazyNamed {
        -(enumEntry joinedBy -",") + -maybe(-",") + -maybe(-";")
    }
    val enumEntry by lazyNamed {
        modifiers + -lex.identifier + -maybe(valueArguments) + -maybe(classBody)
    }
    val type: Parser<Char, Unit> by lazyNamed {
        typeModifiers + typeReference
    }
    val atomType: Parser<Char, Unit> by lazyNamed {
        -"(" + defer { typeReference } + -")" or userType
    }
    val nullableType by lazyNamed {
        atomType + -((-"?").many())
    }
    val functionType by lazyNamed {
        -maybe(nullableType + -".") + -"(" + -maybe((parameter or defer{ type }) joinedBy -",") + -")" + -"->" + -maybe(defer{ type })
    }
    val typeReference: Parser<Char, Unit> by lazyNamed {
        functionType or nullableType
    }
    val userType by lazyNamed { -(simpleUserType joinedBy -".") }
    val simpleUserType by lazyNamed {
        -lexeme(lex.identifier) + -maybe(-"<" + -(((optionalProjection + defer{ type }) or -"*") joinedBy -",") + -">")
    }
    val optionalProjection by lazyNamed{ -maybe(varianceModifier) }

    val modifiers by lazyNamed {
        -(modifier or annotation).many()
    }
    val typeModifiers by lazyNamed {
        -(keyword("suspend") or defer{ annotations }).many()
    }


    val modifier by lazyNamed {
        oneOf(
                classModifier,
                accessModifier,
                varianceModifier,
                memberModifier,
                parameterModifier,
                typeParameterModifier,
                functionModifier,
                propertyModifier
        )
    }
    val classModifier by lazyNamed {
        oneOf(
                keyword("abstract"),
                keyword("final"),
                keyword("enum"),
                keyword("open"),
                keyword("annotation"),
                keyword("sealed"),
                keyword("data")
        )
    }
    val memberModifier by lazyNamed {
        oneOf(
                keyword("override"),
                keyword("open"),
                keyword("final"),
                keyword("abstract"),
                keyword("lateinit")
        )
    }
    val accessModifier by lazyNamed {
        oneOf(
                keyword("private"),
                keyword("protected"),
                keyword("public"),
                keyword("internal")
        )
    }
    val varianceModifier by lazyNamed {
        keyword("in") or keyword("out")
    }
    val parameterModifier by lazyNamed {
        keyword("noinline") or keyword("crossinline") or keyword("vararg")
    }
    val typeParameterModifier by lazyNamed { keyword("reified") }
    val functionModifier by lazyNamed {
        keyword("tailrec") or
                keyword("operator") or
                keyword("infix") or
                keyword("inline") or
                keyword("external") or
                keyword("suspend")
    }
    val propertyModifier by lazyNamed {
        -"const"
    }
    val annotations by lazyNamed {
        annotation or annotationList
    }
    val annotation by lazyNamed {
        -"@" + -maybe(annotationUseSiteTarget + -":") + unescapedAnnotation
    }
    val annotationList by lazyNamed {
        -"@" + -maybe(annotationUseSiteTarget + -":") + -"[" + unescapedAnnotation.many() + -"]"
    }
    val annotationUseSiteTarget by lazyNamed {
        keyword("field") or
                keyword("file") or
                keyword("property") or
                keyword("get") or
                keyword("set") or
                keyword("receiver") or
                keyword("param") or
                keyword("setparam") or
                keyword("delegate")
    }
    val unescapedAnnotation: NoResultParser by lazyNamed {
        -(lex.identifier joinedBy -".") + -maybe(typeArguments) + -maybe(valueArguments)
    }

    val expression by lazyNamed { -disjunction }
    val assignment by lazyNamed {
        -assignableExpression + -assignmentOperator + expression
    }
    val assignmentOperator =
            -"=" or -"+=" or -"-=" or -"*=" or -"/=" or -"%="
    val disjunction by lazyNamed {
        -(conjunction joinedBy -"||")
    }
    val conjunction by lazyNamed {
        -(equality joinedBy -"&&")
    }
    val equality by lazyNamed {
        -(comparison joinedBy (-"==" or -"!=" or -"===" or -"!=="))
    }
    val comparison by lazyNamed {
        -(infixOperation joinedBy (-">" or -"<" or -">=" or -"<="))
    }
    val infixOperation by lazyNamed {
        elvisExpression + -isOrInOperation.many()
    }
    val isOrInOperation by lazyNamed {
        (isOperator + type) or (inOperator + elvisExpression)
    }
    val isOperator = keyword("is") or keyword("!is")
    val inOperator = keyword("in") or keyword("!in")
    val elvisExpression by lazyNamed {
        -(infixFunctionCall joinedBy -"?:")
    }
    val infixFunctionCall by lazyNamed {
        -(rangeExpression joinedBy -lex.identifier)
    }
    val rangeExpression by lazyNamed {
        -(additiveExpression joinedBy -"..")
    }
    val additiveExpression by lazyNamed {
        -(multiplicativeExpression joinedBy (-"+" or -"-"))
    }
    val multiplicativeExpression by lazyNamed {
        -(asExpression joinedBy (-"*" or -"/" or -"%"))
    }
    val asExpression by lazyNamed {
        prefixUnaryExpression + -(asOperator + type).many()
    }
    val asOperator = -("as?") or keyword("as")
    val prefixUnaryExpression: Parser<Char, Any> by lazyNamed {
        prefixUnaryOperator.many() + postfixUnaryExpression
    }
    val prefixUnaryOperator by lazyNamed {
        -"++" or -"--" or -"+" or -"-" or -"!" or annotation or label
    }
    val label by lazyNamed { -lex.identifier + -"@" }
    val postfixUnaryExpression: Parser<Char, Unit> by lazyNamed {
        -defer{ primaryExpression } + -postfixUnaryOperator.many()
    }
    val postfixUnaryOperator by lazyNamed {
        oneOf(
                -"++",
                -"--",
                -"!!",
                indexSuffix,
                memberAccessOperator + -lex.identifier,
                invocationArguments
        )
    }
    val memberAccessOperator by lazyNamed {
        oneOf(-".", -".?", -"::")
    }
    val invocationArguments by lazyNamed {
        -oneOf(
                 maybe(typeArguments) + maybe(valueArguments) + trailingLambda,
                maybe(typeArguments) + valueArguments,
                typeArguments
        )
    }
    val valueArguments: Parser<Char, Unit> by lazyNamed {
        -"(" + -maybe(lex.identifier + -"=") +
                -maybe(-"*") + -(defer{ expression } joinedBy -",") +
                -")"
    }
    val typeArguments by lazyNamed {
        -"<" + (type joinedBy -",") + -">"
    }
    val indexSuffix: Parser<Char, Unit> by lazyNamed {
        -"[" + -(defer{ expression } joinedBy -",") + -"]"
    }
    val assignableExpression by lazyNamed {
        postfixUnaryExpression
    }
    val primaryExpression by lazyNamed {
        parenthesizedExpression or
                literal or
                functionLiteral or
                objectLiteral or
                thisExpression or
                superExpression or
                -lex.identifier or
                callableReference or
                jumpExpression or
                ifExpression or
                loopExpression or
                tryExpression
    }
    val callableReference by lazyNamed {
        -maybe(type) + -"::" + -lex.identifier
    }
    val parenthesizedExpression: Parser<Char, Unit> by lazyNamed {
        -"(" + defer { expression } + -")"
    }
    val literal by lazyNamed {
        -lex.booleanLiteral or
                -lex.integerLiteral or
                -lex.floatLiteral or
                -lex.charLiteral or
                -lex.stringLiteral or
                -lex.nullLiteral
    }
    val thisExpression by lazyNamed {
        keyword("this") + -maybe(labelReference)
    }
    val superExpression by lazyNamed {
        keyword("super") + -maybe(superTypeReference) + -maybe(labelReference)
    }
    val superTypeReference by lazyNamed {
        -"<" + type + -">"
    }
    val labelReference by lazyNamed {
        -constant("@") + -lex.identifier
    }
    val controlStructureBody by lazyNamed {
        block or -(maybe(annotations) + expression)
    }
    val ifExpression: Parser<Char, Unit> by lazyNamed {
        keyword("if") + -"(" + expression + -")" + -controlStructureBody + -lex.semi + -maybe(elseBranch)
    }
    val elseBranch: Parser<Char, Unit> by lazyNamed {
        keyword("else") + controlStructureBody + -lex.semi
    }
    val tryExpression: Parser<Char, Unit> by lazyNamed {
        keyword("try") + block + -catchBlock.many() + -maybe(finallyBlock)
    }
    val catchBlock by lazyNamed {
        keyword("catch") + -"(" + annotations + -lex.identifier + -":" + userType + -")" + block
    }
    val finallyBlock by lazyNamed {
        keyword("finally") + block
    }
    val loopExpression by lazyNamed {
        forExpression or whileExpression or doWhileExpression
    }
    val forExpression by lazyNamed {
        keyword("for") + -"(" + -maybe(annotations) + (multipleVariableDeclarations or variableDeclarationEntry) +
                keyword("in") + expression + -")" + controlStructureBody
    }
    val whileExpression by lazyNamed {
        keyword("while") + -"(" + expression + -")" + controlStructureBody
    }
    val doWhileExpression by lazyNamed {
        keyword("do") + controlStructureBody + keyword("while") + -"(" + expression + -")"
    }
    val functionLiteral: Parser<Char, Unit> by lazyNamed {
        anonymousFunction or lambdaLiteral
    }
    val anonymousFunction by lazyNamed {
        FUN + valueParameters + -maybe(-":" + type) + functionBody
    }
    val lambdaLiteral by lazyNamed {
        block or (-"{" + -(multipleVariableDeclarations or variableDeclarationEntry).many() + -"->" +
                statements + -"}")
    }
    val statements by lazyNamed {
        -lex.semi.many() + -(defer{ statement } joinedBy -lex.semi) + -lex.semi.many()
    }
    val statement by lazyNamed {
        declaration or -(maybe(annotations) + defer{ expression })
    }
    val declaration: NoResultParser by lazyNamed {
        function or property or klass or typeAlias or objekt
    }
    val objectLiteral: Parser<Char, Unit> by lazyNamed {
        OBJECT + -maybe(-":" + (delegationSpecifier joinedBy -",")) + classBody
    }
    val jumpExpression: Parser<Char, Unit> by lazyNamed {
        oneOf(
                keyword("throw") + expression,
                -constant("return") + -maybe(labelReference) + -maybe(expression),
                -constant("continue") + -maybe(labelReference),
                -constant("break") + -maybe(labelReference)
        )
    }

    val trailingLambda: Parser<Char, Unit> by lazyNamed { -maybe(annotations) + -maybe(label) + lambdaLiteral}
}

fun String.escape(): String = flatMap {
    when {
        it.isLetterOrDigit() -> listOf(it)
        it == '\t' -> "\\t".asIterable()
        it == '\n' -> "\\n".asIterable()
        it == '\b' -> "\\b".asIterable()
        it == '\r' -> "\\r".asIterable()
        it == '\"' -> "\\\"".asIterable()
        it == '\'' -> "\\\'".asIterable()
        else -> listOf(it)
    }
}.joinToString("")

fun main(args: Array<String>) {
    val thisSource = File("src/main/kotlin/ru/spbstu/kparsec/examples/KotlinParser.kt").readText()
    val res = KotlinLexer.parse(thisSource)
    when (res) {
        is Success -> println(res.result.joinToString("\n") { "\"" + it.escape() + "\"" })
    }
    run {
        System.err.println(KotlinDeclParser.type.parse("Parser<T, R>"))
        System.err.println(KotlinDeclParser.function.parse("fun <T, R> Parser<T, R>.manyOneToString() = manyOne().map { it.joinToString(\"\") };\n"))
        //println(KotlinFileParser.parse(thisSource))
    }
}
