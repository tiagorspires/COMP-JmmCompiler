grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
ADD : '+' ;
DIV : '/' ;
NOT : '!' ;
SUB : '-' ;
AND : '&&' ;
OR : '||' ;
LESS : '<' ;
GREATER : '>' ;
LSQPAREN : '[' ;
RSQPAREN : ']' ;
COMMA : ',' ;
MEMBERCALL : '.' ;
SIZE : '.length' ;
NEW : 'new' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
THIS : 'this' ;
ELLIPSIS : '...' ;

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;
EXTENDS : 'extends' ;
STATIC : 'static' ;
VOID : 'void' ;
STRING : 'String' ;

INTEGER : [0] | [1-9][0-9]* ;
TRUE : 'true' ;
FALSE : 'false' ;
BOOLEAN : 'boolean';
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

COMMENT_MULTILINE : '/*' .*? '*/' -> skip;
COMMENT_EOL : '//' .*? '\n' -> skip;
WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT value+=ID (MEMBERCALL value+=ID)* SEMI
    ;


classDecl
    : CLASS className=ID (EXTENDS extendClassName=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : type LSQPAREN RSQPAREN #Array //
    | BOOLEAN #Boolean //
    | INT #Integer //
    | STRING #String //
    | name=ID #Id //
    | VOID #Void //
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false, boolean hasEllipsis=false, boolean hasReturn=false]
    :(PUBLIC {$isPublic=true;})? (STATIC {$isStatic=true;})?
                   type name=ID
                   LPAREN (param (COMMA param)*) (COMMA INT ELLIPSIS {$hasEllipsis=true;} ellipsisName=ID)? RPAREN
                   LCURLY varDecl* stmt* (RETURN {$hasReturn=true;} expr SEMI)? RCURLY

    | (PUBLIC {$isPublic=true;})? (STATIC {$isStatic=true;})?
                    type name=ID
                    LPAREN (INT ELLIPSIS {$hasEllipsis=true;} ellipsisName=ID)? RPAREN
                    LCURLY varDecl* stmt* (RETURN {$hasReturn=true;} expr SEMI)? RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr SEMI #ExprStmt //
    | LCURLY stmt* RCURLY #StmtScope //
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt //
    | WHILE LPAREN expr RPAREN stmt #WhileStmt //
    | var= ID EQUALS expr SEMI #AssignStmt //
    | var= ID LSQPAREN expr RSQPAREN EQUALS expr SEMI #ArrayAssign //
    ;

expr
    : LPAREN expr RPAREN #Paren //
    | LSQPAREN (expr (COMMA expr)*)? RSQPAREN #ArrayInit //
    | expr LSQPAREN expr RSQPAREN #ArrayAccess //
    | expr SIZE #Length //
    | expr MEMBERCALL name=ID LPAREN (expr (COMMA expr)*)? RPAREN #FunctionCall //
    | expr LPAREN expr RPAREN #MemberCall //
    | value= THIS #Object //
    | value= NOT expr #Negation //
    | NEW INT LSQPAREN expr RSQPAREN #NewArray //
    | NEW name=ID LPAREN RPAREN #NewClass //
    | expr op= (MUL | DIV) expr #BinaryOp //
    | expr op= (ADD | SUB) expr #BinaryOp //
    | expr op= (LESS | GREATER) expr #BinaryOp //
    | expr op= AND expr #BinaryOp //
    | expr op= OR expr #BinaryOp //
    | value= INTEGER #IntegerLiteral //
    | value= (TRUE | FALSE) #BooleanLiteral //
    | name=ID #VarRefExpr //
    ;



