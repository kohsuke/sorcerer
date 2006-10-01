// script used in the source-view.html, but loaded in another frame to reduce the start-up time

var sourceView = {}; // the module definition


// HTML generator
sourceView.builder = makeFull(derive(abstractBuilder,{
  reservedWord : function(name) {
    return function(context) {
      context.css('rw').appendText(name).pop();
    }
  },
  primitiveType : function(name) {
    return function(context) {
      context.css('pr').appendText(name).pop();
    }
  },
  sourceText : function(text) {
    return function(context) {
      context.appendText(text);
    }
  },

  // comment
  O : function(/*...*/) {
    // determine CSS class
    var head = arguments[0];
    var style;
    if(head.startsWith("//"))
      style = "cs";
    else
    if(head.startsWith("/**"))
      style = "cj";
    else
      style = "cm";

    var children = this.$$(arguments);

    return function(context) {
      context.css(style);
      children(context);
      context.pop();
    }
  },

  // whitespace
  w : function(n) {
    return function(context) {
      context.appendText(makeWhitespace(n));
    }
  },

  nl : function(context) {
    context.appendText('\n');
    context.lineNumber++;
  },

  C : function(typeTableIndex,descendants,children) { // class
    return function(context) {
      if(descendants.length>0)
        context.addBookmark(bookmark.makeSubtype(context.me.types,descendants));

      var old = context.currentDecl;
      context.currentDecl = context.me.types[typeTableIndex];
      children(context);
      context.currentDecl = old;
    }
  },

  I : function(text) { // identifier in method/class declaration
    var f = function(context) {
      var d = context.currentDecl;
      context.declLink(d.href);
      context.css(d.css+" d",d.usage())
          .appendText(d.displayText()).pop().pop();
    }
    f.identifier = text;
    return f;
  },

  B : function(/*...*/) { // curly brackets {...}
    return this.parenthesis('{','}',this.$$(arguments));
  },

  P : function(/*...*/) { // parenthesis (...)
    return this.parenthesis('(',')',this.$$(arguments));
  },

  parenthesis : function(L,R,children) {
    return function(context) { // TODO: scroll to the other bracket by clicking
      var lid=idGen();
      var rid=idGen();

      context.html.push("<span id=:"+lid+">"+L+"</span>");
      children(context);
      context.html.push("<span id=:"+rid+">"+R+"</span>");

      lazyInitManager.add(function() {
        var highlighter = function(lhs,rhs) {
          lhs.onmouseover = function() {
            lhs.className="highlighted-bracket";
            rhs.className="highlighted-bracket";
          }
          lhs.onmouseout = function() {
            lhs.className="";
            rhs.className="";
          }
        };
        var open = sourceView.document.getElementById(":"+lid);
        var closed = sourceView.document.getElementById(":"+rid);
        if(open==null)
          YAHOO.log(lid+" not found");
        else {
          highlighter(open,closed);
          highlighter(closed,open);
        }
      });
    }
  },

  M : function(methodTableIndex,sup,sub,children) { // method declaration
    return function(context) {
      if(sup.length>0)
        context.addBookmark(bookmark.makeSuperMethods(context.me.methods,sup));
      if(sub.length>0)
        context.addBookmark(bookmark.makeSubMethods(  context.me.methods,sub));

      var old = context.currentDecl;
      context.currentDecl = context.me.methods[methodTableIndex];
      children(context);
      context.currentDecl = old;
    }
  },

  N : function(n) { // method reference
    return function(context) {
      var m = context.me.methods[n];
      context.link(m.href).css(m.css+" r",m.usage()).appendText(m.name).pop().pop();
    }
  },


  T : function(n) { // type reference
    return function(context) {
      var t = context.me.types[n];
      context.link(t.href).css(t.css+" r",t.usage()).appendText(t.displayText()).pop().pop();
    }
  },

  F : function(/*arguments*/) {// field declaration
    var argList = arguments;
    var children = this.$$(arguments);
    return function(context) {
      var old = context.currentDecl;
      context.currentDecl = makeFieldEntry(context.currentDecl,argList);
      children(context);
      context.currentDecl = old;
    }
  },

  G : function(typeIdx,mods,name) {// field reference
    return function(context) {
      var t = context.me.types[typeIdx];
      context.link(t.linker.field(t,name)).css(mods+" r",t.fullName+'#'+name).appendText(name).pop().pop();
    }
  },

  V : function(n,children) {// variable declaration
    return function(context) {
      var old = context.currentDecl;
      context.currentDecl = context.me.localVariables[n];
      children(context);
      context.currentDecl = old;
    }
  },

  W : function(n) { // variable reference
    return function(context) {
      // TODO: reuse this similarity between other decls
      var v = context.me.localVariables[n];
      context.link(v.href).css(v.css+" r",v.usage()).appendText(v.name).pop().pop();
    }
  },

  L : function(str) { // literal
    return function(context) {
      context.css("lt").appendText(str).pop();
    }
  }
}));


