/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define([
    'views/designobjects/designObjectsView',
    "views/designobjects/designObjectsCollection"
], function (objectsView, DesignObjects)
{
    "use strict";

    var p = mentor.publisher;
    var globalObjectConfiguration = {
        "wires": {
            type: "wires",
            category: p.documentCategory.WIRES, sectionTitle: "WiresTitle"
        },
        "nets": {
            type: "nets",
            category: p.documentCategory.NETS, sectionTitle: "NetsTitle"
        },
        "inlines": {
            type: "inlines",
            category: p.documentCategory.INLINES, sectionTitle: "InlinesTitle"
        },
        "grounds": {
            type: "grounds",
            category: p.documentCategory.GROUNDS, sectionTitle: "GroundsTitle"
        },
        "devices": {
            type: "devices",
            category: p.documentCategory.DEVICES, sectionTitle: "DevicesTitle"
        },
        "connectors": {
            type: "connectors",
            category: p.documentCategory.CONNECTORS, sectionTitle: "ConnectorTitle"
        },
        "splices": {
            type: "splices",
            category: p.documentCategory.SPLICES, sectionTitle: "SplicesTitle"
        },
        "multicores": {
            type: "multicores",
            category: p.documentCategory.MULTICORES, sectionTitle: "MulticoresTitle"
        }
    };
    var objectSectionFactory = function (id)
    {
        var designObjects, designObjectSectionView;
        var viewId = id;
        var type = viewId
        var category = viewId;
        var sectionTitle = viewId;
        var sectionConfig = globalObjectConfiguration[viewId];

        if (sectionConfig) {
            type = sectionConfig.type;
            category = sectionConfig.category;
            sectionTitle = sectionConfig.sectionTitle;
        }

        designObjects = new (DesignObjects().extend({type: type, category: category}))();
        designObjectSectionView = objectsView(designObjects);
        designObjectSectionView.title = sectionTitle;
        designObjectSectionView.type = type;

        return designObjectSectionView;
    }
    objectSectionFactory.isViewFactory = true;
    return objectSectionFactory;

});
