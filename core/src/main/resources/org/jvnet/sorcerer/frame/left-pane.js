// add script tag to DOM to load it
function loadScript(href) {
    head = document.getElementsByTagName("head")[0];
    script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = href;
    head.appendChild(script);
}
