/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define("TwoDLocationCollection", ["PopoverItem", "currentPackage"],
        function (PopoverItem, selectedPackage)
        {
            "use strict";
            var TwoDLocationCollection = PopoverItem.extend({
                initialize: function ()
                {
                    selectedPackage.on("change:id", this.fetch, this);
                },
                getData: function (designObject)
                {
                    return (designObject && designObject.get2dLocationViews)
                            ? designObject.get2dLocationViews().listItems : [];
                }
            });
            return new TwoDLocationCollection();
        });