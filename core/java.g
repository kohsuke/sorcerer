header {
package org.jvnet.sorcerer.impl;
}

//----------------------------------------------------------------------------
// The Java scanner
//----------------------------------------------------------------------------
class JavaLexer extends Lexer;

options {
	exportVocab=Java;		// call the vocabulary "Java"
	testLiterals=false;		// don't automatically test for literals
	k=4;					// four characters of lookahead
	charVocabulary='\u0003'..'\uFFFE';
	// without inlining some bitset tests, couldn't do unicode;
	// I need to make ANTLR generate smaller bitsets; see
	// bottom of JavaLexer.java
	codeGenBitsetTestThreshold=20;
    classHeaderPrefix = "public";

}

{
	/** flag for enabling the "assert" keyword */
	private boolean assertEnabled = true;
	/** flag for enabling the "enum" keyword */
	private boolean enumEnabled = true;

	/** Enable the "assert" keyword */
	public void enableAssert(boolean shouldEnable) { assertEnabled = shouldEnable; }
	/** Query the "assert" keyword state */
	public boolean isAssertEnabled() { return assertEnabled; }
	/** Enable the "enum" keyword */
	public void enableEnum(boolean shouldEnable) { enumEnabled = shouldEnable; }
	/** Query the "enum" keyword state */
	public boolean isEnumEnabled() { return enumEnabled; }
}

// OPERATORS
QUESTION		:	'?'		;
LPAREN			:	'('		;
RPAREN			:	')'		;
LBRACK			:	'['		;
RBRACK			:	']'		;
LCURLY			:	'{'		;
RCURLY			:	'}'		;
COLON			:	':'		;
COMMA			:	','		;
//DOT			:	'.'		;
ASSIGN			:	'='		;
EQUAL			:	"=="	;
LNOT			:	'!'		;
BNOT			:	'~'		;
NOT_EQUAL		:	"!="	;
DIV				:	'/'		;
DIV_ASSIGN		:	"/="	;
PLUS			:	'+'		;
PLUS_ASSIGN		:	"+="	;
INC				:	"++"	;
MINUS			:	'-'		;
MINUS_ASSIGN	:	"-="	;
DEC				:	"--"	;
STAR			:	'*'		;
STAR_ASSIGN		:	"*="	;
MOD				:	'%'		;
MOD_ASSIGN		:	"%="	;
SR				:	">>"	;
SR_ASSIGN		:	">>="	;
BSR				:	">>>"	;
BSR_ASSIGN		:	">>>="	;
GE				:	">="	;
GT				:	">"		;
SL				:	"<<"	;
SL_ASSIGN		:	"<<="	;
LE				:	"<="	;
LT				:	'<'		;
BXOR			:	'^'		;
BXOR_ASSIGN		:	"^="	;
BOR				:	'|'		;
BOR_ASSIGN		:	"|="	;
LOR				:	"||"	;
BAND			:	'&'		;
BAND_ASSIGN		:	"&="	;
LAND			:	"&&"	;
SEMI			:	';'		;


// Whitespace -- ignored
WS	:	(	' '
		|	'\t'
		|	'\f'
			// handle newlines
		|	(	options {generateAmbigWarnings=false;}
			:	"\r\n"	// Evil DOS
			|	'\r'	// Macintosh
			|	'\n'	// Unix (the right way)
			)
			{ newline(); }
		)+
		{ _ttype = Token.SKIP; }
	;

// Single-line comments
SL_COMMENT
	:	"//"
		(~('\n'|'\r'))*
	;

// multiple-line comments
ML_COMMENT
	:	"/*"
		(	/*	'\r' '\n' can be matched in one alternative or by matching
				'\r' in one iteration and '\n' in another. I am trying to
				handle any flavor of newline that comes in, but the language
				that allows both "\r\n" and "\r" and "\n" to all be valid
				newline is ambiguous. Consequently, the resulting grammar
				must be ambiguous. I'm shutting this warning off.
			 */
			options {
				generateAmbigWarnings=false;
			}
		:
			{ LA(2)!='/' }? '*'
		|	'\r' '\n'		{newline();}
		|	'\r'			{newline();}
		|	'\n'			{newline();}
		|	~('*'|'\n'|'\r')
		)*
		"*/"
		{/*$setType(Token.SKIP);*/}
	;


// character literals
CHAR_LITERAL
	:	'\'' ( ESC | ~('\''|'\n'|'\r'|'\\') ) '\''
	;

// string literals
STRING_LITERAL
	:	'"' (ESC|~('"'|'\\'|'\n'|'\r'))* '"'
	;


