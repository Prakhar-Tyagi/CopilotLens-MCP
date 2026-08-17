/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/

define("OptionsCollection",
    function () {
        "use strict";
        var option = Backbone.Model.extend(), OptionsItems;
        OptionsItems = Backbone.Collection.extend({
            model : option,
            initialize : function () {
            },
            fetch : function (optionsModel) {
                var index, models = [], items = this.getData(optionsModel), model;
                for (index in items) {
                    if (items.hasOwnProperty(index)) {
                        model = new option();
                        model.set(items[index]);
                        model.toolTips = items[index].getToolTips && items[index].getToolTips();
                        models.push(model);
                    }

                }
                this.reset(models);
                return this.models;
            },
            getData : function (optionsModel) {
                return optionsModel;
            }
        });
        return new OptionsItems();
    });