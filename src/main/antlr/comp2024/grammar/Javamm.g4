grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

COMMENT : '/*' .*? '*/' -> skip; // Multi-line comments
LINE_COMMENT : '//' ~[\r\n]* -> skip; // End-of-line comments

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
ADD : '+' ;
SUB : '-' ;
DIV : '/' ; // Define DIV token

CLASS : 'class' ;
INT : 'int' ;
STRING : 'String' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : [0-9]+ ; // Modified to allow multi-digit integers
ID : [a-zA-Z_$] [a-zA-Z0-9_$]* ;

WS : [ \t\n\r\f]+ -> skip ;

program : importDecl* classDecl  EOF;

importDecl : 'import' ID ( '.' ID )* SEMI; // Modified to include SEMI

classDecl : CLASS ID ('extends' ID)? LCURLY (varDecl)* (methodDecl)* RCURLY;

varDecl : type ID SEMI; // Modified to include SEMI

methodDecl : ('public'?)? type ID LPAREN parameters RPAREN LCURLY (varDecl)* (stmt)* returnStmt? RCURLY
                  | ('public'?)? 'static' 'void' 'main' LPAREN 'String' '[' ']' ID RPAREN LCURLY (varDecl)* (stmt)* RCURLY;

parameters : (parameter (',' parameter)*)?;

parameter : type ID;

returnStmt : 'return' expr SEMI?;


type : 'int' '[' ']' // Array type
     | 'int' '...'   // Varargs type
     | BOOLEAN
     | INT
     | STRING
     | ID;            // User-defined type

stmt : LCURLY (stmt)* RCURLY // Block stmt
          | 'if' LPAREN expr RPAREN stmt ('else' stmt)? // If stmt
          | 'while' LPAREN expr RPAREN stmt // While stmt
          | expr SEMI // expr stmt
          | ID EQUALS expr SEMI // Assignment stmt
          | ID '[' expr ']' EQUALS expr SEMI // Array assignment stmt
          ;

expr : expr ('&&' | '<' | ADD | SUB | MUL | DIV) expr // Binary expr
           | expr '[' expr ']' // Array access expr
           | expr '.' 'length' // Array length expr
           | expr '.' ID LPAREN (expr (',' expr)*)? RPAREN // Method call expr
           | ID LPAREN (expr (',' expr)*)? RPAREN // Method call expr without dot
           | 'new' 'int' '[' expr ']' // New int array expr
           | 'new' ID LPAREN RPAREN // New object expr
           | '!' expr // Logical negation expr
           | LPAREN expr RPAREN // Parenthesized expr
           | '[' (expr (',' expr)*)? ']' // Array literal expr
           | INTEGER // Integer literal expr
           | 'true' // Boolean literal expr (true)
           | 'false' // Boolean literal expr (false)
           | ID // Identifier expr
           | 'this'; // "this" expr
