package ru.spbstu.kparsec.examples

import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.Success
import ru.spbstu.kparsec.parse
import ru.spbstu.kparsec.parsers.*
import java.io.File

data class Token<R>(val type: String, val result: R)

fun <T, R> Parser<T, R>.manyOneToString() = manyOne().map { it.joinToString("") }
fun <T, R> Parser<T, R>.manyToString() = many().map { it.joinToString("") }

infix fun <T> Parser<T, String>.concat(that: Parser<T, String>) = zip(this, that) { x, y -> x + y }
@JvmName("concatChar")
infix fun <T> Parser<T, Char>.concat(that: Parser<T, String>) = zip(this, that) { x, y -> x + y }
@JvmName("concatChar2")
infix fun <T> Parser<T, String>.concat(that: Parser<T, Char>) = zip(this, that) { x, y -> x + y }

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

    val shebang by lazy {
        -"#!" + inputCharacter.manyToString()
    }
    val inputCharacter by lazy {
        char { it !in "$CR$LF" }
    }
    val newLine by lazy {
        +"$LF" or (+"$CR" concat (+"$LF").orElse(""))
    }
    val inlineSpace by lazy {
        oneOf("$WS$TAB$FF").map { "$it" }
    }
    val whitespace by lazy {
        newLine or inlineSpace
    }
    val comment by lazy {
        eolComment or delimitedComment
    }
    val eolComment by lazy {
        +"//" concat inputCharacter.manyToString()
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
    val identifier by lazy {
        regularIdentifier or escapedIdentifier
    }
    val regularIdentifier by lazy {
        identifierStart concat identifierParts.orElse("")
    }
    val identifierStart by lazy { letter }
    val letter by lazy { char { Character.isAlphabetic(it.toInt()) } }
    val identifierParts by lazy { identifierPart.manyOneToString() }
    val identifierPart by lazy { identifierStart or digit or +'_' }
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
    val integerLiteral by lazy {
        (decimalLiteral or hexLiteral or binaryLiteral or +"0") concat integerLiteralSuffix.orElse("")
    }
    val integerSeparator by lazy { +"_" }
    val integerLiteralSuffix by lazy { +"L" }
    val decimalDigit by lazy { range('0'..'9').map { "$it" } }
    val positiveDigit by lazy { range('1'..'9').map { "$it" } }
    val decimalLiteral by lazy {
        positiveDigit concat decimalDigits.orElse("")
    }
    val decimalDigits by lazy { decimalDigit concat (decimalDigit or integerSeparator).manyToString() }
    val hexDigit by lazy {
        decimalDigit or oneOf("abcdefABCDEF").map { "$it" }
    }
    val hexDigits by lazy { hexDigit concat (hexDigit or integerSeparator).manyToString() }
    val hexLiteral by lazy {
        +"0" concat oneOf("xX").map { "$it" } concat hexDigits
    }
    val binaryDigit by lazy { oneOf("01").map { "$it" } }
    val binaryDigits by lazy { binaryDigit concat (binaryDigit or integerSeparator).manyToString() }
    val binaryLiteral by lazy {
        +"0" concat oneOf("bB").map { "$it" } concat binaryDigits
    }
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
        char { it !in "$CR$LF$BACKLASH" }.map { "$it" } or
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
        literalChar.filter { it !in "$DOUBLE_QUOTE$DOLLAR" } or interpolation or constant("$DOLLAR")
    }
    val interpolation by lazy {
        constant("$DOLLAR") concat interpolatedCode
    }
    val interpolatedCode by lazy {
        identifier or (constant("$LBRACE") concat defer { token }.manyToString() concat constant("$RBRACE"))
    }
    val rawStringLiteral by lazy {
        (char(DOUBLE_QUOTE) * 3).map { it.joinToString("") } concat
                rawStringLiteralPart.manyToString() concat
                (char(DOUBLE_QUOTE) * 3).map { it.joinToString("") }
    }
    val rawLiteralChar by lazy { char { it != DOLLAR } }
    val rawStringLiteralPart by lazy {
        rawLiteralChar.manyToString() or interpolation or constant("$DOLLAR")
    }

    val nullLiteral = +"null"
    val booleanLiteral = +"true" or +"false"

    val token by lazy {
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
    val inputElement by lazy {
        whitespace or comment or token
    }

    val semi = -";" or newLine
    override val self: Parser<Char, List<String>> = inputElement.many() + eof()
}

