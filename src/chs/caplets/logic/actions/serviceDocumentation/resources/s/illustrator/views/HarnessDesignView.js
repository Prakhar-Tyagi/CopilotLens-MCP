/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, Utils*/
define(
        [
            "views/contentpanel/HarnessDesignPanel",
            "models/selectedSystem",
            "fileDisplayHandler",
            "harnessLayouts"
        ],
        function (HarnessDesignPanel,
                selectedSystem,
                fileDisplayHandler,
                harnessLayouts)
        {
            "use strict";

            return function (designType, idAttr)
            {
                var DocumentSetsContentPanel,
                        p = mentor.publisher;

                DocumentSetsContentPanel = HarnessDesignPanel.extend(
                        {

                    publishedEventType: "harnessDiagram",
                    designType: designType,
                    doNotLoadOnStart: true,
                    getDocumentTitle: function (documentSet)
                    {
                        return documentSet.getNameWithPartNumberAndRevision();
                    },

                    getContentType: function ()
                    {
                        return this.designType;
                    },
                    getDataId: function ()
                    {
                        return selectedSystem.get(idAttr);
                    },
                    getDocumentType: function ()
                    {
                        return "diagrams";
                    },
                    getCloseEvent: function (type)
                    {
                        return "change:clearNavigationPanelSelection";
                    }
                });
                fileDisplayHandler.addFileHandler(designType, function (content)
                {
                    //selectedSystem.set("selectedElement", content.id);
                    harnessLayouts.setSelectedHarnessDataToRender(content,
                            idAttr,
                            designType);
                });

                return new DocumentSetsContentPanel();

            };
        }
);