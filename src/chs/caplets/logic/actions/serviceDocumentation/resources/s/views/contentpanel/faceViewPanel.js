/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor, setTimeout, require, $, SVGEventHandler, splitPanelView, _*/
define(
        ["backbone", "underscore", "models/selectedSystem", "currentPackage",
            "views/contentpanel/toolbar/contentToolBar",
            "views/contentpanel/designReportPanel", "models/faceviews", "TranslationUtils"],
        function (Backbone, underscore, selectedSystem, currentPackage, Toolbar, DesignReportPanel, faceviews,
                TranslationUtils)
        {
            "use strict";
            var faceViewSVGEventHandler,
                    FaceViewPanel = DesignReportPanel.extend({
                        init: function ()
                        {
                            var that = this;
                            this.currentPackage.on("change:language", this.render, this);
                            this.currentPackage.on("change:vin", this.render, this);
                            this.selectedSystem.on("change:optionExpression", this.render, this);
                            this.selectedSystem.on("change:faceview", this.render, this);

                            /**
                             * This event handler remove any events registered for faceview
                             *
                             */
                            this.selectedSystem.on("closeFaceview", this.undelegateEvents, this);

                            this.selectedSystem.on("change:systemId", function ()
                            {
                                this.selectedSystem.set("faceview", "", {silent: true});
                                that.undelegateEvents();
                                that.isDiagramOpen = false;
                            }, this);
                        },

                        isPanelActive: function ()
                        {
                            return this.isReportOpen();
                        },

                        isReportOpen: function ()
                        {
                            return this.selectedSystem.get("faceview");
                        },

                        resetModel: function ()
                        {
                            this.selectedSystem.set("faceview", "", {silent: true});
                        },

                        popout: function (data)
                        {
                            var id = data && data.get("id");
                            if (id) {
                                mentor.publisher.popoutHandler.openPopout("popout.html#/faceview/" +
                                "systemId/" + data.get("objectId") + "/" +
                                "id" + id + "/" +
                                this.currentPackage.get("id").replace("\\", "/"));

                            }
                            else {
                                mentor.publisher.popoutHandler.openPopout("popout.html#/faceview/" +
                                "systemId/" + data.get("objectId") + "/" +
                                this.currentPackage.get("id").replace("\\", "/"));

                            }
                        },
                        showPopout: function ()
                        {
                            var data = this.selectedSystem.get("faceview");
                            this.popout(data);
                        },

                        showDiagramBtn: function ()
                        {
                            this.isDiagramOpen = false;
                            if (this.isFaceViewOpen()) {
                                this.toggleVisibility(".diagrams-button", false);
                                this.toggleVisibility(".reports-button", false);
                                this.toggleVisibility(".related-data-button", false);
                            }
                        },

                        isFaceViewOpen: function ()
                        {
                            return $("#systemFaceView", this.$el).length > 0;
                        },

                        enableSystemBtns: function ()
                        {
                            this.hideAllSystemButtons();
                            if (mentor.publisher.selectedSystem.get("systemId")) {
                                if (this.isDiagramOpen) {
                                    this.hideDiagramBtn();
                                }
                                else {
                                    this.showDiagramBtn();
                                }
                            }
                        },
                        addSVGZoomAndPanForSymbol: function ()
                        {
                            mentor.publisher.svgLoader('../../../../s/SVGPan.js',
                                    mentor.publisher.contentType.CONNECTOR_FACE_VIEW).loadSVGContentHTML("",
                                    "systemFaceView",
                                    faceViewSVGEventHandler);
                        },
                        processCavityTableAndSymbol: function (systemId, connId, container, symbolName, symbolExists)
                        {

                            this.systemReportLoadFinished(container, systemId);
                            this.applyLanguageFilterOnReport(container);
                            this.enableSystemBtns();
                            if (symbolExists) {
                                this.addSVGZoomAndPanForSymbol();
                            }
                        },
                        translateTableHeader: function (containerId)
                        {
                            var reportContainer = $("#" + containerId);
                            require(["TranslationUtils"], function (TranslationUtils)
                            {
                                $("table.reportTableStyle thead>tr>td", reportContainer).each(function ()
                                {
                                    TranslationUtils.replaceLanguageCodeWithTranslatedText($(this), true);
                                });
                            });
                        },

                        createZoomToolBar: function ()
                        {
                            require(["views/zoomToolBarView"], function (ZoomToolBarView)
                            {
                                var zoomToolBar =
                                        new ZoomToolBarView({
                                            el: $('#systemFaceView'),
                                            handler: faceViewSVGEventHandler
                                        });
                                zoomToolBar.render();
                            });
                        },
                        setData: function (faceview, config)
                        {
                            this.selectedSystem.set("faceview", faceview, config);
                        },
                        getData: function ()
                        {
                            return this.selectedSystem.get("faceview");
                        },
                        isFaceViewDataAvailable: function ()
                        {
                            return this.getData() &&
                                    this.getData().get("symbol");
                        },
                        getFVSymbolPath: function (p, connectorTable, data)
                        {
                            if (data.get("multiple-faceview-support")) {
                                return data.get("path");
                            }
                            else {
                                return p.dataLoader.getFaceViewSymbol(connectorTable,
                                        data.get("systemId"), p.project.getId());
                            }

                        },
                        getCavityTablePath: function (p, connectorTable, data)
                        {
                            if (data.get("multiple-faceview-support")) {
                                return data.get("cavityTable");
                            }
                            else {
                                return p.dataLoader.getCavityTable(connectorTable, data.get("systemId"),
                                                p.project.getId()) || "";
                            }
                        },
                        getSymbolName: function (data)
                        {
                            if (data.get("multiple-faceview-support")) {

                                return data.get("mainText");
                            }
                            else {

                                return data.get("symbol");
                            }

                        },
                        renderToolbar: function (title, data)
                        {
                            return this.toolBar.render({
                                type: mentor.publisher.contentType.SYSTEM_REPORT,
                                isSystem: true,
                                title: title,
                                systemId: data.get("systemId")
                            }).$el;
                        },
                        enableViewButton: function ()
                        {
                            this.toolBar.enableFaceViewsNavigation({
                                faceViewSymbolHandler: this
                            });
                        },
                        createToolBar: function (title, data)
                        {

                            this.toolBar = new Toolbar();
                            this.$el.append(this.renderToolbar(title, data));
                            if (this.enableFaceViewSymbolNavigation(this.getData())) {
                                this.enableViewButton();
                            }
                        },
                        showFaceViews: function (evt)
                        {
                            var views = this.getData().get("faceviews");
                            if (views) {
                                var x = evt.pageX;
                                var y = evt.pageY;

                                var Backbone = require("backbone");
                                var popoverFilterModel = new (Backbone.Model.extend({}))();
                                this.getPopOverObject().showPopover(this.getData().get("name"), x, y, false,
                                        popoverFilterModel);
                                this.addFaceViewSymbolViewEntry(views, popoverFilterModel);
                            }
                        },
                        findFaceView: function (faceviews, clickedItem)
                        {
                            return _.find(faceviews, function (fv)
                            {
                                return fv.id === clickedItem.get("id");
                            });
                        }, changeSymbol: function (faceviewsymbolInfo)
						{
							mentor.publisher.fileDisplayHandler.display(faceviewsymbolInfo);
						}, showSymbolByView: function (evt, clickedItem)
                        {
                            var faceviews = this.getData().get("faceviews");
                            var selectedSymbol = this.findFaceView(faceviews, clickedItem);
                            if (selectedSymbol) {

                                this.getData().set("id", selectedSymbol.id);
                                this.getData().set("faceviewId", selectedSymbol.id);
                                this.getData().set("path", selectedSymbol.path);
                                this.getData().set("view", selectedSymbol.mainText);
                                if (selectedSymbol.mainText) {

                                    this.getData().set("mainText",
                                            this.getData().get("name") + " " + this.getData().get("separator") + " " +
                                            selectedSymbol.mainText);
                                }
                                else {
                                    this.getData().set("mainText",
                                            this.getData().get("name"));
                                }
								this.changeSymbol(this.getRequiredFaceViewAttributes(this.getData().attributes));
								// this.setData(faceviews, {silent: true});
                                // this.render();
                                //this.changeSymbol(path);

                            }

                        },
                        changeTitle: function (title)
                        {
                            $(".component-label", this.$el).html(title);
                        },

                        onPopout: function (event, clickedItem)
                        {
                            var faceviews = this.getData().get("faceviews");
                            var selectedSymbol = this.findFaceView(faceviews, clickedItem);
                            var Model = Backbone.Model.extend({}), faceviewModel = new Model();
                            if (selectedSymbol) {
                                faceviewModel.set(selectedSymbol);
                                faceviewModel.set("objectId", this.getData().get("objectId"));
                                this.popout(faceviewModel);
                            }
                        },
                        getPopOverObject: function ()
                        {
                            return mentor.publisher.designObjectPopover;
                        },
                        addFaceViewSymbolViewEntry: function (faceviews, popoverFilterModel)
                        {
                            var views = [], config = {
                                expand: true,
                                showPopoutBtn: true,
                                async: true,
                                onMouseClick: this.showSymbolByView.bind(this),
                                onPopout: this.onPopout.bind(this)
                            };
                            views = /*this.getAllViews(*/faceviews/*)*/ || [];
                            var that = this;
                            setTimeout(function ()
                            {
                                that.getPopOverObject().addSection(mentor.publisher.languageTranslator.localize("FaceViewButtonTitle"),
                                        views, popoverFilterModel, config);
                            }, 100);

                        },
                        enableFaceViewSymbolNavigation: function (faceview)
                        {
                            if (faceview && faceview.get && faceview.get("multiple-faceview-support")) {
                                var faceviews = faceview.get("faceviews");
                                faceviews = faceviews || [];
                                var views = /*this.getAllViews(*/faceviews/*)*/ || [];
                                return views.length > 1;
                            }
                        },

                        addZoomAndPanSupport: function (data, connectorTable, symbolName, symbolExists)
                        {
                            var that = this;
                            setTimeout(function ()
                            {
                                that.addAutomationClasses();
                                that.processCavityTableAndSymbol(data.get("systemId"),
                                        connectorTable.replace(".html", ""), "tableLoadArea",
                                        symbolName, symbolExists);
                                that.createZoomToolBar();
                                that.notify();

                            }, 10);
                        },
                        clearExistingContent: function (data)
                        {
                            mentor.publisher.contentArea.closeExistingPanel({
                                type: mentor.publisher.contentType.CONNECTOR_FACE_VIEW,
                                systemId: data.get("systemId")
                            }, this);
                            this.setElement(this.container);
                            this.$el.html('');

                        },
                        getTitle: function (symbolName, faceView, data)
                        {
                            return splitPanelView.getFaceViewWindowTitle(symbolName.replace(".svg",
                                    ""), faceView.get("objectId"), data.get("systemId"));
                        },
                        compileHTMLTemplate: function ()
                        {
                            return underscore;
                        },
                        generateHTML: function (html, symbolFilePath, symbolExists)
                        {
                            return this.compileHTMLTemplate().template(this.templateHTML)({
                                cavityTableHTML: html,
                                path: Utils.prepareFilePath(symbolFilePath),
                                symbolExists: symbolExists
                            });
                        },
                        getRequiredFaceViewAttributes: function (attributes)
                        {
                            var faceviewcontent = {};
                            faceviewcontent.type = mentor.publisher.contentType.CONNECTOR_FACE_VIEW;
                            faceviewcontent.systemId = attributes.systemId;
                            faceviewcontent.objectId = attributes.objectId;
                            faceviewcontent.id = attributes.id;
                            if (attributes["multiple-faceview-support"]) {
                                faceviewcontent.viewId = attributes.view;
                            }
                            faceviewcontent.title = attributes.mainText;
                            return faceviewcontent;
                        }, showSymbol: function (template, data, title, connectorTable, symbolName, symbolExists)
                        {
                            this.$el.append(template);
                            var allFaceViewAttributes = data.attributes;
                            var faceviewcontent = this.getRequiredFaceViewAttributes(allFaceViewAttributes);
                            mentor.publisher.contentArea.layoutContentPanel(faceviewcontent);
                            faceViewSVGEventHandler =
                                    faceViewSVGEventHandler || new SVGEventHandler();
                            this.addZoomAndPanSupport(data, connectorTable, symbolName, symbolExists);
                        },
                        prepareContentPanelAndRenderSymbol: function (symbolName, faceView, data, html, symbolFilePath,
                                symbolExists, connectorTable)
                        {
                            var template, title = this.getTitle(symbolName, faceView, data);
                            this.clearExistingContent(data);
                            template = this.generateHTML(html, symbolFilePath, symbolExists);
                            this.createToolBar(title, data);
                            this.showSymbol(template, data, title, connectorTable, symbolName,
                                    symbolExists);
                        },
                        getAMDLoader: function ()
                        {
                            return require;
                        },
                        loadCavityTableHTMLAndRenderPanel: function (cavityWireTable, that, symbolName, faceView, data,
                                symbolFilePath, symbolExists, connectorTable)
                        {
                            this.getAMDLoader()(["text!" + "../" + Utils.prepareFilePath(cavityWireTable)],
                                    function (html)
                                    {
                                        that.prepareContentPanelAndRenderSymbol(symbolName, faceView, data, html,
                                                symbolFilePath, symbolExists, connectorTable);
                                    });
                        },
                        renderCavityTableAndSymbol: function (cavityWireTable, symbolName, faceView, data,
                                symbolFilePath,
                                symbolExists, connectorTable)
                        {
                            var that = this;
                            if (cavityWireTable) {
                                this.loadCavityTableHTMLAndRenderPanel(cavityWireTable, that, symbolName, faceView,
                                        data,
                                        symbolFilePath, symbolExists, connectorTable);
                            }
                        },
                        render: function ()
                        {
                            var faceView, symbolExists, p = mentor.publisher, data, symbolName, connectorTable, symbolFilePath, cavityWireTable;
                            if (this.isFaceViewDataAvailable()) {

                                faceView = this.selectedSystem.get("faceview");
                                data = faceView;
                                symbolName = this.getSymbolName(data);
                                symbolExists = data.get("path");
                                connectorTable = data.get("cavityTable");
                                symbolFilePath = this.getFVSymbolPath(p, connectorTable, data);
                                //this.selectedSystem.set("systemId", data.get("systemId"), {silent: true});
                                cavityWireTable =
                                        this.getCavityTablePath(p, connectorTable, data);
                                this.renderCavityTableAndSymbol(cavityWireTable, symbolName, faceView, data,
                                        symbolFilePath, symbolExists, connectorTable);

                            }

                            return this;
                        },
                        notify: function ()
                        {
                            FaceViewPanel.__super__.notify.apply(this, {});
                            this.selectedSystem.trigger("closeSystemReport");
                        },
                        addAutomationClasses: function ()
                        {
                            this.$("table").addClass("auto-cavity-chart");
                            this.$(".auto-cavity-chart thead>tr").addClass("auto-cavity-chart-header");
                            this.$(".auto-cavity-chart tbody>tr").addClass("auto-cavity-chart-row");
                        },
                        translateTableCells: function (container) {
                            $("td", container).each(function () {
                                if ($(this).data("disable-translation") === true) {
                                    return;
                                }
                                TranslationUtils.replaceLanguageCodeWithTranslatedText($(this), true);
                            });
                        },
                    });

            return new FaceViewPanel();
        }
);