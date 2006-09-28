
// the prototype-oriented programming library
function object(o) {
  function F() {}
  F.prototype = o;
  return new F();
}
// create a new object by using the given prototype then copying over
// all properties from v.
function derive(proto,v) {
  o = object(proto);
  for(p in v)
    o[p] = v[p];
  return o;
}

// extension to built-in objects
Array.prototype.forEach = function(closure) {
  for(var i=0; i<this.length; i++)
    closure(this[i]);
}

String.prototype.startsWith = function(rhs) {
  var len = rhs.length;
  if(this.length<len)  return false;
  return this.substr(0,len)==rhs;
}




// class to fade a tag
function Fader(e,r,g,b) {
  self = this;
  this.grad = 0;
  this.steps = 20;

  this.trigger = function() {
    window.setTimeout(this.away,25);
  }

  this.set = function() {
    function ratio(i) {
      return (255*self.grad+i*(self.steps-self.grad))/self.steps;
    }
    e.style.backgroundColor = "rgb("+ratio(r)+","+ratio(g)+","+ratio(b)+")";
  }

  this.away = function() {
    self.set();
    if(++self.grad <= self.steps) {
      self.trigger();
    }
  }

  this.set();
  window.setTimeout(this.away,1000);
}


// manages loading of scripts and avoids duplicates.
var scriptLoadManager = {
  completedLoads : {},
  pendingLoads : {},

//
// overridable methods
//
  // compute href to .js from the key name.
  getScriptName : function(key) {return key;},

  // perform the post processing of the model objects
  postProcess : function(key,model) {},

  /*
    loads the given script.

    when the loading is complete, "callback(model)" is invoked
    where 'model' is what's given to 'loadComplete'.
  */
  load : function(key,callback) {
    var model = this.completedLoads[key];
    if(model!=null) {
      callback(model);
    } else {
      var inject=false;
      var p = this.pendingLoads[key];
      if(p==null) {
        p = [];
        inject = true;
        this.pendingLoads[key] = p;
      }
      p.push(callback);
      if(inject) {
        loadScript(this.getScriptName(key));
      }
    }
  },

  // to be invoked from scripts that are loaded.
  loadComplete : function(key,model) {
    this.postProcess(key,model);

    this.completedLoads[key] = model;

    var p = this.pendingLoads[key];
    this.pendingLoads[key]=null;

    // fire callbacks
    if(p!=null) {// this should be always true, but just to be defensive
      for( var i=0; i<p.length; i++ )
        p[i](model);
    }
  }
}
