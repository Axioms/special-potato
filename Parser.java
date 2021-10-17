/*
File name: Parser.java
What it does:
1. public Parser(String fileName): Invokes the lexical analyzer with the file as the argument
2. public Program parse() 
    2.1 searches for next token and specifies that it has to be FUNCTION_TOK 
    i.e. every program starts with function
    2.2 gets the function name (ex. function a)
    2.3 looks for the next tokens and they have to be 
    LEFT_PAREN_TOK and RIGHT_PAREN_TOK because Julia function declarations
    are of the form function a ( )
    2.4 Gets block
    2.5 makes sure the function ends with END_TOK
    2.6 Does error handling and says anything after end is garbage
    2.7 return new Program (blk);
3. private Block getBlock() is the getter method for Block.java. It adds statements to the 
block for the interpreter. 
4. getStatement() declares a stmt and calls Token tok = lex.getLookaheadToken(); to determine what token
are valid starts to statements AND sets stmt equal to the getter method that corresponds with what 
token is at the beginning of the statement (for ex. if it starts with DISPLAY_TOK then stmt is a 
PrintStatement so it calls getPrintStatement)
5. getAssignmentStatement() addresses statements of the form var = expr. It assigns var to getId and 
scans for the next token which MUST be ASSIGN_TOK (which is "="). Then it assigns expr to 
getArithmeticExpression  which categorizes arithmetic expressions based on what tokens they start with 
(ex. constant, Id, etc.)
6. GENERAL FORMAT OF THE GETTER METHODS OF TYPE STATEMENT:
     private Statement get<whatever method this applies to>() throws ParserException, LexicalException
    {
        Token tok = lex.getNextToken();
        match (tok, TokenType.<whichever token>_TOK);
        tok = lex.getNextToken ();
        match (tok, TokenType.<whichever token>_TOK);
        tok = lex.getNextToken ();
        return new <whichever file this is associated with> (arg1, arg2, etc.);
    } 
    GENERAL FORMAT OF THE GETTER METHODS OF TYPE ARITHMETICEXPRESSION, BINARY, OR BOOLEANEXPRESSION:
     private <whichever file this applies to> get<what it applies to>() throws ParserException, LexicalException
    {
        <declare an op>
        Token tok = lex.getNextToken();
        if (tok.getTokType() == TokenType.<whichever token>)
            op = <operator>.<token>;
        else if (tok.getTokType() == TokenType.<whichever token>)
            op = <operator>.<token>;
        ...
        else
            throw new ParserException ("operator expected at row " +
                    tok.getRowNumber()  + " and column " + tok.getColumnNumber());

        ArithmeticExpression expr1 = getArithmeticExpression();
        ArithmeticExpression expr2 = getArithmeticExpression ();
        return new <type of expression>Expression (op, expr1, expr2);
    }
   
7. private boolean isValidStartOfStatement(Token tok): this specifies what tokens a statement can start with
(id, if, while, for, display). It's a boolean because it evaluates to T/F and throws an exception 
if the statement starts with something that isn't a token it specified.
8. match() basically uses lookahead to make sure tokens match token types.
*/

import java.io.FileNotFoundException;

public class Parser
{
    private LexicalAnalyzer lex;

    public Parser(String fileName) throws FileNotFoundException, LexicalException
    {
        lex = new LexicalAnalyzer (fileName);
    }

    public Program parse () throws ParserException, LexicalException
    {
        Token tok = lex.getNextToken();
        match (tok, TokenType.FUNCTION_TOK);
        Id functionName = getId();
        tok = lex.getNextToken ();
        match (tok, TokenType.LEFT_PAREN_TOK);
        tok = lex.getNextToken ();
        match (tok, TokenType.RIGHT_PAREN_TOK);
        Block blk = getBlock();
        tok = lex.getNextToken();
        match (tok, TokenType.END_TOK);
        tok = lex.getNextToken();
        if (tok.getTokType() != TokenType.EOS_TOK)
            throw new ParserException ("garbage at end of file");
        return new Program (blk);
    }

    private Block getBlock() throws ParserException, LexicalException
    {
        Block blk = new Block();
        Token tok = lex.getLookaheadToken();
        while (isValidStartOfStatement (tok))
        {
            Statement stmt = getStatement();
            blk.add (stmt);
            tok = lex.getLookaheadToken();
        }
        return blk;
    }

