/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, Utils, window*/
var mentor = {publisher: {}};

mentor.publisher.setURLParams = function (queryStr)
{
    var match,
        pl = /\+/g, // Regex for replacing addition symbol with a space
        search = /([^&=]+)=?([^&]*)/g,
        decode = function (s)
        {
            return decodeURIComponent(s.replace(pl, " "));
        },
        query = queryStr.substring(1);

    mentor.publisher.urlParams = {};
    while (match = search.exec(query)) {
        var paramVal = match[2];
        if (paramVal.indexOf('|') > -1 ) {
            paramVal = paramVal.split('|');
            paramVal = paramVal.map(param => decode(param));
        } else {
            paramVal = decode(paramVal);
        }
        mentor.publisher.urlParams[decode(match[1])] = paramVal;
    }
    /*
     if package name is specified in URL query param then store it in project properties
     */
    if (mentor.publisher.urlParams.package) {
        mentor.publisher.urlParams.project = mentor.publisher.urlParams.package;
    }

};

(window.onpopstate = function ()
{
    mentor.publisher.setURLParams(window.location.search);

})();

mentor.publisher.object = function (oldObject)
{
    "use strict";
    function F()
    {

    }

    F.prototype = oldObject;
    return new F();
};

mentor.publisher.stopEventFlow = function (event)
{
    "use strict";
    event = event || {};
    try {
        event.cancelBubble = true;
    }
    catch (e) {
        //JavaFX WebKit engine has this property as read only and throws error when modified
        //ignore the error
    }

    if (event.stopPropagation) {
        event.stopPropagation();
    }

    if (event.preventDefault) {
        event.preventDefault();
    }

};

function createOptionExpressions(options1, options2, operator)
{
    "use strict";
    var optionExpression = "";
    if (options1 || options2) {
        if (!options1) {
            optionExpression = options2;
        }
        else if (!options2) {
            optionExpression = options1;
        }
        else {
            optionExpression = "(" + options2 + ")" + " " + operator + " " + "(" + options1 + ")";
        }
    }
    return optionExpression;
}

function extend()
{
    "use strict";
    var arg, prop, child = {};
    for (arg = 0; arg < arguments.length; arg += 1) {
        for (prop in arguments[arg]) {
            if (arguments[arg].hasOwnProperty(prop)) {
                child[prop] = arguments[arg][prop];
            }
        }
    }
    return child;
}
var globalId = 0;
function createId()
{
    "use strict";
    globalId = globalId + 1;
    return globalId;
}

function isPDFJSSupported()
{
    var usePdfJs = mentor.publisher.config["use-pdfjs"] || "";
    if (usePdfJs && usePdfJs === 'true' && (window.Worker ||  window.location.href.indexOf("http") === 0)) {
        return true;
    }
    return false;
}

/*
 Custom data files can be pdfs,docs,pngs for which plugin is needed
 can be html, for which we need to follow the reports flow,
 can be svg also, for which we need to follow svg flow.
 */
var getPluginType = function (path, nullableOpts)
{
    "use strict";
    path = path || "";
    var orgPath = path;

    var opts = nullableOpts || {};
    opts.shouldIdentifyCapitalReports =
            !opts.hasOwnProperty("shouldIdentifyCapitalReports") || opts.shouldIdentifyCapitalReports;

    path = path.toLowerCase();
    if (path.indexOf('.pdf') !== -1) {

        return 'application/pdf';
    }
    else if (path.indexOf('.doc') !== -1 ||
        path.indexOf('.dot') !== -1 ||
        path.indexOf('.w6w') !== -1 ||
        path.indexOf('.word') !== -1) {
        return 'application/msword';
    }
    else if (path.indexOf('.xl') !== -1) {
        return 'application/excel';
    }
    else if (path.indexOf('.pot') !== -1 ||
        path.indexOf('.pps') !== -1 ||
        path.indexOf('.ppt') !== -1 ||
        path.indexOf('.ppz') !== -1) {
        return 'application/mspowerpoint';
    }
    else if (path.indexOf('.jpeg') !== -1 || path.indexOf('.jpe') !== -1 || path.indexOf('.jpg') !== -1 ||
        path.indexOf('.jfif') !== -1) {
        return 'image/pjpeg';
    }
    else if (path.indexOf('.png') !== -1) {
        return 'image/png';
    }
    else if (path.indexOf('.gif') !== -1) {
        return 'image/gif';
    }
    else if (path.indexOf('.htm') !== -1 || path.indexOf('.html') !== -1 || isURLHTTPLink(path)) {
        if (opts.shouldIdentifyCapitalReports && mentor.publisher.xmlLoader.isItaReport(orgPath)) {
            return mentor.publisher.contentType.CAPITAL_REPORT;
        }
        return 'text/html';
    }
    else if (path.indexOf("3dxml") !== -1) {
        /**
         * This will open any CATIA 3dxml in same way as packet generated 3d xmls are opened
         */
        return "3dxml";
    }
    else if (path.indexOf("svg") !== -1) {
        /**
         * this will make sure that external SVGs are opened similiarly as TwoDSVgs are opened
         */
        return "image/svg+xml";
    }
    else if (isJsonFile(path)) {
        return "application/json";
    }

    return '';
};

function isJsonFile(path)
{
    var safePath = path || "";
    return safePath.indexOf(".json") !== -1;
}

function isURLHTTPLink(url)
{
    "use strict";
    url = url || "";
    return (url).indexOf("http") !== -1 || (url).indexOf("www.") !== -1;
}


var callFunction = function (method)
{
    "use strict";
    return typeof method === "function" ? method() : "";
};
function is_touch_device()
{
    var isTouchSpported = 'ontouchstart' in window // works on most browsers
        || 'onmsgesturechange' in window; // works on ie10
    return isTouchSpported;
};

String.prototype.hashCode = function () {
    for (var ret = 0, i = 0, len = this.length; i < len; i++) {
        ret = ((ret << 7) - ret) + this.charCodeAt(i);
        ret = ret & ret;
    }
    return ret;
};


if (!String.prototype.endsWith) {
    String.prototype.endsWith = function(text, position) {
        // This works much better than >= because
        // it compensates for NaN:
        if (!(position < this.length))
            position = this.length;
        else
            position |= 0; // round position
        return this.substr(position - text.length, text.length) === text;
    };
}

/*
 Update string with args in place of placeholders({0}, {1} etc)
 */
String.prototype.format = function() {
    var args = arguments;

    return this.replace(/\{(\d+)\}/g, function(match, groupOne) {
        return args[groupOne];
    });
};