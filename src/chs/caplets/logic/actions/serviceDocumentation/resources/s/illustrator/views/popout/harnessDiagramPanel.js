/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, define*/
define(
        [
            "views/contentpanel/harnessDiagramPanel"
        ],
        function (harnessDiagramPanel)
        {
            "use strict";
            var p = mentor.publisher;
            harnessDiagramPanel.getDocumentTitle = function (documentSet)
            {
                return documentSet.getNameWithPartNumberAndRevision();
            };
            harnessDiagramPanel.getTitle = function ()
            {
                var doc = this.getDocumentSet();
                var title = this.getDocumentTitle(doc);

                var diagram = this.getSystemData();
                return diagram.get("title");
            };
            return harnessDiagramPanel;
        }
);