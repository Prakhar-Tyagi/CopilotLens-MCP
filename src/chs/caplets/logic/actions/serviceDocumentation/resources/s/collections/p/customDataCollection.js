/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define("CustomDataCollection", ["PopoverItem"],
    function (PopoverItem) {
        "use strict";
        var p = mentor.publisher, CustomDataCollection = PopoverItem.extend({
            getData : function (designObject) {
                var customDataArray = [], originalArray = (designObject.getCustomData) ? designObject.getCustomData() :
                    [];
                var type = designObject && designObject.getType && designObject.getType();
                var customDataOrder = p.dataLoader.getCustomPopoverSectionOrder() || [];
                Utils.sortByGivenOrder(originalArray, customDataOrder[type], function (obj) {
                    return obj.title;
                });

                for (var k = 0; k < originalArray.length; k++) {
                    customDataArray[k] = {};
                    customDataArray[k] = originalArray[k];
                    if (k === 0) {
                        customDataArray[k].expand = this.expand;
                    } else {
                        customDataArray[k].expand = false;
                    }
                }
                return customDataArray;
            },
            findDataContent: function (collection, dataId) {
                const dataContent = collection.get('listItems');
                if (dataContent && dataContent.length > 0) {
                    for (let k = 0; k < dataContent.length; k++) {
                        if (dataContent[k].mainText === dataId) {
                            return dataContent[k];
                        }
                    }
                }
            }
        });
        return new CustomDataCollection();
    });