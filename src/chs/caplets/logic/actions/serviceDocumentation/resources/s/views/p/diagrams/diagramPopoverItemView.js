/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, $, mentor, Backbone*/
define(["PopoverItemView", "DiagramsCollection", "currentPackage", "fileDisplayHandler"],
    function (PopoverItemView, diagrams, selectedPackage, fileDisplayHandler) {
        "use strict";
        var PackagesPopoverItem = PopoverItemView.extend({
            getData : function () {
                return diagrams;
            },
            getTitle : function () {
                return "";
            },
            getClassName : function () {
                return "diagrams";
            },
            isExpanded : function () {
                return true;
            },
            events : {
                "click .diagrams>.listItem" : "popoverItemClicked",
                "click .diagrams>.listItem>.popUp" : "popOut"
            },
            /*    popOut : function (event) {
             var cid = this.getDataId(event), content;
             content = diagrams.get(cid);
             mentor.publisher.popoutHandler.openPopout("popout.html#/system/" + content.get("systemId") + "/" +
             content.get("diagramId") + "/" +
             selectedPackage.get("id").replace("\\", "/"));
             event.stopPropagation();
             },*/
            createURL : function (content) {
                return "popout.html#/system/" + content.get("systemId") + "/" +
                    content.get("diagramId") + "/" +
                    selectedPackage.get("id").replace("\\", "/");
            },

            getItemContent : function (itemId) {
                return diagrams.get(itemId);
            },

            updateURL : function (clickedSystem) {
               /* if (!Utils.is_mozilla()) {
                    Backbone.history.navigate("system/" + clickedSystem.get("systemId") + "/" +
                        clickedSystem.get("diagramId") + "/" +
                        selectedPackage.get("id").replace("\\", "/"),
                        {trigger : false});
                }*/
            },

            displayContent : function (clickedSystem) {
                var content;
                if (clickedSystem) {
                    this.updateURL(clickedSystem);
                    /*                    if (!Utils.is_mozilla()) {
                     Backbone.history.navigate("system/" + clickedSystem.get("systemId") + "/" +
                     clickedSystem.get("diagramId") + "/" +
                     selectedPackage.get("id").replace("\\", "/"),
                     {trigger : false});
                     }*/
                    content = {
                        id : clickedSystem.get("systemId"),
                        diagramId : clickedSystem.get("diagramId"),
                        reset : false,
                        type : mentor.publisher.contentType.SYSTEM_SVG
                    };
                    fileDisplayHandler.display(content);
                }
            }
            /*
             showDiagrams : function (event) {
             var cid = $(event.currentTarget).attr('data-id'), clickedSystem, content;
             clickedSystem = diagrams.get(cid);
             if (clickedSystem) {
             if (!Utils.is_mozilla()) {
             Backbone.history.navigate("system/" + clickedSystem.get("systemId") + "/" +
             clickedSystem.get("diagramId") + "/" +
             selectedPackage.get("id").replace("\\", "/"),
             {trigger : false});
             }
             content = {
             id : clickedSystem.get("systemId"),
             diagramId : clickedSystem.get("diagramId"),
             reset : false,
             type : mentor.publisher.contentType.SYSTEM_SVG
             };
             fileDisplayHandler.display(content);
             }
             }*/
        });
        return new PackagesPopoverItem();
    });
