/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/

define("PrintContentCollection", [],
        function ()
        {
            "use strict";
            var PrintContentModel = Backbone.Model.extend({idAttribute: "mainText"}), PrintContentItems;
            PrintContentItems = Backbone.Collection.extend({
                model: PrintContentModel,

                fetch: function (model)
                {
                    var data = this.getData(model);
                    //do not list pdf 2d location views
                    if (data) {
                        data = data.filter(function (item)
                        {
                            return !(item.type === "locationViewSVGLoadArea" &&
                                    ((item.url && typeof item.url == "string" && item.url.endsWith(".pdf")) ||
                                            (item.url == "object" && item.url.path &&
                                                    itrm.url.path.endsWith((".pdf")))));
                        });
                    }
                    this.reset(data);
                },
                getData: function (data)
                {
                    return data;
                }
            });

            return new PrintContentItems();
        });