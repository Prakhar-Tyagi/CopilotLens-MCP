/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global SVGEventHandler, mentor, getWindowObj, Utils, $, define, require, LoadMask*/
define(
        [
            "underscore",
            "views/contentpanel/SVGContentPanel",
            "currentPackage",
            "models/selectedSystem",
            "preferences",
            "views/contentpanel/toolbar/LayoutButtons",
            "harnessLayouts",
            "harnessLayoutBarHandler"
        ],
        function (_, SVGContentPanel, currentPackage, selectedSystem, preferences, LayoutButtons, harnessLayouts,
                HarnessLayoutBarHandler)
        {
            "use strict";

            var DocumentSetsContentPanel,
                    handler,
                    svgLoader,
                    p = mentor.publisher;

            DocumentSetsContentPanel = SVGContentPanel.extend({
                LayoutButtons: LayoutButtons,
                harnessLayoutBarHandler: new HarnessLayoutBarHandler(),

                subscribedEventType: "harnessReport",
                publishedEventType: "harnessDiagram",

                bindEvents: function ()
                {

                },

                init: function ()
                {
                    selectedSystem.on("change:" + this.getContentType(), function ()
                    {
                        this.updateSystemData();
                        this.render();
                    }, this);
                    preferences.on("change:language", this.updateTitle, this);
                },

                createZoomToolBar: function (containerId)
                {
                    var that = this;
                    require(["views/zoomToolBarView"], function (ZoomToolBarView)
                    {
                        var zoomToolBar;
                        that.handler = /*handler ||*/ new SVGEventHandler();
                        zoomToolBar = new ZoomToolBarView({el: $('#' + containerId), handler: that.handler});
                        zoomToolBar.render();
                        $('object', that.$el).width("100%").height("100%");
                    });
                },

                addZoomAndPanEventHandlers: function (containerId)
                {
                    if (!svgLoader) {
                        svgLoader = p.svgDetailPanel();
                    }
                    if (!this.svgLoader) {
                        this.svgLoader =
                                p.svgLoader("", this.getContentType());
                    }
                    svgLoader.loadSVG("", containerId, this.svgLoader, this.handler);
                },

                designType: "harnessLayoutDiagram",

                resetData: function ()
                {
                    selectedSystem.unset(this.designType);
                },
                hasData: function ()
                {

                },
                close: function ()
                {
                    DocumentSetsContentPanel.__super__.close.apply(this, arguments);
                    this.resetData();
                },

                clearContent: function ()
                {
                    this.close();
                },

                enableArtifact: function (type)
                {
                    if (this.toolBar) {
                        this.isReportActive = false;

                        this.toolBar.layoutButtons.enableDocumentSets([".reports-button"]);
                        this.deActivateInValidDocuments();
                    }
                },

                disableArtifact: function (type)
                {
                    this.isReportActive = true;
                    this.deActivateInValidDocuments();
                },

                deActivateInValidDocuments: function ()
                {
                    if (this.toolBar) {
                        this.toolBar.layoutButtons.disableDocumentSets(
                                ['.related-data-button'/*, ".renderConnectivityBtn"*/]);
                        if (!this.isDocumentTypeActive("diagrams")) {
                            this.toolBar.layoutButtons.disableDocumentSets([".diagrams-button"]);
                        }

                        if (!this.isDocumentTypeActive("reports") || this.isReportActive) {
                            this.toolBar.layoutButtons.disableDocumentSets([".reports-button"]);
                        }

                    }
                }, afterContentDisplayed: function (containerId)
                {

                    this.addZoomAndPanEventHandlers(containerId);
                    this.createZoomToolBar(containerId);
                    this.deActivateInValidDocuments();
                    DocumentSetsContentPanel.__super__.afterContentDisplayed.apply(this, arguments);
                },

                showPopout: function (event)
                {
                    if (!this.getDocumentSet()) {
                        return;
                    }
                    var layoutId,
                            diagramId,
                            projectId,
                            objectId;

                    event.stopPropagation();

                    layoutId = this.getDocumentSet().id;
                    diagramId = this.getSystemData().id;
                    objectId = selectedSystem.get("objectId");
                    projectId = currentPackage.get("id").replace("\\", "/");

                    p.popoutHandler.openPopout(
                            "popout.html#/" +
                            this.getContentType().toLowerCase() + "/" +
                            layoutId + "/" +
                            diagramId + "/" +
                            projectId + "/" + objectId
                    );
                },

                isDataAvailable: function ()
                {
                    return selectedSystem.has(this.designType);
                }, getData: function ()
                {
                    return selectedSystem.get(this.designType);
                }, updateSystemData: function ()
                {
                    if (this.isDataAvailable()) {
                        this._diagram = this.getData().clone();
                        this.updateTitle();
                    }
                    else {
                        this._diagram = null;
                    }
                },

                getSystemData: function ()
                {
                    return this._diagram;
                },

                getContentType: function ()
                {
                    return p.contentType.HARNESS_LAYOUT_DIAGRAM;
                },

                getDocumentTitle: function (documentSet)
                {
                    return documentSet.get("mainText");
                }, updateTitle: function ()
                {
                    if (this.getSystemData()) {
                        var diagramTitle = this.getSystemData().get("mainText"),
                                layoutTitle = this.getDocumentTitle(this.getDocumentSet()),
                                title;
                        if (getWindowObj().diagramAsSystemsObjectFactoryImpl) {
                            title = Utils.translate(layoutTitle);
                        }
                        else {
                            title = Utils.translate(layoutTitle) + ", " + Utils.translate(diagramTitle);
                        }
                        this.getSystemData().set("title", title);
                    }
                },

                getTitle: function ()
                {
                    var diagram = this.getSystemData();
                    return diagram.get("title");
                },

                getDataId: function ()
                {
                    return selectedSystem.get("harnessLayoutId");
                }, getDocumentSet: function ()
                {
                    var diagram,
                            layoutId;

                    diagram = this.getData();
                    if (diagram) {
                        layoutId = diagram.get("layoutId") || this.getDataId();
                        return harnessLayouts.get(layoutId);
                    }
                },

                getDocumentContainer: function ()
                {
                    var conatainer = this.container;
                    return $(conatainer + " .panel_content").attr('id');
                },
                getDocumentType: function ()
                {
                    return "diagrams";
                },

                isDocumentTypeActive: function (documentType)
                {
                    documentType = documentType || "diagrams";
                    var limit = 0;
                    if (this.getDocumentType() === documentType) {
                        limit = 1;
                    }
                    var documents, documentSet = this.getDocumentSet();

                    if (documentSet) {

                        documents = documentSet.getDocumentsInGroupTitled(documentType);
                        if (documents && _.size(documents) > 0) {
                            return _.size(documents) > limit;
                        }
                    }

                },

                getToolBarContent: function ()
                {
                    var systemData = this.getSystemData();
                    var handler = new HarnessLayoutBarHandler();
                    handler.setContentType(this.getContentType());
                    handler.setDataId(this.getDataId());
                    return {
                        forDiagramPanel: true,
                        handler: handler,
                        type: this.getContentType(),
                        layout: this.getDocumentSet(),
                        title: this.getTitle()
                    };
                },
                getDiagramPath: function (diagram) {
                    var path = diagram.get("path");
                    if(window.heavySVGs) {
                        var lastIndexOfSepa = path.lastIndexOf("/");
                        if(path.indexOf("\\") >= 0) {
                            lastIndexOfSepa = path.lastIndexOf("\\");
                        }
                        return path.substr(0, lastIndexOfSepa) + "/optimized_" + path.substr(lastIndexOfSepa + 1);
                    }
                    return path;
                },

                getDataToRender: function (content)
                {
                    var diagram = this.getData();
                    if (diagram) {
                        var path = this.getDiagramPath(diagram);
                        return {
                            path:path,
                            diagram: diagram,
                            type: this.getContentType()
                        };
                    }
                },

                beforeContentDisplay: function ()
                {
                    if (p.detailLayoutManager.isPanelOpen(p.detailLayoutManager.getPanelId(this.getContentType()))) {
                        //clear existing content if any
                        p.contentArea.closeExistingPanel({type: this.getContentType()}, this);

                    }
                    LoadMask.LoadSVGMask(this.container);
                }

            });

            return DocumentSetsContentPanel;
        }
);