    private Statement getStatement() throws ParserException, LexicalException
    {
        Statement stmt;
        Token tok = lex.getLookaheadToken();
        if (tok.getTokType() == TokenType.IF_TOK)
            stmt = getIfStatement();
        else if (tok.getTokType() == TokenType.WHILE_TOK)
            stmt = getWhileStatement();
        else if (tok.getTokType() == TokenType.DISPLAY_TOK)
            stmt = getPrintStatement();
        else if (tok.getTokType() == TokenType.ID_TOK)
            stmt = getAssignmentStatement();
        else if (tok.getTokType() == TokenType.FOR_TOK)
            stmt = getForStatement();

        else
            throw new ParserException ("invalid statement at row " +
                    tok.getRowNumber()  + " and column " + tok.getColumnNumber());
        return stmt;
    }

    private Statement getAssignmentStatement() throws ParserException, LexicalException
    {
        Id var = getId();
        Token tok = lex.getNextToken();
        match (tok, TokenType.ASSIGN_TOK);
        ArithmeticExpression expr = getArithmeticExpression();
        return new AssignmentStatement (var, expr);
    }
    /*
    private Statement getPrintStatement() throws ParserException, LexicalException
    {
        Token tok = lex.getNextToken();
        match (tok, TokenType.PRINT_TOK);
        tok = lex.getNextToken ();
        match (tok, TokenType.LEFT_PAREN_TOK);
        ArithmeticExpression expr = getArithmeticExpression();
        tok = lex.getNextToken ();
        match (tok, TokenType.RIGHT_PAREN_TOK);
        return new PrintStatement (expr);
    } */
    private Statement getPrintStatement() throws ParserException, LexicalException
    {
        Token tok = lex.getNextToken();
        match (tok, TokenType.DISPLAY_TOK);
        tok = lex.getNextToken ();
        match (tok, TokenType.QUOTE_TOK);
        ArithmeticExpression expr = getArithmeticExpression();
        tok = lex.getNextToken ();
        match (tok, TokenType.QUOTE_TOK);
        return new PrintStatement (expr);
    } 

    private Statement getForStatement() throws ParserException, LexicalException
    {
        Token tok = lex.getNextToken();
        match (tok, TokenType.FOR_TOK);
        Id var = getId();
        tok = lex.getNextToken();
        match (tok, TokenType.ASSIGN_TOK);
        ArithmeticExpression expr1 = getArithmeticExpression();
        tok = lex.getNextToken();
        match (tok, TokenType.COL_TOK);
        ArithmeticExpression expr2 = getArithmeticExpression();
        Block blk = getBlock();
        tok = lex.getNextToken();
        match (tok, TokenType.END_TOK);
        Iter it = new Iter(expr1, expr2);
        return new ForStatement (var, it, blk);
    }

    private Statement getWhileStatement() throws ParserException, LexicalException
    {
        Token tok = lex.getNextToken ();
        match (tok, TokenType.WHILE_TOK);
        BooleanExpression expr = getBooleanExpression();
        Block blk = getBlock();
        tok = lex.getNextToken();
        match (tok, TokenType.END_TOK);
        return new WhileStatement (expr, blk);
    }

    private Statement getIfStatement() throws ParserException, LexicalException
    {
        Token tok = lex.getNextToken ();
        match (tok, TokenType.IF_TOK);
        BooleanExpression expr = getBooleanExpression();
        Block blk1 = getBlock();
        tok = lex.getNextToken ();
        match (tok, TokenType.ELSE_TOK);
        Block blk2 = getBlock();
        tok = lex.getNextToken();
        match (tok, TokenType.END_TOK);
        return new IfStatement (expr, blk1, blk2);
    }

    private boolean isValidStartOfStatement(Token tok)
    {
        assert (tok != null);
        return tok.getTokType() == TokenType.ID_TOK ||
                tok.getTokType() == TokenType.IF_TOK ||
                tok.getTokType() == TokenType.WHILE_TOK ||
                tok.getTokType() == TokenType.FOR_TOK ||
                tok.getTokType() == TokenType.DISPLAY_TOK;
    }


