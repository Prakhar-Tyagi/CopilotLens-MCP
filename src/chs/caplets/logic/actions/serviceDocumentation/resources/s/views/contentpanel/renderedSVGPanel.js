/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global define, mentor, setTimeout, $, SVGEventHandler, require, Backbone, Utils, window, renderer*/
define(
    ["views/contentpanel/SVGContentPanel", "fileDisplayHandler", "models/selectedSystem", "currentPackage",
        "SignalTracerModel"],
    function (SVGContentPanel, fileDisplayHandler, selectedSystem, currentPackage, signalTraceModel) {
        "use strict";
        var svgLoader, DiagramContentPanel, handler;
        DiagramContentPanel = SVGContentPanel.extend({

            initialize : function () {
                var that = this;
                selectedSystem.on("change:optionExpression", this.closeSignalTracePanel, this);
                currentPackage.on("change:vin", this.closeSignalTracePanel, this);
                currentPackage.on("change:language", this.languageChangeHandler, this);
                this.getSystemData().on("change:systemId", function () {
                    that.undelegateEvents();
                }, this);
                this.addRenderSVGEventHandler();
                signalTraceModel.addEventHandlers();
            },
            publishEvents: function() {

            },

            afterContentDisplayed: function (containerId)
            {

                this.addZoomAndPanEventHandlers(containerId);
                this.createZoomToolBar(containerId);
                ///this.deActivateInValidDocuments();
                //DiagramContentPanel.__super__.afterContentDisplayed.apply(this, arguments);
            },

            isPanelOpen : function () {
                //it is based on the fact that if regenrate btn is visiblt in toolbar that means render SVG panel is active
                var regenerateButton = $(".regenerateBtn",
                    this.$el), isActive = $(regenerateButton).length > 0 &&
                    ($(regenerateButton).css("display") !== "none");
                if (isActive) {
                    isActive = ($(regenerateButton).parent().parent().css("display") !== "none");
                }
                return isActive;
            },

            closeSignalTracePanel : function () {
                //since signal tracer window can not be filter based on VIN/Config therefore existing signal
                // tracer window is closed when VIN/config filter is applied
                if (this.isPanelOpen()) {
                    $(".closeBtn", this.$el).trigger("click");
                }
            },

            close : function () {
                DiagramContentPanel.__super__.close.apply(this, arguments);
                /*
                 This is to invalidate all the connectivity in session for web-based Signal Tracer.
                 */
                if (renderer) {
                    renderer.destroySession();
                }
            },

            addRenderSVGEventHandler : function () {
                var that = this;
                fileDisplayHandler.addFileHandler(this.getContentType(), function (content) {
                    that.contentToRender = content;
                    content.path = Utils.prepareFilePath(content.path);
                    that.render(content);
                });
            },

            getTitle : function () {
                return mentor.publisher.languageTranslator.localize(this.getContentToDisplay().mainText);
            },

            getSVGLoader : function () {
                this.svgLoader = this.svgLoaded ||
                mentor.publisher.svgLoader('../../s/SVGPan.js', mentor.publisher.contentType.RENDERED_SVG);
                return this.svgLoader;
            },

            createZoomToolBar: function (containerId)
            {
                var that = this;
                require(["views/zoomToolBarView"], function (ZoomToolBarView)
                {
                    var zoomToolBar;
                    handler = handler || new SVGEventHandler();
                    zoomToolBar =
                            new ZoomToolBarView({el: $('#' + containerId), handler: handler, hideFixZoomButton: true});
                    zoomToolBar.render();
                    $('object', that.$el).width("100%").height("100%");
                });
            },

            addZoomAndPanEventHandlers: function (containerId)
            {
                if (!svgLoader) {
                    svgLoader = mentor.publisher.svgDetailPanel();
                }
                svgLoader.loadSVG("", containerId, this.getSVGLoader(), handler);
            },

            showDocumentsBtns : function () {
                return false;
            },

            getToolBarContent : function () {
                var systemData = this.getSystemData();
                return {
                    type: mentor.publisher.contentType.CUSTOM_VIEW,
                    isSystem: this.showDocumentsBtns(),
                    title: this.getTitle(),
                    systemId: systemData.get("systemId")
                    ,
                    computeTitle: this.getTitle.bind(this)
                };
            },

            relayoutContentPanel : function (isPanelOpen) {
                var renderSVGModel, Model = Backbone.Model.extend({path : this.getContentToDisplay(), type : mentor.publisher.contentType.CUSTOM_VIEW, systemId : this.getSystemData().get("systemId"), title : this.getTitle()});
				renderSVGModel = new Model();
				renderSVGModel.set(this.getContentToDisplay(), {silent: true});
                mentor.publisher.contentArea.layoutContentPanel(renderSVGModel, isPanelOpen);
            },

            getContentToDisplay : function () {
                return this.contentToRender;
            },

            getContentType : function () {
                return mentor.publisher.contentType.RENDERED_SVG;
            },

            getDocumentContainer : function () {
                return "locationViewSVGLoadArea";
            },

            addRegenerateBtnForPopedoutWindow : function () {
                $(".regenerateBtn", this.$el).show();
                $(".popOutBtn", this.$el).hide();
                if (this.isPopoutWindow()) {
                    $(".regenerateBtn", this.$el).on("click", function (event) {
                        renderer.regenerateSVG();
                        mentor.publisher.stopEventFlow(event);
                    });

                    $(".regenerateBtn", this.$el).on("mouseover", function (event) {
                        mentor.publisher.toolTip.showToolTipFromEvent(event);
                    });

                    $(".renderConnectivityBtn", this.$el).on("mouseover", function (event) {
                        mentor.publisher.toolTip.showToolTipFromEvent(event);
                    });

                    $(".regenerateBtn", this.$el).on("mouseleave", function (event) {
                        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP, event);
                    });

                    $(".renderConnectivityBtn", this.$el).on("mouseleave", function (event) {
                        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP, event);
                    });

                    $(".renderConnectivityBtn", this.$el).on("click", function (event) {
                        signalTraceModel.flushConnectivity(false);
                        signalTraceModel.render(false);
                    });
                }
            },

            languageChangeHandler: function () {
                if(this.isPanelOpen()) {
                    if (!renderer) {
                        renderer = new WebBasedSignalRenderer();
                    }
                    renderer.regenerateSVG();
                }
            },

            processContent : function (isPanelOpen) {
                var that = this;
                setTimeout(function () {
                    that.processDisplayedDocument(that.getDocumentContainer(), isPanelOpen);
                    that.addRegenerateBtnForPopedoutWindow();
                }, 10);
            }

        });

        return new DiagramContentPanel();
    }
);
