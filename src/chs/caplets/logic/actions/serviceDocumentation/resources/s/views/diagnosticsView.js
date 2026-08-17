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
    'backbone',
    "currentPackage",
    "fileDisplayHandler",
    "Diagnostics",
    "ListView"
], function ($, Backbone, selectedPackage, fileDisplayHandler, diagnostics, ListView) {
    "use strict";

    var DiagnosticsView = ListView(diagnostics).extend({
        title: "diagnostics",
        cssClass: "FaultCodes",

        clicked: function (event) {
            var cid = $(event.currentTarget).attr('data-id');

            var diagnostic = diagnostics.get(cid);
            if (diagnostic) {
                fileDisplayHandler.display({
                    id: diagnostic.get("id"),
                    reset: true,
                    type: mentor.publisher.contentType.DIAGNOSTIC
                });
            }
        },

        popOut: function (event) {

            var id = $(event.target).parent().attr('data-id'), selectedElement, content, system;
            event.stopPropagation();

            var diagnostic = diagnostics.get(id);
            if (!diagnostic) {
                return;
            }

            var packageTitle = encodeURIComponent(selectedPackage.get("title"));
            var diagnosticTitle = encodeURIComponent(diagnostic.get("nameAttr"));
            var diagnosticURL = "popout.html?project=" + packageTitle + "&view=diagnostic&viewName=" + diagnosticTitle;
            var effSetter = require("filehandlers/effectivitySetter");
            mentor.publisher.popoutHandler.openPopout(
                    effSetter.addEffAndProjectIdInURLs(diagnosticURL)
            );
        },

    });

    return new DiagnosticsView();
});
