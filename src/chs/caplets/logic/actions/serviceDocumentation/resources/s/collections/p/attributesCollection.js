/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define("AttributesCollection", ["PopoverItem"],
    function (PopoverItem) {
        "use strict";
        var AttributesCollection = PopoverItem.extend({
            getData : function (designObject) {
                return designObject.getAttributes ? designObject.getAttributes().listItems : [];
            }
        });
        return new AttributesCollection();
    });