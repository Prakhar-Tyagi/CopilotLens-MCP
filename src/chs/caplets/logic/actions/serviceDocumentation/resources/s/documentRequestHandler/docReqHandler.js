/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */(function (p) {
    "use strict";
    p.documentRequestHandlerFactory = {

        get: function (type) {
            type = type || "";
            if (type === p.contentType.OBJECT_CROSS_REF) {
                return p.crossReferenceHandler;
            }
            else if (type === p.contentType.OLD_DESIGN_REVISION || type === p.contentType.NEW_DESIGN_REVISION) {
                return p.illustratorDesignReferenceHandler;
            }
            else if (type === p.contentType.DESIGN_OBJECT) {
                return p.objectPopoverDisplayHandler;
            }
            else if (type === p.contentType.PDF_OBJECT) {
                return p.pdfTextClickHandler;
            }

        }

    }

})(mentor.publisher);

(function (p) {
    p.objectPopoverDisplayHandler = {
        display: function (content, event) {
            content = content || {};
            var x = ($("#detailPopup")[0] && $("#detailPopup")[0].left) || 0;
            var y = ($("#detailPopup")[0] && $("#detailPopup")[0].top) || 0;
            var designObject = p.project.loadObjectData(content.systemId,
                    content.id), xrefs;
            xrefs = designObject.getCrossReferences ? designObject.getCrossReferences().listItems : [];
            displayAttributes("", content.id, x, y).showPopUpPanel(event);
            var data = {};
            data.objectId = content.id;
            data.systemId = content.systemId;
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS,
                    data);
        },

        loadToolTip: function (content) {

        }

    }

}(mentor.publisher));

(function (p) {
    var contentArea, documentDisplayHandler, navigationPanelModel;
    p.crossReferenceHandler = {
        setNavPanelModel: function (p_navModel) {
            navigationPanelModel = p_navModel;
        },
        getNavPanelModel: function () {
            return navigationPanelModel || require("models/navPanelModel");
        },
        setContentArea: function (p_contentArea) {
            contentArea = p_contentArea;
        },
        getContentArea: function () {
            return contentArea || getWindowObj().mentor.publisher.contentArea;
        },
        setDocumentDisplayHandler: function (p_displayHandler) {
            documentDisplayHandler = p_displayHandler;
        },
        getDocumentDisplayHandler: function () {
            return documentDisplayHandler || getWindowObj().mentor.publisher.fileDisplayHandler;
        },
        display: function (content) {
            var type, xrefToShow;
            type = p.contentType.SYSTEM_SVG;
            if (content.attributes) {
                content = content.attributes;
                content.type = type;
            }
            if (!content.diagramId) {

                xrefToShow = this.fetchFirstCrossReference(content);
                if (!xrefToShow) {
                    return;
                }
                else {
                    content = xrefToShow;
                    content.type = type;
                }

            }
            this.getContentArea().closeAllSplitPanelsIfNewSystemIsOpened(content)
            this.showXref(content);

        },
        showXref: function (content) {
            this.getDocumentDisplayHandler().display(content);
            this.getNavPanelModel().trigger("scrollNavigationPanelToTheSelectedElement");
        },
        fetchFirstCrossReference: function (content) {
            var designObject = mentor.publisher.project.loadObjectData(content.systemId, content.objectId),
                    xrefs;
            xrefs = designObject.getCrossReferences ? designObject.getCrossReferences().listItems : [];
            if (xrefs && xrefs.length > 0) {
                return this.firstActiveSystem(xrefs);
            }
            else {
                this.showXref(content);
                return "";
            }
        },
        isInOpenDiagrams: function (content) {
            var designObject = mentor.publisher.project.loadObjectData(content.systemId, content.objectId),
                    xrefs,
                    opts;
            opts = {
                includeOpenedDiagrams: true,
                includeUnopenedDiagrams: false
            }
            xrefs = designObject.getCrossReferences ? designObject.getCrossReferences(opts).listItems : [];
            return (xrefs && xrefs.length > 0);
        },
        firstActiveSystem: function (xrefs) {
            return xrefs[0];
        },
        createURL: function (content) {

            var type, xrefToShow;
            if (content.attributes) {
                content = content.attributes;
            }
            if (!content.diagramId) {
                type = p.contentType.SYSTEM_SVG;
                xrefToShow = this.fetchFirstCrossReference(content);
                if (!xrefToShow) {
                    return;
                }
                else {
                    content = xrefToShow;
                }
            }
            if (!content.type) {
                content.type = p.contentType.SYSTEM_SVG;
            }
            return "popout.html#/system/" + content.systemId + "/" +
                    content.diagramId + "/" +
                    p.project.getId().replace("\\", "/") + "/" + content.objectId;
        },
        loadObject: function (content) {
            var designObject = p.project.loadObjectData(content.systemId,
                    content.objectId || content.get("objectId"));
            return designObject;
        },

        loadToolTip: function (content) {
            if (content && content.id) {
                //return this.fetchFirstCrossReference(content);
            }
        }
    };
})(mentor.publisher);

