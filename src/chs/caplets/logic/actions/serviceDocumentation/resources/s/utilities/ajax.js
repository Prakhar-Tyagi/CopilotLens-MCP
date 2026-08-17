function ajax(url, callback, async)
{
    var response = '';
    if (!url) {
        return;
    }
    var httpRequest;
    if (XMLHttpRequest) {
        httpRequest = new XMLHttpRequest();
    }
    else if (ActiveXObject) {
        httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
    }
    if (!httpRequest) {
        console.error('XMLHttpRequest not supported');
        callback && callback("[]");
        return response;
    }
    httpRequest.onreadystatechange = function ()
    {
        if (httpRequest.readyState === XMLHttpRequest.DONE) {
            handlers.loaded = true;
            if (httpRequest.status === 200) {
                response = this.responseText;
                callback && callback(this.responseText);
            }
            else {
                callback && callback("[]");
                logMessage(this);
            }
        }
    };
    url = url.replace("\\", "/");
    httpRequest.open('GET', "../" + url, async);
    httpRequest.send(null);
    return response;
}