    /**************************************************************
     * implements the production <expr> -> <operator> <expr> <expr> | id | constant
     */
    private ArithmeticExpression getArithmeticExpression() throws ParserException, LexicalException
    {
        ArithmeticExpression expr;
        Token tok = lex.getLookaheadToken ();
        if (tok.getTokType() == TokenType.ID_TOK)
            expr = getId();
        else if (tok.getTokType() == TokenType.CONST_TOK)
            expr = getConstant();
        else
            expr = getBinaryExpression();
        return expr;
    }

    /****************************************************
     * implements the production <expr> -> <operator> <expr> <expr>
     */
    private ArithmeticExpression getBinaryExpression() throws ParserException, LexicalException
    {
        ArithmeticOperator op;
        Token tok = lex.getNextToken();
        if (tok.getTokType() == TokenType.ADD_TOK)
        {
            match (tok, TokenType.ADD_TOK);
            op = ArithmeticOperator.ADD_OP;
        }
        else if (tok.getTokType() == TokenType.SUB_TOK)
        {
            match (tok, TokenType.SUB_TOK);
            op = ArithmeticOperator.SUB_OP;
        }
        else if (tok.getTokType() == TokenType.MUL_TOK)
        {
            match (tok, TokenType.MUL_TOK);
            op = ArithmeticOperator.MUL_OP;
        }
        else if (tok.getTokType() == TokenType.DIV_TOK)
        {
            match (tok, TokenType.DIV_TOK);
            op = ArithmeticOperator.DIV_OP;
        }
        else if (tok.getTokType() == TokenType.REV_DIV_TOK)
        {
            match (tok, TokenType.REV_DIV_TOK);
            op = ArithmeticOperator.REV_DIV_OP;
        }
        else if (tok.getTokType() == TokenType.EXP_TOK)
        {
            match (tok, TokenType.EXP_TOK);
            op = ArithmeticOperator.EXP_OP;
        }
        else if (tok.getTokType() == TokenType.MOD_TOK)
        {
            match (tok, TokenType.MOD_TOK);
            op = ArithmeticOperator.MOD_OP;
        }
        else
            throw new ParserException (" operator expected at row " +
                    tok.getRowNumber() +" and column "  + tok.getColumnNumber());
        ArithmeticExpression expr1 = getArithmeticExpression();
        ArithmeticExpression expr2 = getArithmeticExpression();
        return new BinaryExpression(op, expr1, expr2);
    }

    private BooleanExpression getBooleanExpression() throws ParserException, LexicalException
    {
        RelativeOperator op;
        Token tok = lex.getNextToken();
        if (tok.getTokType() == TokenType.EQ_TOK)
            op = RelativeOperator.EQ_OP;
        else if (tok.getTokType() == TokenType.NE_TOK)
            op = RelativeOperator.NE_OP;
        else if (tok.getTokType() == TokenType.GT_TOK)
            op = RelativeOperator.GT_OP;
        else if (tok.getTokType() == TokenType.GE_TOK)
            op = RelativeOperator.GE_OP;
        else if (tok.getTokType() == TokenType.LT_TOK)
            op = RelativeOperator.LT_OP;
        else if (tok.getTokType() == TokenType.LE_TOK)
            op = RelativeOperator.LE_OP;
        else
            throw new ParserException ("relational operator expected at row " +
                    tok.getRowNumber()  + " and column " + tok.getColumnNumber());

        ArithmeticExpression expr1 = getArithmeticExpression();
        ArithmeticExpression expr2 = getArithmeticExpression ();
        return new BooleanExpression (op, expr1, expr2);
    }

    private Id getId() throws LexicalException, ParserException
    {
        Token tok = lex.getNextToken();
        match (tok, TokenType.ID_TOK);
        return new Id (tok.getLexeme().charAt(0));
    }

    private ArithmeticExpression getConstant() throws ParserException, LexicalException
    {
        Token tok = lex.getNextToken();
        match (tok, TokenType.CONST_TOK);
        int value = Integer.parseInt(tok.getLexeme());
        return new Constant (value);
    }

    private void match(Token tok, TokenType tokType) throws ParserException
    {
        assert (tok != null && tokType != null);
        if (tok.getTokType() != tokType)
            throw new ParserException (tokType.name() + " expected at row " +
                    tok.getRowNumber() +" and column "  + tok.getColumnNumber());
    }
}