(function (p) {
    function fnPostTextCombiner(text, resultantText)
    {
        return resultantText + text;
    }

    function fnPreTextCombiner(text, resultantText)
    {
        return text + resultantText;
    }

    p.pdfTextClickHandler = {

        readSibilingText: function (currentText, fnSiblingFinder, fnTextCombiner, element, fnMatchFinder) {
            var completeText = currentText;
            var sibling = element;
            var y = $(element).offset().top;
            while (true) {
                sibling = fnSiblingFinder(sibling);

                if (sibling) {
                    var text = $(sibling).text();
                    if (text) {
                        var yPos = $(sibling).offset().top;
                        if (text && yPos === y) {
                            completeText = fnTextCombiner(text, completeText);
                            if (fnMatchFinder && typeof fnMatchFinder === 'function' &&
                                    fnMatchFinder.call(this, completeText.trim())) {
                                break;
                            }
                        }
                        else {
                            break;
                        }
                    }
                    else {
                        break;
                    }
                }
                else {
                    break;
                }
            }
            return completeText;
        },

        getNextElement: function (currentElement) {
            return $(currentElement).next();
        },

        getPrevElement: function (currentElement) {
            return $(currentElement).prev();
        },

        getElement: function (evt) {
            return $(evt.target);
        },

        concatenateAdjacentTextFromSameLine: function (evt) {
            var target = this.getElement(evt);
            var completeText = $(target).text() || "";

            completeText = this.readSibilingText(completeText, this.getNextElement, fnPostTextCombiner, target);

            completeText = this.readSibilingText(completeText, this.getPrevElement, fnPreTextCombiner, target);
            completeText = completeText || "";
            return this.getUIDsForName(completeText.trim());
        },

        getMatchingRightContent: function (evt) {
            var target = this.getElement(evt);
            var completeText = $(target).text() || "";

            completeText = this.readSibilingText(completeText, this.getNextElement, fnPostTextCombiner, target,
                    this.getUIDsForName);
            completeText = completeText || "";
            return this.getUIDsForName(completeText.trim());
        },

        onContentClick: function (content, x, y, shouldShowPopover) {
            if (!content) {
                return;
            }

            this.highlightMatches(content);
            this.zoomMatches(content);

            if (!shouldShowPopover) {
                return;
            }

            if (content[0].relatedObjects.length == 1) {
                this.displayObjectPopover("", content[0].relatedObjects[0].objectConnUID, x, y);
            }
            else if (content[0].relatedObjects.length > 1) {
                this.showMultipleMatches(content, x, y);
            }
        },

        onClick: function (evt, textValue, x, y, shouldShowPopover) {
            // Match enclosing div text
            var content = this.getUIDsForName(textValue);

            // match whole line
            if (!content) {
                content = this.concatenateAdjacentTextFromSameLine(evt);
            }

            // match text to the right, break at first match
            if (!content) {
                content = this.getMatchingRightContent(evt);
            }

            this.onContentClick(content, x, y, shouldShowPopover);
        },
        attachEventHandler: function (pdfDoc, containerCSSSelector, opts) {
            var that = this;

            opts = opts || {};
            opts.opensPopoverOnSecondClick = opts.opensPopoverOnSecondClick || false;
            opts.opensPopoverOnControlClick = opts.opensPopoverOnControlClick || false;

            var pdfDoc$ = $(pdfDoc);
            if (pdfDoc$.attr('data-eventHandler-added') !== 'true') {
                pdfDoc$.on("click", function (evt) {
                    var x = evt.clientX + $(containerCSSSelector).offset().left;
                    var y = evt.clientY + $(containerCSSSelector).offset().top;

                    var shouldShowPopover =
                            (opts.opensPopoverOnControlClick && evt.ctrlKey) ||
                            !opts.opensPopoverOnSecondClick ||
                            $(evt.target).attr("data-highlighted") === "true";

                    var objectNames = $(evt.target).data("object-names") || [];
                    if (objectNames.length == 0) {
                        var textValue = $(evt.target).text() || "";
                        textValue = textValue.trim();
                        that.onClick(evt, textValue, x, y, shouldShowPopover);
                    }
                    else {
                        var content = that.getUIDsForName(objectNames[0]);
                        that.onContentClick(content, x, y, shouldShowPopover);
                    }

                    if (opts.opensPopoverOnSecondClick) {
                        $("*[data-highlighted='true']", $(containerCSSSelector)).attr("data-highlighted", "");
                        $(evt.target).attr("data-highlighted", "true");
                    }
                });
                setTimeout(function () {
                    $("#presentationMode", pdfDoc$).css("display", "none");
                    $("#secondaryPresentationMode", pdfDoc$).css("display", "none");
                    $("#openFile", pdfDoc$).css("display", "none");
                    $("#secondaryOpenFile", pdfDoc$).css("display", "none");
                }, 300);
                pdfDoc$.attr('data-eventHandler-added', 'true')
            }
        },
        getUIDsForName: function (text) {
            text = text || "";
            text = text.trim();
            return mentor.publisher.nameToUIDMap.getUIDsFor(text);
        },
        displayObjectPopover: function (schemUID, UID, x, y) {
            var popup = displayAttributes("", UID, x, y);
            popup.showPopUpPanel();
        },
        highlightMatches: function (content) {
            if (!content || content.length === 0 || !content[0].relatedObjects ||
                    content[0].relatedObjects.length === 0) {
                return;
            }

            var objects = content[0].relatedObjects
                    .map(function (item) {
                        return item.objectConnUID;
                    });
            p.eventDispatcher.dispatchEvent(p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                objects : objects
            });
        },
        zoomMatches: function (content) {
            setTimeout(function () {
                var config = p.xmlLoader.loadFile("config.json", false, true, "json");
                if (config && config.data && config.data['autoZoomOnClick']) {
                    window.crossHighlightHandler.zoomViews();
                }
            }, 500);
        },
        showMultipleMatches: function (content, x, y) {
            if (!content || content.length === 0 || !content[0].relatedObjects ||
                    content[0].relatedObjects.length === 0) {
                return;
            }

            var contentToDisplay = [{}];
            contentToDisplay[0].title = content[0].name;
            contentToDisplay[0].relatedDocuments = content[0].relatedDocuments;
            contentToDisplay[0].relatedObjects = _.filter(content[0].relatedObjects, function (obj) {
                var objs = [], filtered = [];
                objs.push(obj);
                filtered = objs;
                return filtered.length === 1;
            });
            contentToDisplay[0].relatedObjects = _.map(contentToDisplay[0].relatedObjects, function (item) {
                var diaId = item.svgPath || "";
                var from = diaId.lastIndexOf("\\");
                var to = diaId.lastIndexOf(".svg");
                diaId = diaId.substr(from + 1, to - from - 1);

                var suffix = "";
                switch (item.type) {
                    case p.contentType.OLD_DESIGN_REVISION:
                        suffix = " (Old)";
                        break;

                    case p.contentType.NEW_DESIGN_REVISION:
                        suffix = " (New)";
                        break;
                }

                var id = item.objectSchemUid || item.objectConnUID;
                return extend(item, {
                    mainText: item.name + suffix,
                    subText: item.diagramName,
                    id: id,
                    diagramId: item.diagramId || diaId,
                    diagramUID: item.diagramId || "",
                    objectId: item.objectConnUID,
                    optionExpression: item.optionExpressions,
                    type: item.type || p.contentType.OBJECT_CROSS_REF
                });
            });
            contentToDisplay[0].relatedObjects = contentToDisplay[0].relatedObjects.filter(createXrefsFilter({
                filterProp: "diagramUID"
            }));
            contentToDisplay[0].relatedObjects = _.sortBy(contentToDisplay[0].relatedObjects, "mainText");

            if ((!contentToDisplay[0].relatedObjects || contentToDisplay[0].relatedObjects.length == 0) &&
                    (!contentToDisplay[0].relatedDocuments || contentToDisplay[0].relatedDocuments.length == 0)) {
                return;
            }

            if (contentToDisplay[0].relatedObjects[0]) {
                this.showPopover(mentor.publisher.languageTranslator.localize("Links"), contentToDisplay, x, y, true);
                p.eventDispatcher.dispatchEvent(p.events.UPDATE_SIGNAL_TRACER,
                        {systemId: "dummySystemId", id: contentToDisplay[0].relatedObjects[0].objectId, flush: true});
            }
        },
        showPopover: function (title, content, x, y, showFilter) {
            var Backbone = require("backbone");
            var popoverFilterModel = new (Backbone.Model.extend({}))();
            p.designObjectPopover.showPopover(content[0].title, x, y, true, popoverFilterModel);

            setTimeout(function () {
                p.designObjectPopover.addSection(mentor.publisher.languageTranslator.localize("Links"),
                        content[0].relatedObjects,
                        popoverFilterModel);
            }, 100)
        }

    }

    function getContentToShow(content)
    {
        var contentToShow = content.attributes || content;
        contentToShow.type = contentToShow.type || content.type;
        return contentToShow;
    };

    p.illustratorDesignReferenceHandler = {
        display: function (content) {
            var contentToShow = getContentToShow(content);
            if (contentToShow.diagramUID) {
                require(
                        ["fileDisplayHandler"],
                        function (fileDisplayHandler) {
                            fileDisplayHandler.display({
                                listItemId: contentToShow.systemId,
                                layoutId: contentToShow.systemId,
                                id: contentToShow.diagramUID,
                                objectId: contentToShow.objectConnUID,
                                reset: false,
                                type: contentToShow.type,
                                group: p.documentCategory.DIAGRAMS,
                                doNotSaveAsHistory: true
                            });

                            setTimeout(function () {
                                p.eventDispatcher.dispatchEvent(p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                                    objects: [contentToShow.objectConnUID]
                                });

                                setTimeout(function () {
                                    var config = mentor.publisher.xmlLoader.loadFile("config.json", false, true,
                                            "json");
                                    if (config && config.data && config.data['autoZoomOnClick']) {
                                        window.crossHighlightHandler.zoomViews();

                                    }
                                }, 100);
                            }, 2000);
                        }
                );
            }
        },

        loadToolTip: function (content) {
            // no-op
        },

        createURL: function (content) {
            var contentToShow = getContentToShow(content);
            return p.popoutHandler.createURL({
                type: contentToShow.type,
                layoutId: contentToShow.systemId,
                objectId: contentToShow.objectConnUID,
                diagramId: contentToShow.diagramUID,
                projectId: p.project.getId().replace("\\", "/")
            });
        }
    };

}(mentor.publisher));

