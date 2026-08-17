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
    'views/p/relatedData/objectsView',
    "views/designobjects/designObjectsCollection",
    "PopoverFilterModel",
    "RelatedDataPopoverModel",
    "views/p/relatedData/activeDiagAwareColl"
], function (objectsView,
        DesignObjects,
        PopoverFilterModel,
        RelatedDataPopoverModel,
        ActiveDiagramAwareCollection)
{
    "use strict";

    var p = mentor.publisher;
    var globalObjectConfiguration = {
        "signalsItem": {
            type: "signals",
            category: p.documentCategory.GROUNDS, sectionTitle: "SignalTitle"
        },
        "devicesItem": {
            type: "devices",
            category: p.documentCategory.DEVICES, sectionTitle: "DevicesTitle"
        },
        "connectorsItem": {
            type: "connectors",
            category: p.documentCategory.CONNECTORS, sectionTitle: "ConnectorTitle"
        }
    };

    var viewFactory = function (id)
    {
        var factory = {
            ActiveDiagramAwareCollection: ActiveDiagramAwareCollection,
            DesignObjectsCollection: DesignObjects,
            createCollection: function (sectionConfig)
            {
                var collectionBehaviour = this.getViewAttributes(sectionConfig);
                var RelatedDataObjCollection = this.DesignObjectsCollection(collectionBehaviour);
                var activeDiaramAware = this.ActiveDiagramAwareCollection(sectionConfig);
                var RelDataActiveDiaAwareColl = RelatedDataObjCollection.extend(activeDiaramAware);
                return new RelDataActiveDiaAwareColl();
            },
            getViewAttributes: function (sectionConfig)
            {
                return {
                    getUrl: function ()
                    {
                        try {
                            var currentProject = require("currentPackage");
                            var selectedSystem = require("models/selectedSystem");
                            var systemID = selectedSystem.get("systemId");
                            if (systemID) {
                                return p.pathResolver.getSystemObjectDataFilePath(p.project.getId(),
                                        systemID,
                                        sectionConfig.type).replace(".xml", ".json");
                            }
                        }
                        catch (e) {
                            return "";
                        }
                    },
                    usePlainSearch: true,
                    useSameThreadToLoad: true,
                    searchModel: PopoverFilterModel
                };
            },
            OjectsView: objectsView,
            RelatedDataPopoverModel: RelatedDataPopoverModel,
            reRenderView: function ()
            {
                this.designObjectSectionView.paginationDelegate.reset();
                this.designObjectSectionView.clearView();
                this.designObjectSectionView.inprogress = false;
                this.designObjectSectionView.render();
            },
            initCollectionAndReRenderView: function ()
            {
                this.designObjects.initialize();
                setTimeout(this.reRenderView.bind(this), 100);
            },
            create: function ()
            {
                var sectionConfig = globalObjectConfiguration[this.sectionId];
                this.designObjects = this.createCollection(sectionConfig);
                this.designObjectSectionView = this.OjectsView(this.designObjects);
                this.designObjectSectionView.title = sectionConfig.sectionTitle || this.sectionId;
                this.RelatedDataPopoverModel.on("load", this.initCollectionAndReRenderView.bind(this));
                this.RelatedDataPopoverModel.on("closed",
                        this.designObjects.resetData.bind(this.designObjects));
                return this.designObjectSectionView;
            }
        };
        factory.sectionId = id;
        return factory.create();
    };
    viewFactory.isViewFactory = true;
    return viewFactory;
});
