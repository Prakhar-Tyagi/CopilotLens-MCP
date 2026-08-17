var ICookieHandler = function () {
    this.createCookie = function (name, value, days) {
        throw new Error("Interface method should be implemented");
    };

    this.readCookie = function (name) {
        throw new Error("Interface method should be implemented");
    };

    this.deleteCookie = function (name) {
        throw new Error("Interface method should be implemented");
    };
};

var WebCookieHandler = function () {}
WebCookieHandler.prototype = new ICookieHandler();
WebCookieHandler.prototype.createCookie = function (name, value, days) {
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        var expires = "; expires=" + date.toGMTString();
    }
    else {
        var expires = "";
    }
    document.cookie =  name + "=" + value + expires + "; path=/";
};
WebCookieHandler.prototype.readCookie = function (name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1, c.length);
        }
        if (c.indexOf(nameEQ) == 0) {
            return c.substring(nameEQ.length, c.length);
        }
    }
    return null;
}
WebCookieHandler.prototype.deleteCookie = function (name) {
    document.cookie = name +'=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
}

window.cookieHandler = new WebCookieHandler();

