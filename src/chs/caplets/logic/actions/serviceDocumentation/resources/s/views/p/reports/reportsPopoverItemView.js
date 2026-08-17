/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, $, mentor, Backbone*/
define(["PopoverItemView", "ReportsCollection", "currentPackage", "fileDisplayHandler"],
    function (PopoverItemView, reports, selectedPackage, fileDisplayHandler) {
        "use strict";
        var ReportsPopoverItem = PopoverItemView.extend({
            getData : function () {
                return reports;
            },
            getTitle : function () {
                return "";
            },
            isExpanded : function () {
                return true;
            },
            getClassName : function () {
                return "reports";
            },
            events : {
                "click .reports>.listItem" : "popoverItemClicked",
                "click .reports>.listItem>.popUp" : "popOut"
            },

            createURL : function (content) {
                return "popout.html#/report/" + content.get("systemId") + "/" +
                    content.id + "/" +
                    selectedPackage.get("id").replace("\\", "/");
            },

            getItemContent : function (itemId) {
                return reports.get(itemId);
            },

            displayContent : function (clickedReport) {
                var content;
                if (clickedReport) {
                    content = {

                        /*id : clickedReport.get('systemId'),*/
                        reportId : clickedReport.id,
                        systemId : clickedReport.get('systemId'),
                        reset : false,
                        type : mentor.publisher.contentType.SYSTEM_REPORT
                    };
                    fileDisplayHandler.display(content);
                }

            }
        });
        return new ReportsPopoverItem();
    });
