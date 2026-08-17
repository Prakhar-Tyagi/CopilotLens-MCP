/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, mentor, setTimeout, $, SVGEventHandler, require, window*/
define(
        ["backbone", "underscore", "models/selectedSystem", "currentPackage",
            "views/contentpanel/toolbar/contentToolBar"],
        function (Backbone, underscore, selectedSystem, currentPackage, ToolBar)
        {
            "use strict";
            var svgLoader, DocumentDisplayPanel, handler;
            DocumentDisplayPanel = Backbone.View.extend({

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

                clearContent: function ()
                {
                    //reset all state data
                    this.$el.html('');
                    this.undelegateEvents();
                },

                bindEvents: function ()
                {
                    currentPackage.on("change:language", this.render, this);
                    selectedSystem.on("change:optionExpression", this.render, this);
                    currentPackage.on("change:vin", this.render, this);
                    selectedSystem.on("change:systemId", this.clearContent, this);
                }, initialize: function ()
                {
                    var that = this;
                    this.bindEvents();

                    selectedSystem.on(this.getCloseEvent(this.subscribedEventType),
                            this.enableArtifact, this);
                    selectedSystem.on(this.getOpenEvent(this.subscribedEventType),
                            this.disableArtifact, this);
                    this.init();
                },

                enableArtifact: function (type)
                {
                    this['' + this.subscribedEventType + ''] = true;
                },

                disableArtifact: function (type)
                {
                    this['' + this.subscribedEventType + ''] = false;
                },

                isArtifactEnable: function (type)
                {
                    return this['' + type + ''];
                },

                init: function ()
                {

                },
                isPanelOpen: function ()
                {
                },

                events: {
                    "click .popOutBtn": "showPopout",
                    "click .closeBtn": "close"
                },

                publishEvents: function ()
                {
                    this['' + this.publishedEventType + ''] = false;
                    selectedSystem.trigger(this.getCloseEvent(this.publishedEventType), {id: this.getDataId()});
                }, closePanel: function ()
                {
                    this.$el.html('');
                    mentor.publisher.detailLayoutManager.refreshContentToolbars();
                    this.publishEvents();
                }, close: function ()
                {
                    this.closePanel();
                    this.onClose();
                },
                onClose: function ()
                {
                    
                },

                showPopout: function (event)
                {
                    event.stopPropagation();
                },

                getSVGLoader: function ()
                {
                    this.svgLoader = this.svgLoaded || mentor.publisher.svgLoader();
                    return this.svgLoader;
                },

                addZoomAndPanEventHandlers: function (containerId)
                {
                    if (!svgLoader) {
                        svgLoader = mentor.publisher.svgDetailPanel();
                    }
                    svgLoader.loadSVG("", containerId, this.getSVGLoader(), handler);
                },
                getSystemData: function ()
                {
                    return selectedSystem;
                },

                getContentToDisplay: function ()
                {
                    return selectedSystem;
                },

                getContentType: function ()
                {
                    return mentor.publisher.contentType.SYSTEM_SVG;
                },

                getTitle: function ()
                {
                    var systemData = this.getSystemData();
                    return systemData.get("title");
                },

                showDocumentsBtns: function ()
                {
                    return true;
                },

                getToolBarContent: function ()
                {
                    var systemData = this.getSystemData();
                    return {
                        type: this.getContentType(),
                        isSystem: this.showDocumentsBtns(),
                        title: this.getTitle(),
                        systemId: systemData.get("systemId")
                    };
                },

                createToolBar: function ()
                {
                    var toolBar = new ToolBar(),
                            systemData = this.getSystemData();

                    if (this.LayoutButtons) {
                        toolBar.LayoutButtons = this.LayoutButtons;
                    }
                    toolBar.render(this.getToolBarContent());
                    this.$el.append(toolBar.$el);
                    return toolBar;
                },

                generateHTMLUsingTemplate: function (content)
                {
                   /* content.set("path", Utils.prepareFilePath(content.get("path")));*/
                    this.$el.append(underscore.template(this.templateHTML)(content));
                },

                createZoomToolBar: function (containerId)
                {
                    var that = this;
                    require(["views/zoomToolBarView"], function (ZoomToolBarView)
                    {
                        var zoomToolBar;
                        handler = /*handler || */new SVGEventHandler();
                        zoomToolBar = new ZoomToolBarView({el: $('#' + containerId), handler: handler});
                        zoomToolBar.render();
                        $('object', that.$el).width("100%").height("100%");
                    });
                },

                relayoutContentPanel: function (isPanelOpen)
                {
                    mentor.publisher.contentArea.layoutContentPanel(this.getSystemData(), isPanelOpen);
                },

                isPopoutWindow: function ()
                {
                    return window.opener && window.opener.mentor;
                },

                processDisplayedDocument: function (containerId, isPanelOpen)
                {
                    var that = this;
                    //wait for content to get updated in DOM
                    setTimeout(function ()
                    {
                        that.relayoutContentPanel(isPanelOpen);
                        //  that.addZoomAndPanEventHandlers(containerId);
                        // that.createZoomToolBar(containerId);
                        that.afterContentDisplayed(containerId)
                    }, 100);
                },

                getDocumentContainer: function ()
                {
                    return "systemSVGLoadArea";
                },
                removeContentPanelBody: function ()
                {
                    $(".detailContent", this.$el).remove();
                    if (this.isPopoutWindow()) {
                        this.$el.html('');
                    }
                },

                removeExistingContent: function ()
                {
                    this.$el.html('');
                    //clear existing content if any
                    mentor.publisher.contentArea.closeExistingPanel({type: this.getContentType()}, this);
                },

                updateToolbar: function (title) {
                    if (title) {
                        $('.toolbar .component-label', this.$el).text(
                                mentor.publisher.languageTranslator.localize(title));
                    }
                },

                createContentPanelToolbar: function (content) {
                    var that = this, createtoolbar = !this.isPanelOpen();
                    if (createtoolbar) {
                        this.removeExistingContent();
                        this.toolBar = this.createToolBar();
                    }
                    else {
                        //removes SVG from content panel and does not remove the tool bar
                        this.removeContentPanelBody();
                        this.updateToolbar(content && content.mainText);
                    }

                },

                afterContentDisplayed: function (containerId)
                {
                    this.notify();
                },

                beforeContentDisplay: function ()
                {

                },

                getDataToRender: function (content)
                {
                    return content;
                }, render: function (content)
                {
                    var that = this, template, toolBar, createtoolbar = !this.isPanelOpen();
                    if (this.getDataToRender(content)) {
                        this.beforeContentDisplay();
                        this.setElement(this.container);

                        //create toolbar for the panel
                        this.createContentPanelToolbar(content);

                        //add content to the panel
                        this.generateHTMLUsingTemplate(this.getDataToRender(content));

                        this.processContent(!createtoolbar);
                        //this.afterContentDisplayed();
                        return this;
                    }
                    else {
                        this.clearContent();
                    }
                },

                notify: function ()
                {
                    this['' + this.publishedEventType + ''] = true;
                    selectedSystem.trigger(this.getOpenEvent(this.publishedEventType));
                },

                processContent: function (isPanelOpen)
                {
                    this.processDisplayedDocument(this.getDocumentContainer(), isPanelOpen);
                }
            });

            return DocumentDisplayPanel;
        }
);
