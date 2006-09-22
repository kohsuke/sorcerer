// add script tag to DOM to load it
function loadScript(href) {
    head = document.getElementsByTagName("head")[0];
    script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = href;
    head.appendChild(script);
}


// toggle button class for filter
//   e : the <img> element to be used as a button
//   callback : invoked when an image is clicked
function ToggleButton(e,callback) {
  e.isPressed = function() {
    return this.onclick==this.release;
  }
  e.postClick = function() {
    this.onmouseover(); // update the tooltip
    if(callback!=null)
      callback(this);
  }
  e.press = function() {
    this.className="button pressed";
    this.onclick=this.release;
    this.postClick();
  }
  e.release = function() {
    this.className="button";
    this.onclick=this.press;
    this.postClick();
  }
  e.onclick = e.press;
  e.onmouseover = function() {
    tip.innerHTML = this.getAttribute(this.isPressed()?"tip2":"tip1");
  }
  e.onmouseout = function() {
    tip.innerHTML = "";
  }
}

function RadioButton(e,group,callback) {
  group.push(e);

  e.isPressed = function() {
    return this.className="button pressed"
  }
  e.onclick = function() {
    this.className="button pressed";
    for(var i=0; i<group.length; i++ ) {
      if(group[i]!=this)
        group[i].className="button";
    }
    if(callback!=null)
      callback(this);
  }
  e.onmouseover = function() {
    tip.innerHTML = this.getAttribute("tip");
  }
  e.onmouseout = function() {
    tip.innerHTML = "";
  }
}
