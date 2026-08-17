/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global Utils, mentor, document */
mentor.publisher.eventDispatcher = (function () {
    "use strict";
    var topics = {}, pubSub = {};

    pubSub.dispatchEvent = function (type, data) {
        var evt = document.createEvent("CustomEvent");
        evt.initCustomEvent(type, true, true, data);
        document.dispatchEvent(evt);
    };

    pubSub.attachEventListener = function (type, callbackMethod) {
        document.addEventListener(type, callbackMethod, false);
    };

    pubSub.removeEventListener = function (type, callbackMethod) {
        document.removeEventListener(type, callbackMethod, false);
    };
    return pubSub;
}());
