/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/

define("ConfigurationsCollection",
    function () {
        "use strict";
        var configuration = Backbone.Model.extend(), ConfigurationItems;
        ConfigurationItems = Backbone.Collection.extend({
            model : configuration,
            initialize : function () {
            },
            fetch : function (configurationsModel) {
                var index, models = [], items = this.getData(configurationsModel), model;
                for (index in items) {
                    if (items.hasOwnProperty(index)) {
                        model = new configuration();

                        model.set(items[index]);
                        model.toolTips = items[index].getToolTips && items[index].getToolTips();
                        models.push(model);
                    }

                }
                if (models.length === 0) {
                    this.reset();
                }
                else {
                    this.reset(models);
                }
                return this.models;

                //this.reset(this.getData(configurationsModel));
            },
            getConfigurationByName: function (configName) {
                configName = configName || "";
                var items = this.models || [], index;
                for (index in items) {
                    if (items.hasOwnProperty(index) && items[index].get("name").toLowerCase() ===
                        configName.toLowerCase()) {
                        return items[index];
                    }
                }
            },
            getData : function (configurationsModel) {
                return configurationsModel;
            }
        });
        return new ConfigurationItems();
    });