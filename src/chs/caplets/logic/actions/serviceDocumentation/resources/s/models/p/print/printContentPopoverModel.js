/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("PrintContentPopoverModel", ["PopoverModel", "PrintContentCollection"],
    function (PopoverModel, printContentCollection) {
        "use strict";
        var PrintContentPopoverModel = PopoverModel.extend({
            loadCollections : function (model) {
                printContentCollection.fetch(model);
            },
            loadData : function (data) {
                return data.models;
            }
        }), printContentModel, call = function (evt) {
            printContentModel.loadPopoverData(evt);
        };
        printContentModel = new PrintContentPopoverModel();
        mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.SHOW_PANELS_TO_PRINT_POPUP, call);
        return _.extend(printContentModel, Backbone.Events);
    });