typealias NoResultParser = Parser<Char, Unit>

object KotlinFileParser : StringsAsParsers, DelegateParser<Char, Unit> {
    val lex = KotlinLexer
    val decls = KotlinDeclParser
    override val ignored: Parser<Char, Any?> = lex.whitespace or lex.comment

    override val self: NoResultParser get() = kotlinFile

    val kotlinFile: NoResultParser by lazy {
        preamble + decls
    }
    val script: NoResultParser by lazy {
        preamble + -decls.expression.many()
    }
    val preamble: NoResultParser by lazy {
        -maybe(fileAnnotations) + -maybe(packageHeader) + -import.many()
    }
    val fileAnnotations: NoResultParser by lazy { -fileAnnotation.many() }
    val fileAnnotation: NoResultParser by lazy {
        -"@" + -"file" + -":" + ((-"[" + -decls.unescapedAnnotation.manyOne() + -"]") or decls.unescapedAnnotation)
    }
    val packageHeader: NoResultParser by lazy {
        decls.modifiers + -"package" + -(lex.identifier joinedBy -".") + -lex.semi
    }
    val import: NoResultParser by lazy {
        -"import" + -(lex.identifier joinedBy -".") +
                -maybe((-"." + -"*") or (-"as" + lex.identifier)) +
                -lex.semi
    }
}

object KotlinDeclParser : StringsAsParsers, DelegateParser<Char, Unit> {
    val lex = KotlinLexer
    override val ignored: Parser<Char, Any?> = lex.whitespace or lex.comment

    override val self: Parser<Char, Unit> get() = -topLevelObject.many()

    fun keyword(text: String) = -text notFollowedBy lex.identifierPart
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

