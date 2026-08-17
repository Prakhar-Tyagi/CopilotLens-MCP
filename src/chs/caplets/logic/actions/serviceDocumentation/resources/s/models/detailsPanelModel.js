/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, Utils*/
define([
    "currentPackage"
], function (selectedPackage)
{
    "use strict";
    var DetailsPanelModel = Backbone.Model.extend({

        firstItem:"",

        id:"-1",

        initialize:function ()
        {
        },

        fetch:function ()
        {

            var firstPackage = selectedPackage.getFirstSection() || {}, listItems;
            try {
                listItems = firstPackage.listItems() || [];
            }
            catch (e) {
                listItems = [];
            }
            if (listItems.length > 0 && listItems[0]) {
                var newModel = new DetailsPanelModel();
                newModel.set(listItems[0]);
                newModel.type = firstPackage.type;
                this.firstItem = newModel;
                //this.set(listItems[0]);
            }

        }
    });

    return new DetailsPanelModel();
});
