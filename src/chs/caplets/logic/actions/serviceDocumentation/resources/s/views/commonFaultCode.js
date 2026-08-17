/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */
/*global define, mentor*/
define([
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "fileDisplayHandler",
    "ListView",
    "collections/commonFaultCode"
], function ($, underscore, Backbone, selectedPackage, fileDisplayHandler, listview, commonFaultCode) {
    "use strict";
    var commonFaultCodeSection = listview(commonFaultCode).extend({
        title: "whats-in-common",
        cssClass: "faults",
        clicked: function (event) {
            var cid = $(event.currentTarget).attr('data-id'), name, content;

            fileDisplayHandler.display({
                id: cid,
                reset: true,
                type: mentor.publisher.contentType.TROUBLESHOOT,
                activeCodes: [],
                passiveCodes: [],
            });
        },

    });

    return new commonFaultCodeSection();
});
