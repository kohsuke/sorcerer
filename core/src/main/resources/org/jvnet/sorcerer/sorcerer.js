
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

// controls time out action and cancellation
function Future(action,timeout) {
  this.schedule=function() {
    if(this.token!=null)
      window.clearTimeout(this.token);
    this.token=window.setTimeout(function() {
      this.token=null;
      action();
    },timeout);
  }
  this.cancel=function() {
    if(this.token!=null)
      window.clearTimeout(this.token);
    this.token=null;
  }
}



var menu;
var menuSelector;

window.onload = function() {
  // Create the menu
  menu = new YAHOO.widget.Menu("contextmenu");
  menuSelector = document.getElementById('menuSelector');

  var menuItems = [
      {
        text: "Go to declaration",
        action: function() {
          window.location=menu.target.href;
        }
      },
      {
        text: "Find usages",
        action: function() {
          parent.searchpane.displayController.show(menu.target);
        }
      }
    ];

  // Add items to the main menu
  for(var i=0; i<menuItems.length; i++) {
      menuItem =
          new YAHOO.widget.MenuItem(menuItems[i].text);
      menuItem.clickEvent.subscribe(menuItems[i].action);
      menu.addItem(menuItem);
  }


  menu.render(document.body);


  // for hiding menu after a timeout
  var canceller = new Future(function() {
    menu.hide();
  },750);

  menu.mouseOverEvent.subscribe(function(){canceller.cancel();});
  menu.mouseOutEvent.subscribe(function(){canceller.schedule();});

  function showMenu(target) {
    menu.cfg.setProperty("context", [target, "tl", "tr"]);
    menu.getItem(0).cfg.setProperty("url",target.href);
    menu.show();
    menu.target=target;
    return false;
  }

  document.onclick=function(e) {
    if(YAHOO.util.Event.getTarget(e,false)!=menuSelector)
      menu.hide();
  }

  // menu selector control
  var menuSelectorCanceller = new Future(function() {
    menuSelector.style.visibility = "hidden";
  },750);
  menuSelector.onmouseover=function() {
    menuSelectorCanceller.cancel();
  };
  menuSelector.onmouseout=function() {
    menuSelectorCanceller.schedule();
  };
  menuSelector.onclick=function() {
    showMenu(this.target);
    menuSelector.style.visibility="hidden";
  };

  // var links = YAHOO.util.Dom.getElementsByClassName("link",null,document.body);
  var links = document.getElementsByTagName("span");
  for( var i=links.length-1; i>=0; i-- ) {
    var e = links[i];
    if(e.getAttribute("u")==null) continue;
    e.onmouseover=function() {
      var xy = YAHOO.util.Dom.getXY(this);
      xy[0] += this.offsetWidth;
      YAHOO.util.Dom.setXY(menuSelector,xy);
      menuSelector.style.visibility = "visible";
      menuSelector.target=this.parentNode; // must be 'a' tag
    }
    e.onmouseout=function() {
      menuSelectorCanceller.schedule();
    }
  }
}




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

