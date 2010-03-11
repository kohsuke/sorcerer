package sorcerer.client.data;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Adapter on top of {@link ASTVisitor} to parse the encoded sorcerer-generated AST definition.
 *
 * @author Kohsuke Kawaguchi
 */
class Parser extends JavaScriptObject {
    protected Parser() {}

    public static native Parser create(ASTVisitor c) /*-{
        var f = {
            // tables
            //=============================================
            typeTable : function() {// var args
                c.@sorcerer.client.data.ASTVisitor::typeTable(Lcom/google/gwt/core/client/JsArray;)(arguments);
            },
            methodTable : function() {// var args
                c.@sorcerer.client.data.ASTVisitor::methodTable(Lcom/google/gwt/core/client/JsArray;)(arguments);
            },
            localVariableTable : function() {//var args
                c.@sorcerer.client.data.ASTVisitor::localVariableTable(Lcom/google/gwt/core/client/JsArray;)(arguments);
            },
            classDef : function() {// var args
                return this.$$(arguments);
            },


            // meat
            //=============================================
            reservedWord : function(name) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::reservedWord(Ljava/lang/String;)(name);
                }
            },
            primitiveType : function(name) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::primitiveType(Ljava/lang/String;)(name);
                }
            },
            sourceText : function(text) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::sourceText(Ljava/lang/String;)(text);
                }
            },
            O : function() {// var args
                var head = arguments[0];
                var children = this.$$(arguments);
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::comment(Ljava/lang/String;Lsorcerer/client/js/JsFunction;)(head,children);
                }
            },
            w : function(n) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::whitespace(I)(n);
                }
            },
            nl : function() {
                c.@sorcerer.client.data.ASTVisitor::nl()();
            },
            C : function(tti,descendants,children) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::typeDef(ILcom/google/gwt/core/client/JsArrayInteger;Lsorcerer/client/js/JsFunction;)(tti,descendants,children);
                }
            },
            I : function(text) {
                var f = function() {
                    c.@sorcerer.client.data.ASTVisitor::identifier(Ljava/lang/String;)(text);
                };
                f.identifier = text; // so that the parent can figure out the id of the children
                return f;
            },

            B : function() {// var args
                var children = this.$$(arguments);
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::curlyBrace(Lsorcerer/client/js/JsFunction;)(children);
                }
            },
            P : function() {// var args
                var children = this.$$(arguments);
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::parenthesis(Lsorcerer/client/js/JsFunction;)(children);
                }
            },
            M : function(mti,sup,sub,children) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::methodDef(ILcom/google/gwt/core/client/JsArrayInteger;Lcom/google/gwt/core/client/JsArrayInteger;Lsorcerer/client/js/JsFunction;)(mti,sup,sub,children);
                }
            },
            N : function(mti) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::methodRef(I)(mti);
                }
            },
            T : function(tti) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::typeRef(I)(tti);
                }
            },
            F : function() {// var args
                var children = this.$$(arguments);
                var args = arguments;
                var name = (function() {
                    for( var i=0; i<args.length; i++) {
                        if(args[i].identifier)
                            return args[i].identifier;
                    }
                })();

                return function() {
                    c.@sorcerer.client.data.ASTVisitor::fieldDef(Ljava/lang/String;Lsorcerer/client/js/JsFunction;)(name,children);
                }
            },
            G : function(tti,mods,name) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::fieldRef(ILjava/lang/String;Ljava/lang/String;)(tti,mods,name);
                }
            },
            V : function(vti,children) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::variableDef(ILsorcerer/client/js/JsFunction;)(vti,children);
                }
            },
            W : function(vti) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::variableRef(I)(vti);
                }
            },
            L : function(literal) {
                return function() {
                    c.@sorcerer.client.data.ASTVisitor::literal(Ljava/lang/String;)(literal);
                }
            },

            // internal
            //=============================================
            conv : function(a) {
                if(typeof a=='string')
                    a=this.sourceText(a);
                return a;
            },
            $ : function() {//var args
                // convert one instance
                var len=arguments.length;
                if(len==1)  return this.conv(arguments[0]);
                return this.$$(arguments);
            },
            $$ : function(array) {
                // pack multiple items into a container
                var len=array.length;
                for( var i=len-1; i>=0; i-- )
                    array[i] = this.conv(array[i]);
                return this.group(array);
            },
            group : function(children) {
                return function() {
                    for(var i=0;i<children.length;i++)
                        children[i]();
                }
            }
        };

        // aliases
        //=============================================
        f.a  = f.reservedWord('abstract');
        f.as = f.reservedWord('assert');
        f.b  = f.primitiveType('boolean');
        f.br = f.reservedWord('break');
        f.by = f.primitiveType('byte');
        f.ca = f.reservedWord('case');
        f.ct = f.reservedWord('catch');
        f.ch = f.primitiveType('char');
        f.c  = f.reservedWord('class');
        f.cs = f.reservedWord('const');
        f.co = f.reservedWord('continue');
        f.de = f.reservedWord('default');
        f.d  = f.reservedWord('do');
        f.db = f.primitiveType('double');
        f.e  = f.reservedWord('else');
        f.en = f.reservedWord('enum');
        f.ex = f.reservedWord('extends');
        f.f  = f.reservedWord('final');
        f.fn = f.reservedWord('finally');
        f.fl = f.primitiveType('float');
        f.fo = f.reservedWord('for');
        f.i  = f.reservedWord('if');
        f.im = f.reservedWord('implements');
        f.ip = f.reservedWord('import');
        f.is = f.reservedWord('instanceof');
        f.j  = f.primitiveType('int');
        f.it = f.reservedWord('interface');
        f.l  = f.primitiveType('long');
        f.na = f.reservedWord('native');
        f.n  = f.reservedWord('new');
        f.pa = f.reservedWord('package');
        f.pi = f.reservedWord('private');
        f.po = f.reservedWord('protected');
        f.pu = f.reservedWord('public');
        f.r  = f.reservedWord('return');
        f.sh = f.primitiveType('short');
        f.s  = f.reservedWord('static');
        f.sf = f.reservedWord('strictfp');
        f.su = f.reservedWord('super');
        f.sw = f.reservedWord('switch');
        f.sy = f.reservedWord('synchronized');
        f.t  = f.reservedWord('this');
        f.tw = f.reservedWord('throw');
        f.ts = f.reservedWord('throws');
        f.tt = f.reservedWord('transient');
        f.tr = f.reservedWord('try');
        f.v  = f.primitiveType('void');
        f.vl = f.reservedWord('volatile');
        f.wh = f.reservedWord('while');
        f._  = f.w(1);

        return f;
    }-*/;

}
