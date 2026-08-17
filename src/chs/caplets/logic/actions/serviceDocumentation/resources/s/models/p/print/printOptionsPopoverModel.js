/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("PrintOptionsPopoverModel", ["PopoverModel", "PrintOptionsCollection"],
    function (PopoverModel, printOptions) {
        "use strict";
        var PrintPopoverModel = PopoverModel.extend({
            loadCollections : function (model) {
                printOptions.fetch(model);
            },
            loadData : function (data) {
                return data.models;
            }
        }), printPopoverModel, call = function (evt) {
            printPopoverModel.loadPopoverData(evt)
        };
        printPopoverModel = new PrintPopoverModel();
        mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.OPEN_PRINT_POPUP, call);
        return _.extend(printPopoverModel, Backbone.Events);
    });

