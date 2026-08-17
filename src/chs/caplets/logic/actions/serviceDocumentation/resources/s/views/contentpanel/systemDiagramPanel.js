/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, setTimeout, $, SVGEventHandler, require, LoadMask, Utils*/
define(
        ["backbone", "underscore", "models/selectedSystem", "currentPackage",
            "views/contentpanel/toolbar/contentToolBar"],
        function (Backbone, underscore, selectedSystem, currentPackage, ToolBar)
        {
            "use strict";
            var svgLoader, SystemDiagramPanel, handler;
            SystemDiagramPanel = Backbone.View.extend({
                systemData: "",
                doNotLoadOnStart: true,
                subscribedEventType: "systemReport",
                publishedEventType: "systemDiagram",

                getCloseEvent: function (type)
                {
                    return type + "Closed";
                },
                getOpenEvent: function (type)
                {
                    return type + "Opened";
                },

                renderOnLanguageChange: function (model, value, options) {
                    if (options && options.fromSettingsPanel) {
                        return;
                    }
                    this.render();
                },
                initialize: function ()
                {
                    var that = this;
                    currentPackage.on("change:language", this.renderOnLanguageChange, this);
                    selectedSystem.on("change:diagramId", this.render, this);
                    selectedSystem.on("closeSystemDiagram", this.close, this);
                    selectedSystem.on("change:optionExpression", this.render, this);
                    currentPackage.on("change:vin", this.render, this);
                    selectedSystem.on("change:systemId", function ()
                    {
                        //reset all state data
                        that.$el.html('');
                        that.undelegateEvents();
                        this.isReportOpen = false;
                        that.isDiagramOpen = false;
                    }, this);

                    selectedSystem.on(this.getCloseEvent(this.subscribedEventType), this.showReportBtn, this);
                    selectedSystem.on(this.getOpenEvent(this.subscribedEventType), this.hideReportBtn, this);
                },

                showReportBtn: function ()
                {
                    this.isReportOpen = false;
                    if (this.toolBar && this.toolBar.isReportsBtnActive()) {
                        $(".reports-button", this.$el).show();
                    }

                    if (this.toolBar && this.toolBar.isDocumentTypeActive()) {
                        $(".diagrams-button", this.$el).show();
                    }
                    $(".related-data-button", this.$el).show();

                },

                hideReportBtn: function ()
                {
                    this.isReportOpen = true;
                    $(".reports-button", this.$el).hide();
                    $(".related-data-button", this.$el).show();
                    if (this.toolBar && this.toolBar.isDocumentTypeActive()) {
                        $(".diagrams-button", this.$el).show();
                    }
                },

                events: {
                    "click .popOutBtn": "showPopout",
                    "click .closeBtn": "close"
                },

                close: function ()
                {
                    this.$el.html('');
                    selectedSystem.set("diagramId", "", {silent: true});
                    this.isDiagramOpen = false;
                    selectedSystem.trigger(this.getCloseEvent(this.publishedEventType));
                    mentor.publisher.detailLayoutManager.refreshContentToolbars();
                },

                getPopoutURL: function ()
                {
                    var p = mentor.publisher;
                    return p.popoutHandler.createURL({
                        type: p.contentType.SYSTEM_SVG,
                        systemId: this.systemData.get("systemId"),
                        objectId: selectedSystem.get("objectId"),
                        diagramId: this.systemData.get("diagramId"),
                        projectId: currentPackage.get("id").replace("\\", "/")
                    });
                }, showPopout: function (event)
                {
                    mentor.publisher.popoutHandler.openPopout(this.getPopoutURL());

                },

                addZoomAndPanEventHandlers: function (containerId)
                {
                    if (!svgLoader) {
                        svgLoader = mentor.publisher.svgDetailPanel();
                    }

                    if (!this.svgLoader) {
                        this.svgLoader = mentor.publisher.svgLoader("", mentor.publisher.contentType.SYSTEM_SVG);
                    }
                    svgLoader.loadSVG("", containerId, this.svgLoader, handler);
                    this.enableReports();
                },
                enableReports: function ()
                {
                    if (this.isReportOpen) {
                        this.hideReportBtn();
                    }
                    else {
                        this.showReportBtn();
                    }
                },
                getSystemData: function ()
                {
                    return this.systemData;
                },

                createToolBar: function ()
                {
                    var systemData = this.getSystemData();
                    this.toolBar = new ToolBar();
                    this.toolBar.render({
                        type: mentor.publisher.contentType.SYSTEM_SVG,
                        isSystem: true,
                        title: systemData.get("title"),
                        systemId: systemData.get("systemId"),
                        computeTitle: selectedSystem.get("computeTitle")
                    });
                    this.$el.append(this.toolBar.$el);
                    return this.toolBar;
                },

                generateHTMLUsingTemplate: function ()
                {
                    var template, systemData = this.getSystemData();
                    template = underscore.template(this.templateHTML)({
                        path: Utils.prepareFilePath(systemData.get("path")),
                        systemId: systemData.get("systemId")
                    });
                    return template;
                },

                createZoomToolBar: function (containerId)
                {
                    var that = this;
                    require(["views/zoomToolBarView"], function (ZoomToolBarView)
                    {
                        var zoomToolBar;
                        handler = handler || new SVGEventHandler();
                        handler.svgContainerId = containerId;
                        zoomToolBar = new ZoomToolBarView({el: $('#' + containerId), handler: handler});
                        zoomToolBar.render();
                    });
                },

                relayoutContentPanel: function (isContentOpen)
                {
                    mentor.publisher.contentArea.layoutContentPanel({
                        systemId: this.getSystemData().get("systemId"),
                        diagramId: this.getSystemData().get("diagramId"),
                        title: this.getSystemData().get("title"),
                        type: mentor.publisher.contentType.SYSTEM_SVG
                    }, isContentOpen);
                },

                getContentType: function ()
                {
                    return this.getSystemData().get("type");
                },

                processDisplayedDocument: function (containerId)
                {
                    var that = this;
                    //$("object", this.$el).width("1px").height("1px");
                    //wait for content to get updated in DOM
                    setTimeout(function ()
                    {

                        that.relayoutContentPanel(that.isDiagramOpen);
                        mentor.publisher.detailLayoutManager.resetContentPanel();
                        LoadMask.LoadSVGMask(that.container);
                        that.addZoomAndPanEventHandlers(containerId);
                        that.createZoomToolBar(containerId);
                        //$("object", this.$el).width("100%").height("100%");
                        //notify others that system is opened
                        that.notify();
                    }, 100);
                },

                isDiagramDataPresent: function ()
                {
                    return this.getSystemData() && this.getSystemData().get("diagramId");
                },

                removePreviousDiagram: function ()
                {
                    $(".detailContent", this.$el).remove();
                },
                updateTitle: function (title)
                {
                    var computeTitle = this.getSystemData().get("computeTitle");
                    var currentLang = currentPackage.get("language");
                    var translatedTitle = computeTitle(currentLang);
                    $(".component-label", this.$el).html(translatedTitle);
                },

                render: function ()
                {
                    var that = this, containerId = "systemSVGLoadArea", svgContentHTML;
                    this.systemData = selectedSystem.clone();
                    if (this.isDiagramDataPresent()) {
                        // backbone.history.navigate(this.getPopoutURL(), {trigger:false});
                        this.setElement(this.container);

                        if (!this.isDiagramOpen) {
                            //clear existing content if any
                            mentor.publisher.contentArea.closeExistingPanel(this.getSystemData(), this);

                            //create tool bar
                            this.createToolBar();
                        }
                        else {
                            this.updateTitle(this.getSystemData().get("title"));
                            this.removePreviousDiagram();
                        }

                        //get the HTML content using template
                        svgContentHTML = this.generateHTMLUsingTemplate();

                        this.$el.append(svgContentHTML);

                        this.processDisplayedDocument(containerId);

                        return this;
                    }
                },

                notify: function ()
                {
                    this.isDiagramOpen = true;
                    selectedSystem.trigger(this.getOpenEvent(this.publishedEventType));
                }
            });

            return new SystemDiagramPanel();
        }
);
