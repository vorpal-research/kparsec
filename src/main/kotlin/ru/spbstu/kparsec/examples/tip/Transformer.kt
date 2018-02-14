package ru.spbstu.kparsec.examples.tip

interface Transformer {
    fun transformExpr(expr: Expr): Expr = when (expr) {
        is Constant -> expr
        is Variable -> expr
        is Binary -> expr.copy(lhv = transformExpr(expr.lhv), rhv = transformExpr(expr.rhv))
        Input -> Input
        Alloc -> Alloc
        Null -> Null
        is Load -> Load(transformExpr(expr.ptr))
        is TakePtr -> expr
        is Call -> expr.copy(args = expr.args.map(::transformExpr))
        is InCall -> expr.copy(args = expr.args.map(::transformExpr))
    }

    fun transformStmt(stmt: Stmt): Collection<Stmt> = when (stmt) {
        is Assignment -> listOf(stmt.copy(value = transformExpr(stmt.value)))
        is Output -> listOf(stmt.copy(expr = transformExpr(stmt.expr)))
        is Store -> listOf(stmt.copy(value = transformExpr(stmt.value)))
        is If -> listOf(stmt.copy(
                condition = transformExpr(stmt.condition),
                trueBranch = transformBlock(stmt.trueBranch),
                falseBranch = stmt.falseBranch?.let(this::transformBlock)
        ))
        is While -> listOf(stmt.copy(
                condition = transformExpr(stmt.condition),
                body = transformBlock(stmt.body)
        ))
    }

    fun transformBlock(block: Block): Block = block.copy(statements = block.statements.flatMap { transformStmt(it) })
    fun transformFunction(function: Function): Collection<Function> =
            listOf(function.copy(body = transformBlock(function.body), ret = transformExpr(function.ret)))

    fun trasformProgram(program: Program): Program =
            program.copy(functions = program.functions.flatMap { transformFunction(it) })
}

interface Visitor<T> {
    fun default(): T
    fun combine(values: Collection<T>): T
    fun combine(vararg values: T): T = combine(values.asList())

    fun visitExpr(expr: Expr): T = when (expr) {
        is Constant -> default()
        is Variable -> default()
        is Binary -> combine(visitExpr(expr.lhv), visitExpr(expr.rhv))
        Input -> default()
        Alloc -> default()
        Null -> default()
        is Load -> combine(visitExpr(expr.ptr))
        is TakePtr -> default()
        is Call -> combine(expr.args.map(::visitExpr))
        is InCall -> combine(expr.args.map(::visitExpr))
    }

    fun visitStmt(stmt: Stmt): T = when (stmt) {
        is Assignment -> combine(visitExpr(stmt.value))
        is Output -> combine(visitExpr(stmt.expr))
        is Store -> combine(visitExpr(stmt.value))
        is If -> when {
            stmt.falseBranch != null -> combine(
                    visitExpr(stmt.condition),
                    visitBlock(stmt.trueBranch),
                    visitBlock(stmt.falseBranch)
            )
            else -> combine(
                    visitExpr(stmt.condition),
                    visitBlock(stmt.trueBranch)
            )
        }
        is While -> combine(
                visitExpr(stmt.condition),
                visitBlock(stmt.body)
        )
    }

    fun visitBlock(block: Block): T = combine(block.statements.map { visitStmt(it) })

    fun visitFunction(function: Function): T =
            combine(visitBlock(function.body), visitExpr(function.ret))

    fun visitProgram(program: Program): T =
            combine(program.functions.map { visitFunction(it) })
}
