package ru.spbstu.kparsec.parsers

import org.junit.Test
import ru.spbstu.kparsec.assertResult
import ru.spbstu.kparsec.examples.HaskellLexer
import ru.spbstu.kparsec.examples.Token
import ru.spbstu.kparsec.map
import ru.spbstu.kparsec.parse
import kotlin.test.assertEquals

class HaskellTest {
    val lexer = HaskellLexer.program + eof()

    @Test
    fun `haskell lexer works`() {
        println(HaskellLexer.varsym.parse("<|>"))
        println(lexer.parse("""
module Parser where

import Text.Parsec hiding(digit)

type Parser a = Parsec String ()  a

digit :: Parser Char
digit = oneOf "0123456789"

number :: Parser Integer
number = read <${'$'}> many1 digit

plusNumber :: Parser (Integer -> Integer)
plusNumber = do
                 charLiteral '+'
                 spaces
                 res <- prod
                 spaces
                 return (+res)

minusNumber :: Parser (Integer -> Integer)
minusNumber = do
                 charLiteral '-'
                 spaces
                 res <- prod
                 spaces
                 return ${'$'} \x -> x - res

summ :: Parser Integer
summ = do
            x <- prod
            spaces
            fs <- many (plusNumber <|> minusNumber)
            return ${'$'} foldl (flip (${'$'})) x fs

multNumber :: Parser (Integer -> Integer)
multNumber = do
                 charLiteral '*'
                 spaces
                 res <- expr
                 spaces
                 return (*res)

divNumber :: Parser (Integer -> Integer)
divNumber = do
                 charLiteral '/'
                 spaces
                 res <- expr
                 spaces
                 return (`div` res)

prod :: Parser Integer
prod = do
            x <- expr
            spaces
            fs <- many (multNumber <|> divNumber)
            return ${'$'} foldl (\ x f -> f x) x fs

expr :: Parser Integer
expr = number
    <|> do
            charLiteral '('
            spaces
            res <- summ
            spaces
            charLiteral ')'
            spaces
            return ${'$'} res

root = do
            d <- summ
            eof
            return ${'$'} d

main :: IO ()
main = do
            s <- getLine
            putStrLn ${'$'} show ${'$'} parse root "<INPUT>" s
            main
        """.trimIndent()).assertResult().filter { it.type != "spaces" })
    }
}