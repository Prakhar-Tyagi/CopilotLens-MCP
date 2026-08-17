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
    "ListView",
    "GlobalReports"
], function ($, underscore, Backbone, selectedPackage, fileDisplayHandler, listview, globalReports)
{
    "use strict";

    var GlobalReports = listview(globalReports).extend({
        title:"globalreports",
        cssClass:"globalReports",

        popOut:function (event)
        {
            var id = $(event.target).parent().attr('data-id'), globalreport = globalReports.get(id), p = mentor.publisher;
			p.popoutHandler.openPopout(p.popoutHandler.createURL({
                type: p.contentType.CAPITAL_REPORT,
                mainText: globalreport.get("mainText"),
                path: globalreport.get("path"),
                projectId: selectedPackage.get("id"),
			}));
            event.stopPropagation();
        },

        clicked:function (event)
        {
            var cid = $(event.currentTarget).attr('data-id'), content, name;
            content = globalReports.get(cid);
            if (content) {
                content.type = mentor.publisher.contentType.CAPITAL_REPORT;
                content.reset = true;
                content.path = content.get("path");
                content.mainText = content.get("mainText");
                fileDisplayHandler.display(content);
            }
        }

    });

    return new GlobalReports();
});
