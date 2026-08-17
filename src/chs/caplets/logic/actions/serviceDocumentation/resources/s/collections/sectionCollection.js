/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Backbone, mentor*/
define("SectionCollection", ["currentPackage", "textSearch"],
    function (selectedPackage, textSearch) {
        "use strict";
        var SectionItemModel = Backbone.Model.extend(), SectionItems;
        SectionItems = Backbone.Collection.extend({
            model : SectionItemModel,
			/*used for navigation panel filtering by indexed data*/
			getIdToFilter: function(item) {
				return item.get("id");
			},

            getModels : function () {
                var searchText = selectedPackage.get("searchText") || "";
                return textSearch(this).filterByText(searchText);
            },

            initialize : function () {
                selectedPackage.on("change:id", this.fetch, this);
                selectedPackage.on("change:language", this.fetch, this);
                selectedPackage.on("change:vin", this.fetch, this);
                selectedPackage.on("change:config", this.fetch, this);
            },

            createModelObj : function(items) {
                var index, model, models = [];
                for (index in items) {
                    if (items.hasOwnProperty(index)) {
                        model = new this.model();

                        model.set(items[index]);
                        model.toolTips = items[index].getToolTips && items[index].getToolTips();
                        models.push(model);
                    }

                }
                return models;
            },

            fetch : function () {

                var models, items = this.getData(mentor.publisher.project), model;
                models = this.createModelObj(items);
                this.reset(models);
                return this.models;
            },
            getDataLoader : function () {
                return mentor.publisher.dataLoader;
            },
            getData : function (selectedProject) {
                return [];
            }
        });

        return SectionItems;

    });
