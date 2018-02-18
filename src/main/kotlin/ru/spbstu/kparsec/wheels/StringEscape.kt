package ru.spbstu.kparsec.wheels

fun String.escape() =
        replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")
                .replace("\"", "\\\"")
                .replace("\'", "\\\'")
