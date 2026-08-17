/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define([
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "fileDisplayHandler",
    "FaultCodes",
    "ListView"
], function ($, underscore, Backbone, selectedPackage, fileDisplayHandler, faultCodes, listview) {
    "use strict";
    var FaultCodeSection = listview(faultCodes).extend({
        title : "faultcode",
        cssClass : "FaultCodes",

        clicked : function (event) {
            var cid = $(event.currentTarget).attr('data-id'), name, content;
            content = faultCodes.get(cid);
            if (content) {
                fileDisplayHandler.display({id : content.get("id"), reset : true, type : mentor.publisher.contentType.FAULT_CODE});
            }
        }

    });

    return new FaultCodeSection();
});