    val topLevelObject by lazy {
        klass or objekt or function or property or typeAlias
    }
    val typeAlias by lazy {
        modifiers + TYPEALIAS + -lex.identifier + -maybe(typeParameters) + -"=" + type + -lex.semi
    }
    val klass: Parser<Char, Unit> by lazy {
        modifiers + (CLASS or INTERFACE) + -lex.identifier +
                -maybe(typeParameters) + -maybe(primaryConstructor) +
                -maybe(-":" + annotations + (delegationSpecifier joinedBy -",")) +
                typeConstraints + (-maybe(classBody) or enumClassBody) + -lex.semi
    }
    val primaryConstructor by lazy {
        -maybe(modifiers + CONSTRUCTOR) + -"(" + (functionParameter joinedBy -",") + -")"
    }
    val classBody by lazy {
        -maybe(-"{" + members + -"}")
    }
    val members by lazy {
        memberDeclaration.many()
    }
    val delegationSpecifier by lazy {
        constructorInvocation or (userType + -maybe(BY + expression))
    }
    val constructorInvocation by lazy {
        userType + invocationArguments
    }
    val typeParameters by lazy {
        -"<" + -(typeParameter joinedBy -",") + -">"
    }
    val typeParameter by lazy {
        modifiers + -lex.identifier + -maybe(-":" + userType)
    }
    val typeConstraints by lazy {
        -maybe(WHERE + (typeConstraint joinedBy -","))
    }
    val typeConstraint by lazy {
        annotations + lex.identifier + -":" + type
    }
    val memberDeclaration: NoResultParser by lazy {
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
    val anonymousInitializer by lazy {
        INIT + block
    }
    val companionObject by lazy {
        modifiers + COMPANION + OBJECT + -maybe(lex.identifier) +
                -maybe(-":" + -(delegationSpecifier joinedBy -",")) +
                -maybe(classBody)
    }
    val valueParameters by lazy {
        -"(" + -(functionParameter joinedBy -",") + -")"
    }
    val functionParameter by lazy {
        modifiers + maybe(VAL or VAR) + parameter + -maybe(-"=" + expression)
    }
    val block by lazy {
        -"{" + statements + -"}"
    }
    val function by lazy {
        modifiers + FUN + -maybe(typeParameters) + -maybe(type + -".") + -lex.identifier +
                -maybe(typeParameters) + valueParameters + -maybe(-":" + type) +
                typeConstraints + -maybe(functionBody)
    }
    val functionBody by lazy {
        block or (-"=" + expression)
    }
    val variableDeclarationEntry by lazy {
        -lex.identifier + -maybe(-":" + type)
    }
    val multipleVariableDeclarations by lazy {
        -"(" + -(variableDeclarationEntry joinedBy -",") + -")"
    }
    val property by lazy {
        modifiers + (VAL or VAR) + -maybe(typeParameters) +
                -maybe(type + -".") + (multipleVariableDeclarations or variableDeclarationEntry) +
                typeConstraints + -maybe(BY or -"=" + expression + lex.semi) +
                -maybe((getter + maybe(setter)) or (setter + maybe(getter))) + -lex.semi
    }
    val getter by lazy {
        oneOf(
                modifiers + GET,
                modifiers + GET + -"(" + -")" + -maybe(-":" + type) + functionBody
        )
    }
    val setter by lazy {
        oneOf(
                modifiers + SET,
                modifiers + SET + -"(" + modifiers + (-lex.identifier or parameter) + -")" + -maybe(-":" + type) + functionBody
        )
    }
    val parameter by lazy {
        -lex.identifier + -":" + type
    }
    val objekt by lazy {
        OBJECT + -lex.identifier + -maybe(-":" + -(delegationSpecifier joinedBy -",")) + -maybe(classBody)
    }
    val secondaryConstructor by lazy {
        modifiers + keyword("constructor") + valueParameters + -maybe(-":" + constructorDelegationCall) + block
    }
    val constructorDelegationCall by lazy {
        thisExpression + invocationArguments
    }
    val enumClassBody by lazy {
        -"{" + enumEntries + -maybe(-";" + members) + -"}"
    }
    val enumEntries by lazy {
        -(enumEntry joinedBy -",") + -maybe(-",") + -maybe(-";")
    }
    val enumEntry by lazy {
        modifiers + -lex.identifier + -maybe(valueArguments) + -maybe(classBody)
    }
    val type: Parser<Char, Unit> by lazy {
        typeModifiers + typeReference
    }
    val typeReference: Parser<Char, Unit> by lazy {
        oneOf(
                -"(" + defer { typeReference } + -")",
                functionType,
                userType,
                nullableType
        )
    }
    val nullableType by lazy { typeReference + -"?" }
    val userType by lazy { -(simpleUserType joinedBy -".") }
    val simpleUserType by lazy {
        -lex.identifier + -maybe(-"<" + -(((optionalProjection + type) or -"*") joinedBy -",") + -">")
    }
    val optionalProjection by lazy{ varianceModifier }
    val functionType by lazy {
        -maybe(type + -".") + -"(" + -maybe(parameter joinedBy -",") + -")" + -"->" + -maybe(type)
    }
    val modifiers by lazy {
        -(modifier or annotation).many()
    }
    val typeModifiers by lazy {
        -(keyword("suspend") or annotations).many()
    }
    val modifier by lazy {
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
    val classModifier by lazy {
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
    val memberModifier by lazy {
        oneOf(
                keyword("override"),
                keyword("open"),
                keyword("final"),
                keyword("abstract"),
                keyword("lateinit")
        )
    }
    val accessModifier by lazy {
        oneOf(
                keyword("private"),
                keyword("protected"),
                keyword("public"),
                keyword("internal")
        )
    }
    val varianceModifier by lazy {
        keyword("in") or keyword("out")
    }
    val parameterModifier by lazy {
        keyword("noinline") or keyword("crossinline") or keyword("vararg")
    }
    val typeParameterModifier by lazy { keyword("reified") }
    val functionModifier by lazy {
        keyword("tailrec") or
                keyword("operator") or
                keyword("infix") or
                keyword("inline") or
                keyword("external") or
                keyword("suspend")
    }
    val propertyModifier by lazy {
        -"const"
    }
    val annotations by lazy {
        annotation or annotationList
    }
    val annotation by lazy {
        -"@" + -maybe(annotationUseSiteTarget + -":") + unescapedAnnotation
    }
    val annotationList by lazy {
        -"@" + -maybe(annotationUseSiteTarget + -":") + -"[" + unescapedAnnotation.many() + -"]"
    }
    val annotationUseSiteTarget by lazy {
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
    val unescapedAnnotation: NoResultParser by lazy {
        -(lex.identifier joinedBy -".") + -maybe(typeArguments) + -maybe(valueArguments)
    }

    val expression by lazy { -disjunction }
    val assignment by lazy {
        -assignableExpression + -assignmentOperator + expression
    }
    val assignmentOperator =
            -"=" or -"+=" or -"-=" or -"*=" or -"/=" or -"%="
    val disjunction by lazy {
        -(conjunction joinedBy -"||")
    }
    val conjunction by lazy {
        -(equality joinedBy -"&&")
    }
    val equality by lazy {
        -(comparison joinedBy (-"==" or -"!=" or -"===" or -"!=="))
    }
    val comparison by lazy {
        -(infixOperation joinedBy (-">" or -"<" or -">=" or -"<="))
    }
    val infixOperation by lazy {
        elvisExpression + -isOrInOperation.many()
    }
    val isOrInOperation by lazy {
        (isOperator + type) or (inOperator + elvisExpression)
    }
    val isOperator = keyword("is") or keyword("!is")
    val inOperator = keyword("in") or keyword("!in")
    val elvisExpression by lazy {
        -(infixFunctionCall joinedBy -"?:")
    }
    val infixFunctionCall by lazy {
        -(rangeExpression joinedBy -lex.identifier)
    }
    val rangeExpression by lazy {
        -(additiveExpression joinedBy -"..")
    }
    val additiveExpression by lazy {
        -(multiplicativeExpression joinedBy (-"+" or -"-"))
    }
    val multiplicativeExpression by lazy {
        -(asExpression joinedBy (-"*" or -"/" or -"%"))
    }
    val asExpression by lazy {
        prefixUnaryExpression + -(asOperator + type).many()
    }
    val asOperator = -("as?") or keyword("as")
    val prefixUnaryExpression: Parser<Char, Any> by lazy {
        prefixUnaryOperator.many() + postfixUnaryExpression
    }
    val prefixUnaryOperator by lazy {
        -"++" or -"--" or -"+" or -"-" or -"!" or annotation or label
    }
    val label by lazy { -lex.identifier + -"@" }
    val postfixUnaryExpression: Parser<Char, Unit> by lazy {
        -primaryExpression + -postfixUnaryOperator.many()
    }
    val postfixUnaryOperator by lazy {
        oneOf(
                -"++",
                -"--",
                -"!!",
                indexSuffix,
                memberAccessOperator + -lex.identifier,
                invocationArguments
        )
    }
    val memberAccessOperator by lazy {
        oneOf(-".", -".?", -"::")
    }
    val invocationArguments by lazy {
        -oneOf(
                 maybe(typeArguments) + maybe(valueArguments) + trailingLambda,
                maybe(typeArguments) + valueArguments,
                typeArguments
        )
    }
    val valueArguments: Parser<Char, Unit> by lazy {
        -"(" + -maybe(lex.identifier + -"=") +
                -maybe(-"*") + -(expression joinedBy -",") +
                -")"
    }
    val typeArguments by lazy {
        -"<" + (type joinedBy -",") + -">"
    }
    val indexSuffix: Parser<Char, Unit> by lazy {
        -"[" + -(expression joinedBy -",") + -"]"
    }
    val assignableExpression by lazy {
        postfixUnaryExpression
    }
    val primaryExpression by lazy {
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
    val callableReference by lazy {
        -maybe(type) + -"::" + -lex.identifier
    }
    val parenthesizedExpression: Parser<Char, Unit> by lazy {
        -"(" + defer { expression } + -")"
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
        keyword("this") + -maybe(labelReference)
    }
    val superExpression by lazy {
        keyword("super") + -maybe(superTypeReference) + -maybe(labelReference)
    }
    val superTypeReference by lazy {
        -"<" + type + -">"
    }
    val labelReference by lazy {
        -constant("@") + -lex.identifier
    }
    val controlStructureBody by lazy {
        block or -(maybe(annotations) + expression)
    }
    val ifExpression: Parser<Char, Unit> by lazy {
        keyword("if") + -"(" + expression + -")" + -controlStructureBody + -lex.semi + -maybe(elseBranch)
    }
    val elseBranch: Parser<Char, Unit> by lazy {
        keyword("else") + controlStructureBody + -lex.semi
    }
    val tryExpression: Parser<Char, Unit> by lazy {
        keyword("try") + block + -catchBlock.many() + -maybe(finallyBlock)
    }
    val catchBlock by lazy {
        keyword("catch") + -"(" + annotations + -lex.identifier + -":" + userType + -")" + block
    }
    val finallyBlock by lazy {
        keyword("finally") + block
    }
    val loopExpression by lazy {
        forExpression or whileExpression or doWhileExpression
    }
    val forExpression by lazy {
        keyword("for") + -"(" + -maybe(annotations) + (multipleVariableDeclarations or variableDeclarationEntry) +
                keyword("in") + expression + -")" + controlStructureBody
    }
    val whileExpression by lazy {
        keyword("while") + -"(" + expression + -")" + controlStructureBody
    }
    val doWhileExpression by lazy {
        keyword("do") + controlStructureBody + keyword("while") + -"(" + expression + -")"
    }
    val functionLiteral: Parser<Char, Unit> by lazy {
        anonymousFunction or lambdaLiteral
    }
    val anonymousFunction by lazy {
        FUN + valueParameters + -maybe(-":" + type) + functionBody
    }
    val lambdaLiteral by lazy {
        block or (-"{" + -(multipleVariableDeclarations or variableDeclarationEntry).many() + -"->" +
                statements + -"}")
    }
    val statements by lazy {
        -lex.semi.many() + -(statement joinedBy -lex.semi) + -lex.semi.many()
    }
    val statement by lazy {
        declaration or -(maybe(annotations) + expression)
    }
    val declaration: NoResultParser by lazy {
        function or property or klass or typeAlias or objekt
    }
    val objectLiteral: Parser<Char, Unit> by lazy {
        OBJECT + -maybe(-":" + (delegationSpecifier joinedBy -",")) + classBody
    }
    val jumpExpression: Parser<Char, Unit> by lazy {
        oneOf(
                keyword("throw") + expression,
                -constant("return") + -maybe(labelReference) + -maybe(expression),
                -constant("continue") + -maybe(labelReference),
                -constant("break") + -maybe(labelReference)
        )
    }

    val trailingLambda: Parser<Char, Unit> by lazy { -maybe(annotations) + -maybe(label) + lambdaLiteral}
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
    val res = KotlinLexer.parse(File("src/main/kotlin/ru/spbstu/kparsec/examples/KotlinParser.kt").readText())
    when (res) {
        is Success -> println(res.result.joinToString("\n") { "\"" + it.escape() + "\"" })
    }
}
