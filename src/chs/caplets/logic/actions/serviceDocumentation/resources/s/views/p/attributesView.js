/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(["PopoverItemView", "AttributesCollection"],
    function (PopoverItemView, attributes) {
        "use strict";
        var AttributeItem = PopoverItemView.extend({
            getData : function () {
                return attributes;
            },
            getTitle : function () {
                return "AttributesTitle";
            },
            getClassName : function () {
                return "attributes";
            },
            getTooltipContent : function (content) {
                return content.attributes;
            },
            events : {
                "mouseover .attributes>.listItem" : "showToolTip",
                "mouseout .attributes>.listItem" : "removeToolTip"
            }
        });
        return new AttributeItem();
    });
