/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["routers/multipleDocumentRouter", "harnessLayouts", "fileDisplayHandler"],
        function (documentRouter, harnessLayouts, fileDisplayHandler)
        {
            "use strict";
            var p = mentor.publisher;
            var revisionType = {
                1: p.contentType.OLD_DESIGN_REVISION,
                2: p.contentType.NEW_DESIGN_REVISION
            };

            function getUniquePartOFUID(id)
            {
                id = id || "";
                if (id.indexOf("UID") === 0) {
                    var start = id.indexOf("-");
                    var end = id.lastIndexOf("-");
                    return id.substr(start + 1, end - start - 1);
                }
                return id;
            }

            function openDesignsAndReport(docId)
            {
                var index = 0, isHarness = true;
                var itemToShow = harnessLayouts.getHarnessLayoutByDiagramId(docId);
                if (!itemToShow) {
                    var reports = mentor.publisher.project.getData("Reports");
                    itemToShow = _.where(reports, {id: docId});
                    itemToShow[0].type = "ChangeReport";
                    itemToShow[0].reset = false;
                    itemToShow[0].doNotSaveAsHistory = true;
                    fileDisplayHandler.display(itemToShow[0]);
                }
                else {
                    var index = harnessLayouts.indexOf(itemToShow.design);
                    fileDisplayHandler.display({
                        layoutId: itemToShow.design.get("id"), listItemId: docId, group: "diagrams",
                        reset: false,
                        doNotSaveAsHistory: true,
                        type: revisionType[index + 1],
                        mainText: itemToShow.diagram.mainText
                    });

                }
            }

            function openCustomDocOrfaceview(data)
            {
                var dataFragments = data.split("__");
                if (dataFragments.length === 4) {
                    var document = {};
                    document.type = dataFragments[1];
                    document.objectId = dataFragments[2];
                    document.id = dataFragments[3];
                    document.viewId = dataFragments[3];
                    document.reset = false;
                    document.doNotSaveAsHistory = true;
                    fileDisplayHandler.display(document);
                }
            }

            function findAndShowContent(data)
            {
                var docId = data.id;
                if (!docId) {
                    return;
                }
                if (docId.indexOf("__") === 0) {
                    openCustomDocOrfaceview(docId);
                }
                else {
                    openDesignsAndReport(docId);
                }
                mentor.publisher.detailLayoutManager.reLayoutOnWindowResize();

            }

            documentRouter.resetViews = function ()
            {
                p.detailLayoutManager.close(p.contentType.OLD_DESIGN_REVISION);
                p.detailLayoutManager.close(p.contentType.NEW_DESIGN_REVISION);
                p.detailLayoutManager.close(p.contentType.CUSTOM_VIEW);
                p.detailLayoutManager.reset(true);
                p.contentArea.reset();
            };

            documentRouter['old_handleSingleDocument'] = documentRouter.handleSingleDocument;
            
            documentRouter.handleSingleDocument = function (documentObj)
            {
                setTimeout(function ()
                {
                    documentObj = documentObj || {};
                    findAndShowContent(documentObj);
                }, 100)
            };
            function getURIComponent(content)
            {
                content = content || {};
                var type = content.get && content.get("type") || content.type;
                var uricomponent = content.id || content.get && content.get("id");
                if (type != p.contentType.OLD_DESIGN_REVISION &&
                        type != p.contentType.NEW_DESIGN_REVISION &&
                        type != p.contentType.CUSTOM_VIEW) {
                    uricomponent = "__" + type;
                    uricomponent += "__" + content.objectId;
                    uricomponent += "__" + encodeURIComponent(content.viewId || content.title);
                }

                uricomponent = getUniquePartOFUID(uricomponent);
                return uricomponent;
            }

            documentRouter['old_createURL'] = documentRouter.createURL;
            documentRouter.createURL = function (allOpenContent, URI)
            {
                for (var property in allOpenContent) {
                    if (allOpenContent.hasOwnProperty(property)) {
                        var content = allOpenContent[property];
                        var uricomponent = getURIComponent(content);
                        URI = URI + "/" + encodeURIComponent(uricomponent);
                    }
                }
                return URI;
            }
            documentRouter['old_extractDocData'] = documentRouter.extractDocData;
            documentRouter.extractDocData = function (docId)
            {
                return {id: docId};
            }
        });