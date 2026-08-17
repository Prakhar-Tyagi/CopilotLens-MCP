/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor, Backbone, window, require, getPluginType, $, _, isURLHTTPLink, resolveDynamicConfigurationMode*/
define("fileDisplayHandler",
        [
            "systems",
            "collections/informations",
            "FaultCodes",
            "LocationViews",
            "PanelDataCollection",
            "Harnesses",
            "harnessLayouts",
            "models/selectedSystem",
            "currentPackage",
            "routers/multipleDocumentRouter",
            "models/faceviews",
            "filehandlers/systemSVGHandler"
        ],
        function (systems,
                informations,
                faultCodes,
                locationViews,
                panelDataCollection,
                harnesses,
                harnessLayouts,
                selectedSystem,
                currentPackage,
                multipleDocumentRouter,
                faceviews,
                systemSVGHandler) {
            "use strict";

            var fileHandles = {}, isItACapitalReport = function (path, content) {
                if (!path && content.type === mentor.publisher.contentType.CUSTOM_VIEW) {
                    path = informations.get(content.id) && informations.get(content.id).get("path");
                }

                //custom view is actually a capital report
                if (path && content.type === mentor.publisher.contentType.CUSTOM_VIEW &&
                        getPluginType(path) === mentor.publisher.contentType.CAPITAL_REPORT) {
                    content.mainText = content.mainText || content.id;
                    content.type = mentor.publisher.contentType.CAPITAL_REPORT;
                    if (!content.path) {
                        content.path = path;
                    }
                }
            };

            function resetViewIfSystemIdChanged(preSystemId)
            {
                var p = mentor.publisher;
                if (!p.detailLayoutManager.isContentActive(p.contentType.SYSTEM_REPORT) && preSystemId &&
                        selectedSystem.get("systemId") !== preSystemId) {
                    selectedSystem.trigger("closeSystemDiagram");
                    return false;
                }
                return true;
            }

            fileHandles[mentor.publisher.contentType.SYSTEM_SVG] = function (content) {
                return systemSVGHandler.openSystem(content,
                        selectedSystem,
                        resetViewIfSystemIdChanged,
                        resolveDynamicConfigurationMode);
            };

            function ThreeDContentHandler(content)
            {
                require(["views/contentpanel/threeDXMLPanel"], function (threeDXMLPanel) {
                    if (!(content instanceof Backbone.Model)) {
                        var Model = Backbone.Model.extend({});
                        var threeD = new Model();
                        // In popout its a different window object, instanceof content is not matching the Backbone.model
                        threeD.set(content.attributes || content);
                        threeDXMLPanel.openObjectThreeD(threeD);
                    }
                    else {
                        threeDXMLPanel.openObjectThreeD(content);
                    }
                });
            };
            fileHandles[mentor.publisher.contentType.THREE_D_XML] = ThreeDContentHandler;
            fileHandles[mentor.publisher.contentType.JT_3D] = ThreeDContentHandler;

            function RapidAuthorContentHandler(content) {
                require(["views/contentpanel/rapidAuthorCatalogPanel"], function (rapidAuthorCatalogPanel) {
                    mentor.publisher.rapidAuthorCatalogPanel = rapidAuthorCatalogPanel;
                    if (!(content instanceof Backbone.Model)) {
                        var Model = Backbone.Model.extend({});
                        var rapidAuthor = new Model();
                        rapidAuthor.set(content.attributes || content);
                        rapidAuthorCatalogPanel.openObjectThreeD(rapidAuthor)
                    }
                    else {
                        rapidAuthorCatalogPanel.openObjectThreeD(content);
                    }
                });
            };
            fileHandles[mentor.publisher.contentType.RA_3D] = RapidAuthorContentHandler;

            function GlobalReport(content)
            {
                var diagramID, Report = Backbone.Model.extend({}), report = new Report(), mainText;
                mainText = (content.get && content.get("withoutTranslation") &&
                        content.get("withoutTranslation").mainText) || content.mainText;
                report.set({
                    mainText: mainText,
                    path: content.path,
                    splitter: content.splitter,
                    type: mentor.publisher.contentType.CAPITAL_REPORT
                });
                selectedSystem.set("harness", report, {silent: true});
                selectedSystem.trigger("change:harness");
                return report;
            }

            fileHandles[mentor.publisher.contentType.CAPITAL_REPORT] = GlobalReport;
            fileHandles[mentor.publisher.contentType.GLOBAL_REPORT] = GlobalReport;

            fileHandles[mentor.publisher.contentType.CUSTOM_VIEW] = function (content) {
                var diagramID, InformationModel = Backbone.Model.extend({}), customContent = new InformationModel(),
                        customDesignData;
                if (!content.path) {
                    customDesignData = mentor.publisher.project.getData(content.customDataType, content.id);

                    if (customDesignData && $.isArray(customDesignData) && customDesignData.length > 0 &&
                            customDesignData[0].path) {
                        customContent.set({
                            mainText: content.mainText,
                            path: customDesignData[0].path
                        });
                    }
                    else {
                        customContent = informations.get(content.id);
                    }
                }
                else {
                    customContent.set({mainText: content.mainText, path: content.path, objectId: content.objectId});
                }

                selectedSystem.set("customContent", customContent, {silent: true});
                selectedSystem.trigger("change:customContent");
                return customContent;
            };

            fileHandles[mentor.publisher.contentType.LOCATION_VIEWS] = function (content) {
                var diagramID, locationView, currentWindow = window;
                locationView = locationViews.get(content.mainText);
                if (!currentWindow.mentor.publisher.selectedSystem.get("locationView") ||
                        locationView.get("path") !==
                        currentWindow.mentor.publisher.selectedSystem.get("locationView").path) {
                    selectedSystem.set("locationView", {
                        type: mentor.publisher.contentType.LOCATION_VIEWS,
                        mainText: content.mainText,
                        path: content.path ||
                                locationView.get("path")
                    }, {silent: true});
                    selectedSystem.trigger("change:locationView");
                }
                return locationView;
            };

            fileHandles[mentor.publisher.contentType.FAULT_CODE] = function (content) {
                var diagramID, faulcode;
                faulcode = faultCodes.get(content.id);
                faulcode.type = mentor.publisher.contentType.FAULT_CODE;
                selectedSystem.set("faultCode", faulcode);
                return faulcode;
            };

            fileHandles[mentor.publisher.contentType.HARNESS] = function (content) {
                var diagramID, harnessReport;
                harnessReport = harnesses.get(content.id);
                if (!harnessReport && content.path) {

                    var Model = Backbone.Model.extend(), model = new Model();
                    content.type = mentor.publisher.contentType.HARNESS;
                    model.set(content);

                    harnessReport = model;
                }
                selectedSystem.set("harness", harnessReport);
                return harnessReport;
            };

            fileHandles[mentor.publisher.contentType.SYSTEM_REPORT] = function (content) {
                var diagramID, system, report, path, title;
                system = mentor.publisher.project.get(content.systemId);
                report = system.get(content.reportId);
                path = content.path || report.path;
                title = content.title;
                selectedSystem.set("reportId", content.reportId, {silent: true});
                selectedSystem.set("systemId", content.systemId, {silent: true});
                selectedSystem.set("reportPath", path, {silent: true});
                selectedSystem.set("reportTitle", title, {silent: true});
                selectedSystem.set("type", mentor.publisher.contentType.SYSTEM_REPORT, {silent: true});
                selectedSystem.trigger("change:reportId");
                return content;
            };

            fileHandles[mentor.publisher.contentType.CONNECTOR_FACE_VIEW] = function (content) {
                var faceView;
                if (content.get && content.get("objectId")) {

                    var preSystemId = selectedSystem.get("systemId");
                    faceView = content || {};
                    faceView.type = mentor.publisher.contentType.CONNECTOR_FACE_VIEW;

                }
                else {
                    faceView = faceviews.getFaceViewFor(content);
                }
                selectedSystem.set("faceview", faceView);
                return faceView;
            };

            fileHandles[mentor.publisher.contentType.DIAGNOSTIC] = function (content) {
                var diagnostic = "";

                var url = mentor.publisher.project.getId() + "/diagnostics/" + content.id + ".json";
                var saneUrl = Utils.prepareFilePath(url);
                $.ajax({
                    url: saneUrl,
                    async: false,
                    success: function (data, textStatus, xhr) {
                        diagnostic = data;
                    },
                    dataType: "json",
                    mimeType: "application/json"
                });
                diagnostic.id = content.id;
                selectedSystem.set("diagnostic", diagnostic);
                return diagnostic;
            };

            fileHandles[mentor.publisher.contentType.TROUBLESHOOT] = function (content) {
                var allCodes = content.activeCodes.concat(content.passiveCodes);
                require(["collections/faults", "views/troubleshoot/troubleshootPanel"], function (faults, panel) {
                    if (allCodes.length > 0) {
                        var hasValidCodes = allCodes.every(function (code) {
                            return faults.get(code)!=undefined;
                        })
                        if (!hasValidCodes) {
                            alert("Invalid codes provided to troubleshoot.");
                            return;
                        }
                    }
                    panel.render(content);
                });
            };

            fileHandles[mentor.publisher.contentType.FAULT_OBJECT_TABLE] = function (content) {
                require(["views/troubleshoot/troubleshootPanel"], function (panel) {
                    panel.render(content);
                });
            };

            fileHandles[mentor.publisher.contentType.HARNESS_LAYOUT_REPORT] = function (content) {

                return harnessLayouts.setSelectedHarnessDataToRender(content, "harnessLayoutId", content.type);
            };

            fileHandles[mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM] = function (content) {
                return harnessLayouts.setSelectedHarnessDataToRender(content, "harnessLayoutId",
                        "harnessLayoutDiagram");
            };

            mentor.publisher.fileDisplayHandler = {
                resetSystemData: function () {
                    selectedSystem.set({
                        id: "",
                        systemId: "",
                        customContent: "",
                        faceview: "",
                        faultCode: "",
                        locationView: "",
                        harness: "",
                        reportId: "",
                        path: "",
                        reportPath: "",
                        diagramId: "",
                        objectId: "",
                        diagnostic: "",
                        harnessLayoutId: "",
                        harnessLayoutDiagram: "",
                        harnessLayoutReport: "",
                    }, {
                        silent: true
                    });
                    selectedSystem.trigger("change:systemId");
                    this.resetLayoutManager();

                },

                resetLayoutManager: function () {
                    mentor.publisher.detailLayoutManager.reset();
                    mentor.publisher.contentArea.reset();
                },
                multipleDocumentRouter: multipleDocumentRouter,
                setMultipleDocumentRouter: function (router) {
                    this.multipleDocumentRouter = router;
                },

                getContentType: function (content) {
                    return content.type || (content.get && content.get("type")) ||
                            mentor.publisher.contentType.CUSTOM_VIEW;
                },
                shouldOpenInPopoutWindow: function (content) {
                    content = content || {};
                    var path = content.path || "";
                    var isAURL = path && isURLHTTPLink(path);
                    return isAURL;
                },
                openInPopout: function (content, openInTab, windowFeatures) {
                    mentor.publisher.popoutHandler.openPopout(content.path, openInTab, windowFeatures);
                },
                resettContent: function (content) {
                    if (content.reset) {
                        this.resetSystemData();
                    }
                },
                isContentHandlerAvaialable: function (content) {
                    return content.type && fileHandles[content.type];
                },
                getContentId: function (content) {
                    var listItemId = content.listItemId || content.id || content.mainText;
                    if (content.get && content.get("listItemId")) {
                        listItemId = content.get("listItemId");
                    }
                    return listItemId;
                },
                getSelectedObjectId: function (listItemId, content) {
                    var selectedObjectId = selectedSystem.get("selectedElement");
                    if (listItemId) {
                        selectedObjectId = listItemId;
                        selectedSystem.set("id", listItemId);
                    }
                    if (content.type === mentor.publisher.contentType.SYSTEM_SVG) {
                        selectedObjectId = selectedSystem.get("systemId");
                    }
                    return selectedObjectId;
                },
                updateURLHistory: function (listItemId, content) {
                    var selectedObjectId = this.getSelectedObjectId(listItemId, content);
                    var that = this;
                    setTimeout(function () {
                        that.multipleDocumentRouter.save(!content.doNotSaveAsHistory,
                                selectedSystem.get("objectId"), selectedObjectId);
                    }, 500);
                },
                viewDidDisplay: function (listItemId, content) {
                    setTimeout(this.updateURLHistory.bind(
                            this, listItemId, content), 100);
                },
                getContentDisplayHandler: function (type) {
                    return fileHandles[type];
                },
                showContent: function (content) {
                    return this.getContentDisplayHandler(content.type)(content);
                },
                displayContent: function (content) {
                    var listItemId = this.getContentId(content);
                    var view = this.showContent(content);
                    this.viewDidDisplay(listItemId, content);
                    return view;
                },
                display: function (content) {
                    var path = content && content.path;
                    content = content || {};
                    content.type = this.getContentType(content);

                    if (this.shouldOpenInPopoutWindow(content)) {
                        if (content.path.indexOf("www.") === 0) {
                            content.path = "http://" + content.path;
                        }
                        var openInTab = mentor.publisher.config["open-external-documents-in-tab"] || "";
                        this.openInPopout(content, openInTab.toLowerCase() === 'true', "noreferrer");
                    }
                    else {
                        isItACapitalReport(path, content);
                        this.resettContent(content);
                        if (this.isContentHandlerAvaialable(content)) {
                            var displayContent = this.displayContent(content);
                            if (this.shouldHighlightSearchObjects()) {
                                setTimeout(function () {
                                    this.highlightObjectsIfApplicable(content);
                                }.bind(this), 1500);
                            }
                            return displayContent;
                        }
                    }
                },

                shouldHighlightSearchObjects: function() {
                    var p = mentor.publisher;
                    var highlightSearchedObjects = p.config["highlight-searched-objects"] || '';
                    return highlightSearchedObjects.toLowerCase() === 'true';
                },

                highlightObjectsIfApplicable: function (content) {
                    var searchText = currentPackage.get("searchText");
                    var searchStrings = searchText.split(" ").filter(function (e) {
                        return !!e;
                    });

                    function getSVGHightlightStyle()
                    {
                        return {
                            fill: "red",
                            stroke: "red",
                            cursor: "default"
                        };
                    }

                    function getHtmlHighlightStyle()
                    {
                        return {
                            "background-color": "yellow",
                            "color": "red"
                        };
                    }

                    if (searchText && searchStrings.length > 0) {
                        var activeDisplayAreaId = this.getActiveDisplayAreaId();
                        var contentType = content.type || "";
                        var toUpperCase = searchText.toUpperCase();
                        var toLowerCase = searchText.toLowerCase();

                        if (contentType == mentor.publisher.contentType.SYSTEM_SVG) {
                            var object = $("#" + activeDisplayAreaId).find("object")[0];
                            var svgDoc = $(object.getSVGDocument());
                            var svgHighlightStyle = getSVGHightlightStyle();

                            var matchedElements = svgDoc.find("g > text").filter(function (idx, ele) {
                                var matchFound = false;
                                for (var idx in searchStrings) {
                                    matchFound = $(ele).text().toLowerCase().indexOf(searchStrings[idx]) >= 0;
                                    if (matchFound) {
                                        return true;
                                    }
                                }
                                return matchFound;
                            });
                            matchedElements.css(svgHighlightStyle);
                            matchedElements.siblings().css("stroke", "red");

                        }
                        else if (contentType == mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM ||
                                contentType == mentor.publisher.contentType.LOCATION_VIEWS) {
                            var object = $("#" + activeDisplayAreaId).find("object")[0];
                            var svgDoc = $(object.getSVGDocument());

                            var style = getSVGHightlightStyle();
                            var matchedElements = svgDoc.find("g > text").filter(function (idx, ele) {
                                var matchFound = false;
                                for (var idx in searchStrings) {
                                    matchFound = $(ele).text().toLowerCase().indexOf(searchStrings[idx]) >= 0;
                                    if (matchFound) {
                                        return true;
                                    }
                                }
                                return matchFound;
                            });
                            matchedElements.css(style);
                        }
                        else if (contentType == mentor.publisher.contentType.HARNESS ||
                                contentType == mentor.publisher.contentType.HARNESS_LAYOUT_REPORT ||
                                contentType == mentor.publisher.contentType.SYSTEM_REPORT ||
                                contentType == mentor.publisher.contentType.DIAGNOSTIC ||
                                contentType == mentor.publisher.contentType.CAPITAL_REPORT ||
                                contentType == mentor.publisher.contentType.FAULT_CODE) {
                            var container = $("#" + activeDisplayAreaId).find(".detailContent")[0];
                            var style = getHtmlHighlightStyle();
                            var elementsContainingText = this.getElementsContainingText(container, searchStrings);
                            elementsContainingText.css(style);

                        }
                        else if (contentType == mentor.publisher.contentType.CUSTOM_VIEW) {
                            var objectElement = $("#" + activeDisplayAreaId).find("object")[0];
                            var container = $(objectElement.contentDocument);
                            var style = getHtmlHighlightStyle();
                            var elementsContainingText = this.getElementsContainingText(container, searchStrings);
                            elementsContainingText.css(style);
                        }
                        // console.log("#### activeDisplayAreaId/contentType/searchText: "
                        //         + activeDisplayAreaId + "/" + contentType + "/" + searchText);
                    }
                    // console.log("#### activeDisplayAreaId/contentType/searchText: "
                    //         + this.getActiveDisplayAreaId() + "/" + content.type + "/" + searchText);
                },

                getElementsContainingText: function (container, searchStrings) {
                    return $(container)
                            .find("*")
                            .filter(function (idx, ele) {
                                var matchFound = false;
                                for (var idx in searchStrings) {
                                    var textMatch = $(ele).text().toLowerCase().indexOf(searchStrings[idx]) >= 0;
                                    if (textMatch) {
                                        var childrenHasText =
                                                $(ele).children().text().toLowerCase().indexOf(searchStr) >= 0;
                                        matchFound = textMatch && !childrenHasText;
                                        if (matchFound) {
                                            return true;
                                        }
                                    }
                                }
                                return false;

                                // var searchStr = searchText.toLowerCase();
                                // var containsText = $(ele).text().toLowerCase().indexOf(searchStr) >= 0;
                                // if (!containsText) {
                                //     return false;
                                // }
                                // var childrenHasText = $(ele).children().text().toLowerCase().indexOf(searchStr) >= 0;
                                // return containsText && !childrenHasText;
                            });
                },

                getActiveDisplayAreaId: function () {
                    var splitAreas = ["splitter1", "splitter2", "splitter3"];
                    for (var idx in splitAreas) {
                        var eleStyle = $("#" + splitAreas[idx]).attr("style");
                        if (eleStyle.indexOf("display: block") > -1) {
                            return splitAreas[idx];
                        }
                    }
                },

                addFileHandler: function (type, fileHandler) {
                    fileHandles[type] = fileHandler;
                }
            };

            multipleDocumentRouter.setDocumentDisplayHandler(mentor.publisher.fileDisplayHandler);
            return mentor.publisher.fileDisplayHandler;
        });