/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("XRefActiveConfigModel", ["backbone"], function (Backbone) {
    "use strict";
    var XRefActiveConfigModel = Backbone.Model.extend({
        getActiveConfig : function () {
            return this.get('config');
        },
        getFilter : function () {
            var filter = this.get('filter');
            if (!filter) {
                filter = {};
                filter.applyFilter = function (model, config) {
                    return model;
                };
                return filter;
            }
            return this.get('filter');
        }
    }), xrefActiveConfigModel;
    xrefActiveConfigModel = new XRefActiveConfigModel();
    mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.APPLY_CONFIGURATION_FILTER_ON_POPOVER,
        function (evt) {
            xrefActiveConfigModel.set('filter', evt.detail.filter);
            xrefActiveConfigModel.set('config', evt.detail.config);
        });
    return _.extend(xrefActiveConfigModel, Backbone.Events);
});

