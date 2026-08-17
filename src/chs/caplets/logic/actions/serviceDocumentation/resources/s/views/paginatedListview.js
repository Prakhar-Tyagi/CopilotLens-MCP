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
    "LocationViews",
    "fileDisplayHandler",
    "DesignObjectsView"
], function ($,
        underscore,
        Backbone,
        selectedPackage,
        locationViews,
        fileDisplayHandler,
        listView)
{
    "use strict";
    var ListView = listView(locationViews);

    var PaginatedListView = ListView.extend({
        title: "locationViews",
        cssClass: "locationViews",

        popOut: function (event)
        {
            var id = $(event.target).parent().attr('data-id'), locationView = locationViews.get(id);
            if (getPluginType(locationView.get("path")).indexOf("pdf") > 0) {
                mentor.publisher.popoutHandler.openPopout("popout.html#/customFile/" + locationView.get("mainText") +
                    "/" +
                    selectedPackage.get("id").replace("\\", "/") + "/" + locationView.get("path").replace("\\", "/"));
            }
            else {

                mentor.publisher.popoutHandler.openPopout("popout.html#/showLocation/" + locationView.get("mainText") +
                    "/" +
                    selectedPackage.get("id").replace("\\", "/"));
            }
            event.stopPropagation();
        },

        clicked: function (event)
        {
            var cid = $(event.currentTarget).attr('data-id'), content;
            content = locationViews.get(cid);
            if (getPluginType(content.get("path")).indexOf("pdf") > 0) {
                fileDisplayHandler.display({
                    id: content.id,
                    path: content.get("path"),
                    mainText: content.id,
                    type: mentor.publisher.contentType.CUSTOM_VIEW,
                    reset: true
                });
            }
            else if (content) {
                fileDisplayHandler.display({
                    id: content.id,
                    mainText: content.id,
                    type: mentor.publisher.contentType.LOCATION_VIEWS,
                    reset: true
                });
            }
        }
    });

    return new PaginatedListView();
});
