/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/

define("PopoverItem", ["textSearch", "PopoverFilterModel"],
    function (textSearch, popoverFilterModel)
    {
        "use strict";
        var PopoverItemModel = Backbone.Model.extend(), PopoverItems;
        PopoverItems = Backbone.Collection.extend({
            model: PopoverItemModel,
            expand: true,
            applyFilter: false,
            comparator: function (item) {
                return Utils.translatePlainText(item.get("mainText"));
            },
            initialize: function ()
            {
            },

            getPopoverFilterModel: function ()
            {
                return popoverFilterModel;
            },

            getModels: function ()
            {
                if (this.applyFilter) {
                    var searchText = this.getPopoverFilterModel().get("searchText") || "";
                    return textSearch(this, true).filterByText(searchText);
                }
                else {
                    return this.models;
                }
            },

            fetch: function (model)
            {
                var index, models = [], items = this.getData(model), model, ModelObj = this.model ||
                    PopoverItemModel;
                for (index in items) {
                    if (items.hasOwnProperty(index)) {
                        model = new ModelObj();

                        model.set(items[index]);
                        model.toolTips = items[index] && items[index].getToolTips && items[index].getToolTips();
                        models.push(model);
                    }

                }
                this.reset(models);
                return this.models;
            },
            //this is over ridden in the concrete implementations
            getData: function (designObject)
            {
                return [];
            }
        });

        return PopoverItems;
    });