mentor.publisher.nameToUIDMap = (function (p) {
    "use strict";

    var invalidFileNameChars = ['\\\\', '/', ':', '\\*', ';', '"', '<', '>', '\\|', '!', '\\?'];

    return {
        getBucket: function (hashCode) {
            return hashCode % 100;
        },
        replaceInvalidChar: function (text) {
            var transformedText = text;
            _.each(invalidFileNameChars, function (ch) {
                var re = new RegExp(ch, 'g');
                transformedText = transformedText.replace(re, "_");
            });
            return transformedText;

        },

        getNameToUIDFilePath: function (text) {
            var packagePathPrefix = mentor.publisher.project.getId();
            var validFileName = this.replaceInvalidChar(text);
            return packagePathPrefix + "/object-names-to-uid-map" + "/" + this.getBucket(this.getBucketFolderByFileName(validFileName)) +
                    "/" + validFileName + ".json";
        },

        loadData: function (config) {
            if (config) {

                return mentor.publisher.xmlLoader.loadFile(config.file, config.async, config.cacheFile, config.type);
            }
        },

        getUIDsFor: function (text) {
            var file = this.getNameToUIDFilePath(text);
            var content = this.loadData({file: file, async: false, cacheFile: true, type: "json"});
            return content.data;

        },

        getBucketFolderByFileName: function (fileName) {
            for (var ret = 0, i = 0, len = fileName.length; i < len; i++) {
                ret = (ret + fileName.charCodeAt(i));
            }
            return ret;
        }
    };
}(mentor.publisher));