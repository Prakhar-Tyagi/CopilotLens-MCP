/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, $, Backbone, mentor, window, require, getWindowObj, getPluginType*/
define(["PopoverItemView", "TwoDLocationCollection", "fileDisplayHandler", "currentPackage"],
        function (PopoverItemView, twoDLocationCollection, fileDisplayHandler, currentPackage)
        {
            "use strict";

            var TwoDLocationItem = PopoverItemView.extend({
                getData: function ()
                {
                    return twoDLocationCollection;
                },
                getTitle: function ()
                {
                    return "TwoDLocationViewTitle";
                },
                getClassName: function ()
                {
                    return "2dLocations";
                },
                events: {
                    "click .2dLocations>.listItem": "popoverItemClicked",
                    "click .2dLocations>.listItem>.popUp": "popOut"
                },

                displayContent: function (content)
                {
                    if (getPluginType(content.path).indexOf("pdf") > 0) {
                        this.getWindowObj().mentor.publisher.fileDisplayHandler.display({
                            id: content.id,
                            path: content.path,
                            mainText: content.id,
                            type: mentor.publisher.contentType.CUSTOM_VIEW,
                            reset: false
                        });
                    }
                    else {

                        TwoDLocationItem.__super__.displayContent.apply(this, arguments);
                    }
                    require(["models/selectedSystem"], function (selectedSystem)
                    {
                        selectedSystem.trigger("scrollNavigationPanelToTheSelectedElement");
                    });
                },

                createURL: function (content)
                {

                    if (content.get("path") && getPluginType(content.get("path")).indexOf("pdf") > 0) {
                        return "popout.html#/customFile/" + content.get("mainText") +
                                "/" +
                                currentPackage.get("id").replace("\\", "/") + "/" +
                                content.get("path").replace("\\", "/");
                    }
                    return mentor.publisher.popoutHandler.createURL({
                        mainText: content.get("mainText"),
                        projectId: currentPackage.get("id").replace("\\", "/"),
                        objectId: content.get("objectId"),
                        type: mentor.publisher.contentType.LOCATION_VIEWS
                    });
                },

                getItemContent: function (itemId)
                {
                    var content = this.getData().get(itemId);
                    this.getWindowObj().mentor.publisher.selectedSystem.set("objectId", content.get("objectId"),
                            {silent: true});
                    //id is not needed in case of two d location view
                    return {
                        id: content.id, mainText: content.id,
                        type: mentor.publisher.contentType.LOCATION_VIEWS, reset: false,
                        path: content.path || content.get("path") || ""
                    };
                },

                filter: function (items)
                {
                    return items;
                }
            });
            return new TwoDLocationItem();
        });
