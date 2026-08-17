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
    "currentPackage",
    "views/component/PaginatedListView"
], function ($, selectedPackage, PaginatedListView)
{
    "use strict";
    var p = mentor.publisher;
    return function (objects)
    {
        var RelatedDataSectionGrpView = PaginatedListView(objects).extend({
            title: "DesignObjs",
            delegate: this,
            isViewDataAvailable: function ()
            {
                return true;
            },

            clicked: function (event)
            {
                if (this.isValidEvent(event)) {
                    var targetObjectId = $(event.currentTarget).attr("data-id");
                    var targetCoordinates = {x: event.clientX, y: event.clientY};
                    this.highlightObject(targetObjectId, targetCoordinates);
                }
                event.stopPropagation();
            },
            isValidEvent: function (event)
            {
                //when next... or previous... click, it will paginate and will not show any popover
                //so ignore these clicks for popover
                return !$(event.target).hasClass("next") && !$(event.target).hasClass("previous");
            },
            highlightObject: function (cid)
            {
                window.isSVGClick = true;
                try {
                    var selectedObj;
                    selectedObj = this.getSelectedObject(cid);
                    if (selectedObj) {
                        mentor.publisher.eventDispatcher.dispatchEvent(
                                mentor.publisher.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS,
                                selectedObj.attributes);
                        require(["models/selectedSystem"], function (selectedSystem) {
                            var objectData = mentor.publisher.objectDataLoader.load(selectedSystem.get("systemId"),
                                    selectedObj.get("objectId"), mentor.publisher.project.getId()) || {};
                            objectData.get3DViews();
                        });
                    }
                }
                finally {
                    setTimeout(function ()
                    {
                        window.isSVGClick = false;
                    }, 2000);
                }
            },
            getSelectedObject: function (id)
            {
                return this.getData().get(id);
            },
            clearView: function ()
            {
                this.undelegateEvents()
                this.$el.html('');
            }
        });
        return new RelatedDataSectionGrpView();
    };
});

