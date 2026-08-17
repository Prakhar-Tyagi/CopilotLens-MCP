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
    "Harnesses",
    "fileDisplayHandler",
    "ListView"
], function ($, underscore, Backbone, selectedPackage, harnesses, fileDisplayHandler, listview) {
    "use strict";
    var HarnessSection = listview(harnesses).extend({
        title : "harness",
        cssClass : "Harnesses",

        popOut : function (event) {
            var p = mentor.publisher;
            var id = $(event.target).parent().attr('data-id'), harness = harnesses.get(id);
            var url = p.popoutHandler.createURL({
                type: p.contentType.HARNESS,
                mainText: harness.get("mainText"),
                projectId: selectedPackage.get("id")
            })
            p.popoutHandler.openPopout(url);
            event.stopPropagation();
        },

        clicked : function (event) {
            var cid = $(event.currentTarget).attr('data-id'), content, name;
            content = harnesses.get(cid);
            if (content) {
                content.type = mentor.publisher.contentType.HARNESS;
                content.reset = true;
                fileDisplayHandler.display(content);
            }
        }

    });

    return new HarnessSection();
});
