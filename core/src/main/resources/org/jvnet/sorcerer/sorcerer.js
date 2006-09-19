
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


// behavior rules

Behaviour.register({
/*  "#lineNumberTable a" : function(e) {
    e.onclick = function() {
      this.style.backgroundColor = "#0000ff";
      new Fader(this,255,0,0);
      return false; // cancel
    }
  },*/

  ".popup" : function(e) {
    e.parentNode.popup = e;
  },

  ".bookmark" : function(e) {
    e.onmouseover = function() {
      this.popup.style.display="block";
    };
    e.onmouseout  = function() {
      this.popup.style.display="none";
    };
  }

});

