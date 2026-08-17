/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, ?SISW?), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer?s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define("XRefsCollectionItem", ["PopoverItem", "XRefActiveConfigModel"],
        function (PopoverItem, xRefActiveConfigModel) {
            "use strict";
            var XRefsCollectionItem = PopoverItem.extend({
                model: Backbone.Model.extend({idAttribute: "cid"}),
                applyFilter: true,
                initialize: function () {
                    PopoverItem.prototype.initialize.call(this);
                },
                getData: function (designObject) {
                    var activeConfig = xRefActiveConfigModel.getActiveConfig(), xrefs,
                            filter = xRefActiveConfigModel.getFilter();
                    xrefs = designObject.getCrossReferences ? designObject.getCrossReferences().listItems : [];
                    return filter.applyFilter(xrefs, activeConfig);
                }
            });
            return XRefsCollectionItem;
        });