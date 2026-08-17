/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("PopoverFilterModel", ["backbone"], function (Backbone) {
    "use strict";
    var PopoverFilterModel = Backbone.Model.extend({
        initialize : function () {
            var model = this;
            mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLOSE_POPOVER, function (evt) {
                model.set("searchText", "");
            });
            this.on("change:searchText", this.filter, this);
        },
        filter : function () {
            var popoverModel = this.get("popoverModel");
            if (popoverModel) {
                popoverModel.filterModel();
            }
        }
    }), popoverFilterModel;
    popoverFilterModel = new PopoverFilterModel();
    return popoverFilterModel;
});

