/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor*/
mentor.publisher.filter = (function (p) {
    "use strict";
    var vinOptions, filterMethod;

    p.eventDispatcher.attachEventListener(p.events.VIN_FILTER_APPLIED, function (event) {
        vinOptions = event.detail.vinOptions;

        p.filter.vinOptions = vinOptions;
        // alert(data.vinOptions);
        filterMethod = p.vinOptionExpressionFilter.applyFilter;
    });

    p.eventDispatcher.attachEventListener(p.events.ITEM_CLICKED_IN_DYNAMIC_MODE, function (event) {
        vinOptions = event.detail.vinOptions;
        if (vinOptions) {
            p.filter.vinOptions = vinOptions;
            filterMethod = p.vinOptionExpressionFilter.applyFilter;
        }
    });

    return {
        applyFilter : function (objects) {
            if (this.vinOptions) {
                return p.vinOptionExpressionFilter.applyFilter(objects, this.vinOptions);
            } else {
                return objects;
            }
        },

        applyOptionFilter : function (objects) {
            if (this.vinOptions) {
                return p.vinOptionExpressionFilter.applyFilter(objects, vinOptions);
            } else {
                return objects;
            }
        },
        reset : function () {
            this.vinOptions = "";
            filterMethod = function () {
            };
        },
        vinOptions : ""
    };

}(mentor.publisher));