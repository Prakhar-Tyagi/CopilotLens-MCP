/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, $, mentor*/
define(["PopoverItemView", "ThreeDViewCollection", "fileDisplayHandler", "currentPackage"],
    function (PopoverItemView, threeDViewCollection, fileDisplayHandler, currentPackage) {
        "use strict";
        var ThreeDViewItem = PopoverItemView.extend({
            getData : function () {
                return threeDViewCollection;
            },
            events : {
                "mouseover .threeDLocations>.listItem" : "showToolTip",
                "mouseout .threeDLocations>.listItem" : "removeToolTip",
                "click .threeDLocations>.listItem" : "popoverItemClicked",
                "click  .threeDLocations>.listItem>.popUp" : "popOut"
            },
            createURL: function (content) {
                var objectId = content.get("objectId") || "";
                var modified = objectId.replace(/\//g, "___");
                return "popout.html#/threeDXML/" + content.get("mainText") + "/" +
                        currentPackage.get("id").replace("\\", "/") + "/" +
                        content.get("type") + "/" +
                        modified + "/" +
                        content.get("path").replace("\\", "/");
            },

            getItemContent : function (itemId) {
                var content = this.getData().get(itemId);
                content.type = content.get("type");
                return content;
            },
            getTitle : function () {
                return "LocationViewTitle";
            },
            getClassName : function () {
                return "threeDLocations";
            }
        });
        return new ThreeDViewItem();
    });
