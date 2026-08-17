/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(["underscore", "currentPackage", "SectionCollection"], function (_, currentPackage, BaseCollection) {
    "use strict";
    var p = mentor.publisher;
    var Faults = BaseCollection.extend({
        model: Backbone.Model.extend({idAttribute: "code"}),

        getData: function (selectedProjectId) {
            var result = {};
            var projectId = (currentPackage && currentPackage.id) || selectedProjectId;
            $.ajax({
                url: Utils.prepareFilePath(projectId + "/faultcodes.json"),
                async: false,
                success: function (data, textStatus, xhr) {
                    if (data) {
                        Object.keys(data).forEach(function (key, index) {
                            var item = data[key];
                            item.index = index;
                            result[key] = item;
                        });
                    }
                    else {
                        console.error("Data is undefined");
                    }
                }.bind(this),
                error: function (xhr, textStatus, errorThrown) {
                    console.error("Error loading faultcodes.json:", textStatus, errorThrown);
                },
                dataType: "json"
            });
            return result;
        },

        findById: function (modelId) {
            return this.find(function (model) {
                return model.get('index') == modelId;
            });
        },

        fetch: function (projId) {
            var models, items = this.getData(projId);
            models = this.createModelObj(items);
            this.reset(models);
            return this.models;
        }
    });
    return new Faults();
});