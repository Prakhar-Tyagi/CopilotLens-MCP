/*
 * Copyright 2014 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

/*global define, mentor*/
define([
    'jquery',
    'backbone',
    "currentPackage",
    "fileDisplayHandler",
    "Diagnostics",
    "baseTreeView"
], function ($, Backbone, selectedPackage, fileDisplayHandler, diagnostics, baseTreeView) {
    "use strict";

    var DiagnosticsView = baseTreeView(diagnostics).extend({
        title: "diagnostics",
        cssClass: "FaultCodes",

        getDesingfolders: function (diagnostic) {
            var root = Utils.translatePlainText("Diagnostics");
            var path = diagnostic.get("folder");
            return path ? [root + Utils.getDiagnosticFolderDelimiter() + path] : [root];
        },

        getFolderDelimiter: function () {
            return Utils.getDiagnosticFolderDelimiter();
        },

        showFolderCount: true,
        showFoldersFirst: true,

        getModelIdString: function () {
            return "id";
        },

        getFileLabel: function (diagnostic) {
            return diagnostic.get("mainText");
        },

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

            var encodedProject = encodeURIComponent(selectedPackage.get("title"));
            var encodedName = encodeURIComponent(diagnostic.get("nameAttr"));
            var url = "popout.html?project=" + encodedProject + "&view=diagnostic&viewName=" + encodedName;
            var effSetter = require("filehandlers/effectivitySetter");
            mentor.publisher.popoutHandler.openPopout(effSetter.addEffAndProjectIdInURLs(url));
        },

        mouseout: function (event) {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP,
                    event);
        }
    });

    return new DiagnosticsView();
});
