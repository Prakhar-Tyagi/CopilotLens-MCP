/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("SelectedPrintContentModel", ["PopoverModel"],
    function (PopoverModel) {
        "use strict";
        var SelectedPrintContentModel = PopoverModel.extend({
                updateModel : function (content, selected) {
                    if (!selected) {
                        this.contents = this.deleteSelectedItemFromArray(content, this.contents);
                    } else {
                        if ($.inArray(content, this.contents) == -1) {
                        this.contents.push(content);
                        }
                    }
                },
                contents : [],
                deleteSelectedItemFromArray : function (item, array) {
                    var k = 0, tempArray = [];
                    for (k = 0; k < array.length; k = k + 1) {
                        if (array[k] !== item) {
                            tempArray.push(array[k]);
                        }
                    }
                    return tempArray;
                }, reset : function () {
                    this.contents = [];
                }
            }),
            selectedContentModel = new SelectedPrintContentModel();
        return _.extend(selectedContentModel, Backbone.Events);
    });

