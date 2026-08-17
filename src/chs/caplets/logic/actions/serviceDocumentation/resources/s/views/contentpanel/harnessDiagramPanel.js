/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    [
        "views/contentpanel/HarnessDesignPanel",
        "models/selectedSystem",
    ],
    function (HarnessDesignPanel, selectedSystem)
    {
        "use strict";

        var DocumentSetsContentPanel,
            p = mentor.publisher;

        DocumentSetsContentPanel = HarnessDesignPanel.extend({

            subscribedEventType: "harnessReport",
            publishedEventType: "harnessDiagram",
            designType: "harnessLayoutDiagram",

            getContentType: function ()
            {
                return p.contentType.HARNESS_LAYOUT_DIAGRAM;
            },
            getDataId: function ()
            {
                return selectedSystem.get("harnessLayoutId");
            },
            getDocumentType: function ()
            {
                return "diagrams";
            }

        });

        return new DocumentSetsContentPanel();
    }
)