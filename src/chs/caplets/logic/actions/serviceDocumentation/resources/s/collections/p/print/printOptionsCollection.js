/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/

define("PrintOptionsCollection", [],
    function () {
        "use strict";
        var PrintOptionsModel = Backbone.Model.extend(), PrintOptionItems;
        PrintOptionItems = Backbone.Collection.extend({
            model : PrintOptionsModel,

            fetch : function (model) {
                this.reset(this.getData(model));
            },
            getData : function (data) {
                return data;
            }
        });

        return new PrintOptionItems();
    });