/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, $, getIdToHighlight*/
define(
        [
            "views/p/xrefsView",
            "XRefsCollection",
            "currentPackage",
            "fileDisplayHandler"
        ],
        function (xrefsView, xrefs, selectedPackage, fileDisplayHandler)
        {
            "use strict";
            var p = mentor.publisher,
                    OLD_DESIGN_REVISION = p.contentType.OLD_DESIGN_REVISION,
                    NEW_DESIGN_REVISION = p.contentType.NEW_DESIGN_REVISION;

            xrefsView.getTypeForXREF = function (content)
            {
                return (content.get('illustratorDesignType') == 'old') ? OLD_DESIGN_REVISION : NEW_DESIGN_REVISION;
            };

            xrefsView.createURL = function (content)
            {
                var type = this.getTypeForXREF(content);
                return p.popoutHandler.createURL({
                    type: type,
                    layoutId: content.get("systemId"),
                    objectId: content.get("objectId"),
                    diagramId: this.getDiagramId(content),
                    projectId: selectedPackage.get("id").replace("\\", "/")
                });
            };

            xrefsView.getItemContent = function (itemId)
            {
                var content, path, xref;
                xref = xrefs.get(itemId);
                content = {
                    listItemId: xref.get('id'),
                    layoutId: xref.get('id'),
                    id: xref.get('diagramId'),
                    objectId: xref.get('objectId'),
                    reset: false,
                    type: this.getTypeForXREF(xref),
                    group: p.documentCategory.DIAGRAMS,
                    doNotSaveAsHistory: true
                };
                return content;
            };

            xrefsView.displayContent = function (content)
            {
                fileDisplayHandler.display(content);
            };

            return xrefsView;
        });