/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
define("ra3DModel", ["backbone", "underscore", "base3DModel"], function (Backbone, _, Base3DModel) {
    "use strict";
    var ItemMapping = Backbone.Model.extend({
        itemId: null, //RA "Item"
        mappings: null, //SystemPath data
        defaults: function () {
            this.set("itemId", "");
            this.set("mappings", null);
        },
        getItemId: function () {
            return this.get("itemId");
        },
        getMappings: function () {
            return this.get("mappings");
        }
    });
    var ItemMappings = Backbone.Collection.extend({
        model: ItemMapping,
        defaults: {
            model: new ItemMapping()
        },
        modelId: function (attrs) {
            return attrs.itemId;
        }
    })
    var ModelItemIdMap = Backbone.Model.extend({
        path: null, //modelName
        itemMappings: null,
        defaults: function () {
            this.set("path", "");
            this.set("itemMappings", new ItemMappings());
        },
        getPath: function () {
            return this.get("path");
        },
        getItemMappings: function () {
            return this.get("itemMappings");
        }
    });
    var ModelItemIdMaps = Backbone.Collection.extend({
        model: ModelItemIdMap,
        modelId: function (attrs) {
            return attrs.path;
        }
    });

    var RA3DModel = Base3DModel.extend({
        constructor: function () {
            this.modelItemIdMaps = new ModelItemIdMaps();
            this.selectedItems = [];
            this.sheetViews = [];
        },

        selectedItems: null,
        modelItemIdMaps: null,
        sheetViews: null,

        getModelItemIdMaps: function () {
            return this.modelItemIdMaps;
        },

        loadSystemPaths: function (modelPath, uid, systemPaths) {
            var _data = {}, syss = [], optExprs = [];
            _.foldl(systemPaths, this.parseSysPathData, syss)
            _.each(syss, function (sys) {
                optExprs.push(sys.optionExpression);
            });
            _data.systems = syss;
            _data.optionExpression = optExprs;

            //do we have mappings for this model? Get if we do, create otherwise
            var itemIdMapsForModel = this.getModelItemIdMaps().findWhere({path: modelPath})
            if (!itemIdMapsForModel) {
                itemIdMapsForModel = new ModelItemIdMap({path: modelPath});
            }
            itemIdMapsForModel.getItemMappings().add(new ItemMapping({itemId: uid, mappings: _data}));
            this.getModelItemIdMaps().add(itemIdMapsForModel, {merge: true});
        },

        doAjax: function (modelPath) {
            var data;
            var that = this;
            $.ajax({
                url: Utils.prepareFilePath("./" + modelPath + "/itemIdMap.json"),
                dataType: 'json',
                async: false,
                data: data,
                success: function (data) {
                    var itemIdToUIDsMap = data.itemIdToUIDsMap;
                    var names = Object.keys(itemIdToUIDsMap);
                    _.each(names, function (name, i, list) {
                        var systemPaths = itemIdToUIDsMap[name];
                        that.loadSystemPaths(modelPath, name, systemPaths)
                    });
                }
            });
        },

        getItemIdMapsForModel: function (modelPath) {
            if (!this.getModelItemIdMaps().findWhere({path: modelPath})) {
                this.doAjax(modelPath);
            }
            return this.getModelItemIdMaps().findWhere({path: modelPath});
        },

        getSystemDataForRapidAuthorItemInstance: function (itemName, modelPath) {
            //get all the mappings (arrays of system paths) for the found object names
            var itemIdMapsForModel = this.getItemIdMapsForModel(modelPath);
            var itemMappings = [];
            var mappings = itemIdMapsForModel.getItemMappings().findWhere({itemId: itemName});
            if (mappings) {
                itemMappings.push(mappings);
            }
            var filtered = [], that = this;

            //filter the results
            _.each(itemMappings, function (item, i, list) {
                var filterData = that.filterData(item.getMappings());
                _.each(filterData, function (item, i, list) {
                    filtered.push(item);
                });
            });

            return filtered;

        },

        getSystemDataForRapidAuthorItem: function (item, modelPath) {

            var objectNames = _.uniq(item.objectNames);
            //get all the mappings (arrays of system paths) for the found object names
            var itemIdMapsForModel = this.getItemIdMapsForModel(modelPath);
            var itemMappings = [];
            _.each(objectNames, function (name, i, list) {
                var mappings = itemIdMapsForModel.getItemMappings().findWhere({itemId: name});
                if (mappings) {
                    itemMappings.push(mappings);
                }
            });

            var filtered = [], that = this;

            //filter the results
            _.each(itemMappings, function (item, i, list) {
                var filterData = that.filterData(item.getMappings());
                _.each(filterData, function (system, i, list) {
                    filtered.push(system);
                });
            });

            return filtered;
        },

        /**
         * Revers mapping lookup
         * @param connUID The Capital object connUID to find Item mappings for
         * @param modelPath The Model Path
         * @returns array of matched itemIDs
         */
        getItemNamesForSystemId: function (connUID, modelPath) {
            var itemIdMapsForModel = this.getItemIdMapsForModel(modelPath);
            var matches = _.filter(itemIdMapsForModel.getItemMappings().models, function(itemIdMap){
                var match = _.filter(itemIdMap.getMappings().systems, function(system) {
                    return system['connUID'] === connUID || system['sharedUID'] === connUID;
                });
                return match.length > 0;
            });
            return _.map(matches, function(match, i, matches) {
               return match.get('itemId');
            });
        }
    });
    return new RA3DModel();
});