sourceView.defineStructure = function(className,ast) {
  YAHOO.log("parsing AST: "+className);

  lazyInitManager.reset();

  // use a copy of builder (this will receive symbol tables)
  var b=object(sourceView.builder);
  var sourceBuilder = ast(b);

  // the context object to be passed around the dom builder functions
  var context = {
    me : b,             // to access the builder
    currentDecl : null, // when we are building inside a method/class declaration,
                        // this field points to that declaration
    lineNumber : 1,     // current line
    bookmarks: {},      // bookmark map. Map<lineNumber,Array<Bookmark>>

    html: [], // HTML fragments to be added as innerHTML
    tags: [], // end tags to be inserted to .html when the tag closes

    appendText : function(text) {
      this.html.push(text);
      return this;
    },
    pop : function() {
      this.html.push(this.tags.pop());
      return this;
    },
    // wrap by a CSS class
    //   usageKey : optional. usage search key like "java.lang.String#indexOf()"
    css : function(style,usageKey) {
      var frag = "<span class='"+style;
      if(usageKey!=null)
        frag+="' u='"+usageKey+"'>";
      else
        frag+="'>";
      this.html.push(frag);
      this.tags.push("</span>");
      return this;
    },
    // wrap by anchor link
    link : function(href) {
      if(href!=null) {
        this.html.push("<a href='"+href+"'>");
        this.tags.push("</a>");
      }
      return this;
    },
    // declaration and link at the same time
    declLink : function(href) {
      if(href!=null) {
        this.html.push("<a href='"+href+"' id='"+href.after("#")+"'>");
        this.tags.push("</a>");
      }
      return this;
    },
    addBookmark : function(b) {
      var a = this.bookmarks[this.lineNumber];
      if(!a) {
        a = [];
        this.bookmarks[this.lineNumber] = a;
      }
      a.push(b);
    }
  };
  // build the main source code HTML
  YAHOO.log("generating HTML: "+className);
  sourceBuilder(context);

  YAHOO.log("inserting HTML: "+className);
  var pre = sourceView.document.getElementById("main");
  pre.innerHTML = context.html.join("");

  // build the line number table
  function pad(s,n) {
    while(s.length<n)
      s=' '+s;
    return s;
  }

  YAHOO.log("generating line number table");
  var lnt = [];
  for(var i=1;i<=context.lineNumber;i++) {
    var lineText = pad(String(i),4);

    var lineBookmarks = context.bookmarks[i];
    if(lineBookmarks) {
      lineBookmarks.forEach(function(b) {
        sourceView.window.bookmarks[b.id] = b;
        lnt.push(b.buildAnchor());
      });
      lineText = lineText.substring(lineBookmarks.length*2);
    }

    lnt.push("<a name="+i+" href=#"+i+">");
    lnt.push(lineText);
    lnt.push("\n</a>");
  }
  sourceView.document.getElementById("lineNumberTable").innerHTML = lnt.join("");

  // page rendering complete at this point. jump to the anchor
  YAHOO.log("Checking hyperlink");
  var hash = sourceView.window.location.hash;
  if(hash!="") {
    hash=hash.substring(1);
    var e = sourceView.document.getElementById(hash);
    var y = YAHOO.util.Dom.getY(e);
    YAHOO.log("Scrolling to "+hash+" "+e+" "+y);
    sourceView.window.scroll(0,y);
  }

  // render outline
  sourceView.window.top.outline.main.load(ast);

  YAHOO.log("complete");

  lazyInitManager.start();
}

// invoked from AST JavaScript when this is a redirect to another script
sourceView.redirect = function(localTypeName,targetScript) {
  // TODO: this is somewhat unreliable.
  var loc = String(sourceView.window.location.search);
  var idx = loc.lastIndexOf('/');
  if(idx>0) loc=loc.substring(0,idx+1)+targetScript;
  else      loc="?"+targetScript;

  loc += "#~"+localTypeName;

  if(sourceView.window.location.hash!=null) {
    loc += "-"+sourceView.window.location.hash;
  }
  sourceView.window.location.href=loc;
}

// load AST
sourceView.load = function(name) {
  YAHOO.log("Loading "+name);
  loadScript(name);
}




// controls time out action and cancellation
//   action:
sourceView.makeFuture = function(action,timeout) {
  return {
    schedule: function() {
      if(this.token!=null)
        sourceView.window.clearTimeout(this.token);
      this.token=sourceView.window.setTimeout(function() {
        this.token=null;
        action();
      },timeout);
    },
    cancel: function() {
      if(this.token!=null)
        sourceView.window.clearTimeout(this.token);
      this.token=null;
    }
  };
}

YAHOO.log = function(sMsg, sCategory, sSource) {
  sourceView.window.YAHOO.log(sMsg,sCategory,sSource);
}