// escape sequence -- note that this is protected; it can only be called
// from another lexer rule -- it will not ever directly return a token to
// the parser
// There are various ambiguities hushed in this rule. The optional
// '0'...'9' digit matches should be matched here rather than letting
// them go back to STRING_LITERAL to be matched. ANTLR does the
// right thing by matching immediately; hence, it's ok to shut off
// the FOLLOW ambig warnings.
protected
ESC
	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	'0'..'3'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	'0'..'7'
				(
					options {
						warnWhenFollowAmbig = false;
					}
				:	'0'..'7'
				)?
			)?
		|	'4'..'7'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	'0'..'7'
			)?
		)
	;


// hexadecimal digit (again, note it's protected!)
protected
HEX_DIGIT
	:	('0'..'9'|'A'..'F'|'a'..'f')
	;


// a dummy rule to force vocabulary to be all characters (except special
// ones that ANTLR uses internally (0 to 2)
protected
VOCAB
	:	'\3'..'\377'
	;

protected ID_START:
        '_' | '$' | 
        (            
            {Character.isLetter(LA(1))}? 
            ~(
                '_' | '$' | '/' | '*' | '0'..'9' | 
                '.' | '\'' | '\\' | '"' | '\t' | '\n' | 
                '\r' | ' ' | '\f' | '(' | ')' |
                '{' | '}' | '[' | ']'| ';' | ',' | '=' |
                '+' | '~' | '&' | '<' | '>' | '-' | '!' |
                '^' | '%' | ':' | '?' | '|'| '@'
            )
        )
    ;

protected ID_PART :
        '_' | '$' | 
        (
            {Character.isLetterOrDigit(LA(1))}?
            ~(
                '_' | '$' | '/' | '*' | '0'..'9' | 
                '.' | '\'' | '\\' | '"' | '\t' | '\n' | 
                '\r' | ' ' | '\f' | '(' | ')' |
                '{' | '}' | '[' | ']'| ';' | ',' | '=' |
                '+' | '~' | '&' | '<' | '>' | '-' | '!' |
                '^' | '%' | ':' | '?' | '|' | '@'
            )
        )
    ;


// an identifier. Note that testLiterals is set to true! This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer
IDENT
	options {testLiterals=true;}
	:	ID_START (ID_PART)*
/*		{
			// check if "assert" keyword is enabled
			if (assertEnabled && "assert".equals($getText)) {
				$setType(LITERAL_assert); // set token type for the rule in the parser
			}
			// check if "enum" keyword is enabled
			if (enumEnabled && "enum".equals($getText)) {
				$setType(LITERAL_enum); // set token type for the rule in the parser
			}
		}*/
	;


// a numeric literal
NUM_INT
	{boolean isDecimal=false; Token t=null;}
	:	'.' /*{_ttype = DOT;}*/
			(
				(('0'..'9')+ (EXPONENT)? (f1:FLOAT_SUFFIX {t=f1;})?
				/*{
				if (t != null && t.getText().toUpperCase().indexOf('F')>=0) {
					_ttype = NUM_FLOAT;
				}
				else {
					_ttype = NUM_DOUBLE; // assume double
				}
				}*/)
				|
				// JDK 1.5 token for variable length arguments
				(".." /*{_ttype = TRIPLE_DOT;}*/)
			)?

	|	(	'0' {isDecimal = true;} // special case for just '0'
			(	('x'|'X')
				(											// hex
					// the 'e'|'E' and float suffix stuff look
					// like hex digits, hence the (...)+ doesn't
					// know when to stop: ambig. ANTLR resolves
					// it correctly by matching immediately. It
					// is therefor ok to hush warning.
					options {
						warnWhenFollowAmbig=false;
					}
				:	HEX_DIGIT
				)+

			|	//float or double with leading zero
				(('0'..'9')+ ('.'|EXPONENT|FLOAT_SUFFIX)) => ('0'..'9')+

			|	('0'..'7')+									// octal
			)?
		|	('1'..'9') ('0'..'9')*  {isDecimal=true;}		// non-zero decimal
		)
		(	('l'|'L') /*{ _ttype = NUM_LONG; }*/

		// only check to see if it's a float if looks like decimal so far
		|	{isDecimal}?
			(	'.' ('0'..'9')* (EXPONENT)? (f2:FLOAT_SUFFIX {t=f2;})?
			|	EXPONENT (f3:FLOAT_SUFFIX {t=f3;})?
			|	f4:FLOAT_SUFFIX {t=f4;}
			)
			/*{
			if (t != null && t.getText().toUpperCase() .indexOf('F') >= 0) {
				_ttype = NUM_FLOAT;
			}
			else {
				_ttype = NUM_DOUBLE; // assume double
			}
			}*/
		)?
	;

// JDK 1.5 token for annotations and their declarations
AT
	:	'@'
	;

// a couple protected methods to assist in matching floating point numbers
protected
EXPONENT
	:	('e'|'E') ('+'|'-')? ('0'..'9')+
	;


protected
FLOAT_SUFFIX
	:	'f'|'F'|'d'|'D'
	;
