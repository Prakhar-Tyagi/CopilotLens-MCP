/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("ReportsPopoverModel", ["PopoverModel", "ReportsCollection"],
    function (PopoverModel, reports) {
        "use strict";
        var ReportsPopoverModel = PopoverModel.extend({
            loadCollections : function (model) {
                reports.fetch(model);
            }
        }), reportsPopoverModel;
        reportsPopoverModel = new ReportsPopoverModel();
        return _.extend(reportsPopoverModel, Backbone.Events);
    });

