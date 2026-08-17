/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor,  crossHighlightHandler, require, Backbone*/
define([], function () {
    "use strict";
    var p = mentor.publisher;
    return {
        setDocumentDisplayHandler: function (documentHandler) {
            this.documentHandler = documentHandler;
        },
        extractDocData: function (data) {
            return JSON.parse(data);

        },
        displayDocuments: function (args) {
            var that = this;
            var doc1 = this.extractDocData(args[2]);
            doc1.reset = true;
            this.hidepopover();
            this.handleSingleDocument(doc1);
            if (args.length >= 4) {
                setTimeout(function () {
                    that.handleSingleDocument(that.extractDocData(args[3]));
                }, 100);
            }
            if (args.length >= 5) {
                setTimeout(function () {
                    that.handleSingleDocument(that.extractDocData(args[4]));
                }, 100);
            }
        },
        objectHighlighter: crossHighlightHandler,

        setObjectHighlighter: function (objectHighlighter) {
            this.objectHighlighter = objectHighlighter;
        },
        hidepopover: function () {
            p.eventDispatcher.dispatchEvent(p.events.CLOSE_POPOVER);
        },

        highlightObject: function (objectId) {
            var that = this;
            if (objectId && objectId !== null && objectId !== "null" && that.objectHighlighter &&
                    that.objectHighlighter.initCrossHighlight) {

                setTimeout(function () {
                    that.objectHighlighter.initCrossHighlight(objectId);

                    require(["models/selectedSystem", "SignalTracerModel"],
                            function (selectedSystem, signalTraceModel) {
                                var systemId = selectedSystem && selectedSystem.get("systemId");
                                var objectId = selectedSystem && selectedSystem.get("objectId");
                                signalTraceModel.updateData(systemId, objectId);
                            });
                }, 1000);

            }
        },
        selectedSystem: "",
        setSelectedSystem: function (system) {
            this.selectedSystem = system;
        },
        resetObjectId: function (objectId) {
            if (this.selectedSystem) {

                this.selectedSystem.set("objectId", "", {silent: true});
            }
            else {
                require(["models/selectedSystem"], function (selectedSystem) {
                    selectedSystem.set("objectId", "", {silent: true});
                });
            }

        },
        setSelectedDocument: function (selectedDocumentId) {
            var that = this;
            setTimeout(function () {
                if (that.selectedSystem) {
                    that.selectedSystem.set("id", selectedDocumentId, {silent: true});
                }

                else {
                    require(["models/selectedSystem"], function (selectedSystem) {
                        selectedSystem.set("id", selectedDocumentId);
                    });
                }
            }, 10);

        },
        resetViews: function () {

        },
        render: function (objectId, selectedDocumentId) {
            objectId = decodeURIComponent(objectId);
            selectedDocumentId = decodeURIComponent(selectedDocumentId);
            if (this.documentHandler && this.documentHandler.display && arguments.length >= 3) {
                this.resetViews();
                this.resetObjectId(objectId);
                this.displayDocuments(arguments);
                this.highlightObject(objectId);
                this.setSelectedDocument(selectedDocumentId);
            }
            return true;
        },
        handleSingleDocument: function (documentObj) {
            documentObj = documentObj || {};
            documentObj.doNotSaveAsHistory = true;
            this.documentHandler.display(documentObj);
        },
        contentArea: mentor.publisher.contentArea,
        setContentArea: function (contentArea) {
            this.contentArea = contentArea;
        },
        history: "",
        converToJSONString: function (content) {
            return JSON.stringify(content);
        }, createURL: function (allOpenContent, URI) {
            for (var property in allOpenContent) {
                if (allOpenContent.hasOwnProperty(property)) {
                    URI = URI + "/" + encodeURIComponent(this.converToJSONString(allOpenContent[property]));
                }
            }
            return URI;
        },
        appendURLSegment: function (URI, segmentObject, segmentPrefix) {
            segmentPrefix = segmentPrefix || "";
            if (!segmentObject) {
                segmentObject = "";
            }
            URI = URI + segmentPrefix + encodeURIComponent(segmentObject);
            return URI;
        },
        shouldReplaceExistingURL: function (selectedDocument, options) {
            if (window.location.href.endsWith(".html") || !selectedDocument) {
                options.replace = true;
            }

            if (!selectedDocument) {
                selectedDocument = this.selectedView;
            }
            this.selectedView = selectedDocument;
            return selectedDocument;
        },
        createURLAndStoreAsHistory: function (objectId, selectedDocument) {
            var allOpenContent = this.contentArea.getAllOpenContentDetails();
            var URI = 'document_views';

            var options = {trigger: false};

            selectedDocument = this.shouldReplaceExistingURL(selectedDocument, options);
            var userSession = require("UserSession");
            URI = this.appendProjectId(URI);
            var effectivityRange = "";
            var parentProjectId = "";
            if (userSession) {
                var selectedSubPackage = userSession.getActiveSession().get(userSession.kSelectedPackageProperty);
                effectivityRange = selectedSubPackage && selectedSubPackage.get("effectivityRange");
                parentProjectId = selectedSubPackage && selectedSubPackage.get("projectId");

            }
            URI = this.appendURLSegment(URI, effectivityRange, "/effRange");
            URI = this.appendURLSegment(URI, parentProjectId, "/projId");
            URI = this.appendURLSegment(URI, objectId, "/objectId");
            URI = this.appendURLSegment(URI, selectedDocument, "/curDoc");
            URI = this.createURL(allOpenContent, URI);
            if (!this.history) {
                this.history = Backbone.history;
            } else {
                Utils.resetUrlParams();
            }
            this.history.navigate(URI, options);
        },
        appendProjectId: function (URI) {
            var projectId = p.project.getId();
            if (projectId) {
                return URI + "/" + projectId.replace("\\", "/");
            }
            return URI;
        },
        save: function (storeAsHistory, objectId, selectedDocument) {

            var that = this;
            setTimeout(function () {
                if (storeAsHistory && !Utils.isPopoutWindow()) {
                    that.createURLAndStoreAsHistory(objectId, selectedDocument);
                }

            }, 100);
        },
        setHistoryTracker: function (historyObject) {
            this.history = historyObject;
        }
    };
});
