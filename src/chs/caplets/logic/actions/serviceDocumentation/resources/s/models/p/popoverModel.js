/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("PopoverModel", ["backbone", "PopoverFilterModel"], function (Backbone, popoverFilterModel)
{
    "use strict";
    var p = mentor.publisher;
    var PopoverModel = Backbone.Model.extend({
        _internalModel: {},
        filterModel: function ()
        {
            this.loadCollections(this._internalModel);
        },
        loadPopoverData: function (evt)
        {
            var xLoc, yLoc, data;
            data = data || evt.detail;
            xLoc = data.x || evt.clientX;
            yLoc = data.y || evt.clientY;
            this.load(xLoc, yLoc, data);
        },
        load: function (x, y, data)
        {
            var that = this;
            this._internalModel = {};
            if (!this.isValidEvent(data)) {
                return;
            }
            popoverFilterModel.set("popoverModel", this);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});

            var model = this.loadData(data);

            if (!model) {
                return;
            }
            var isActive = (p.filter.applyFilter([model]) || []).length === 1;
            if (!isActive) {
                return;
            }
            this.set("x", x);
            this.set("y", y);
            this.set("popoverModel", model);
            //this.set("loaded", model);
            this.set("loadSkeleton", !this.get('loadSkeleton'));
            /**
             * wait for skeleton popover to load
             */
            setTimeout(function ()
            {
                that.loadCollections(model);
                that._internalModel = model;
                if (data.callBack) {
                    data.callBack();
                }

            }, 100);
        },
        loadCollections: function (model)
        {
            //this will be over ridden in the implementations
        },
        loadData: function (data)
        {
            //this will be over ridden in the implementations
            return data;
        },
        isValidEvent: function (data)
        {
            //to be over ridden if one deems necessary
            return true;
        }
    }), popoverModel;
    popoverModel = new PopoverModel();
    //return _.extend(popoverModel, Backbone.Events);
    return PopoverModel;
});

