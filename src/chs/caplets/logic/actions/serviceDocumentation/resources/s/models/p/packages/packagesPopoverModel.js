/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("PackagesPopoverModel", ["PopoverModel", "currentPackage", "PackagesCollection"],
    function (PopoverModel, currentPackage, packages) {
        "use strict";
        var PackagesPopoverModel = PopoverModel.extend({
            initialize : function () {
                var that = this;
                currentPackage.on("change:id", function () {
                    that.clear({silent : true});
                });
            },
            loadCollections : function (model) {
                packages.fetch(model);
            },
            loadData : function (data) {
                return data;
            }
        }), packagesPopoverModel;
        packagesPopoverModel = new PackagesPopoverModel();
        return _.extend(packagesPopoverModel, Backbone.Events);
    });

