package org.jvnet.sorcerer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class ReservedWords {
    public static final Map<String,String> TOKENMAP = new HashMap<String,String>();
    static final Set<String> LIST = TOKENMAP.keySet();

    static {
        String[] table = new String[]{
            "abstract",     "a",
            "assert",       "as",
            "boolean",      "b",
            "break",        "br",
            "byte",         "by",
            "case",         "ca",
            "catch",        "ct",
            "char",         "ch",
            "class",        "c",
            "const",        "cs",
            "continue",     "co",
            "default",      "de",
            "do",           "d",
            "double",       "db",
            "else",         "e",
            "enum",         "en",
            "extends",      "ex",
            "final",        "f",
            "finally",      "fn",
            "float",        "fl",
            "for",          "fo",
            "goto",         "?", /* shouldnn't show up in the actual code */
            "if",           "i",
            "implements",   "im",
            "import",       "ip",
            "instanceof",   "is",
            "int",          "j",
            "interface",    "it",
            "long",         "l",
            "native",       "na",
            "new",          "n",
            "package",      "pa",
            "private",      "pi",
            "protected",    "po",
            "public",       "pu",
            "return",       "r",
            "short",        "sh",
            "static",       "s",
            "strictfp",     "sf",
            "super",        "su",
            "switch",       "sw",
            "synchronized", "sy",
            "this",         "t",
            "throw",        "tw",
            "throws",       "ts",
            "transient",    "tt",
            "try",          "tr",
            "void",         "v",
            "volatile",     "vl",
            "while",        "wh"
        };

        for( int i=0; i<table.length; i+=2 )
            TOKENMAP.put(table[i],table[i+1]);
    }
}
