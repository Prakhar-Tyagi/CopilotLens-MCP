/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, splitPanelView, mentor, TwoDSVGEventHandler, setTimeout, require, LoadMask, $*/
define(
        ["backbone", "underscore", "models/selectedSystem", "currentPackage",
            "views/contentpanel/toolbar/contentToolBar"],
        function (Backbone, underscore, selectedSystem, currentPackage, Toolbar)
        {
            "use strict";
            var svgLoader, twoDSVGEventHandler, LocationViewPanel = Backbone.View.extend({
                currentPackage: currentPackage,
                selectedSystem: selectedSystem,

                renderOnLanguageChange: function (model, value, options) {
                    if (options && options.fromSettingsPanel) {
                        return;
                    }
                    this.render();
                },
                initialize: function ()
                {
                    var that = this, previousSystemID = "";
                    this.currentPackage.on("change:language", this.renderOnLanguageChange, this);
                    this.selectedSystem.on("change:optionExpression", this.render, this);
                    this.selectedSystem.on("change:locationView", this.render, this);
                    this.selectedSystem.on("change:systemId", function ()
                    {
                        //this to make sure that the case when first twoD is opened and then system is opened from twoD
                        if (previousSystemID) {
                            that.selectedSystem.set("locationView", "", {silent: true});
                        }

                        previousSystemID = that.selectedSystem.get("systemId");

                    }, this);
                },

                events: {
                    "click .popOutBtn": "showPopout",
                    "click .closeBtn": "close"
                },

                close: function ()
                {
                    this.$el.html('');
                    this.undelegateEvents();
                    this.selectedSystem.set("locationView", "", {silent: true});
                    mentor.publisher.detailLayoutManager.refreshContentToolbars();
                },

                createURL: function (mainText, projectId, objectId)
                {
                    return mentor.publisher.popoutHandler.createURL({
                        mainText: mainText,
                        projectId: projectId,
                        objectId: objectId,
                        type: mentor.publisher.contentType.LOCATION_VIEWS
                    });
                },

                showPopout: function (event, config)
                {
                    config = config || {};
                    var popoutHandler = config.popoutHandler || mentor.publisher.popoutHandler;
                    var objectId = this.selectedSystem.get("objectId");
                    var projectId = this.currentPackage.get("id").replace("\\", "/");
                    var mainText = this.selectedSystem.get("locationView").mainText;
                    popoutHandler.openPopout(this.createURL(mainText, projectId, objectId));
                },

                createZoomToolBar: function (containerId)
                {
                    var that = this;
                    require(["views/zoomToolBarView"], function (ZoomToolBarView)
                    {
                        var zoomToolBar;
                        twoDSVGEventHandler = twoDSVGEventHandler || new TwoDSVGEventHandler();
                        zoomToolBar = new ZoomToolBarView({el: $('#' + containerId), handler: twoDSVGEventHandler});
                        zoomToolBar.render();
                    });
                },

                populateContent: function (content, containerId)
                {
                    var toolBar, p = mentor.publisher;
                    svgLoader = svgLoader ||
                            new mentor.publisher.svgLoader('../../../../../s/SVGPan.js', mentor.publisher.contentType.LOCATION_VIEWS);
                    /*
                     * Use nre TwoD SVG handler, otherwise previoud state cause object unselection error in IE
                     * */
                    twoDSVGEventHandler = /*twoDSVGEventHandler ||*/ new TwoDSVGEventHandler();
                    svgLoader.loadSVGContentHTML("", containerId, twoDSVGEventHandler);
                    this.createZoomToolBar(containerId);
                    return toolBar;
                },

                createToolBar: function ()
                {
                    var toolbar;
                    toolbar = new Toolbar();
                    toolbar.render({
                        type: mentor.publisher.contentType.CUSTOM_VIEW,
                        title: splitPanelView.getTwoDWindowTitle(this.selectedSystem.get("locationView").mainText)
                    });
                    return toolbar;
                },

                compileTemplate: function ()
                {
                    return underscore.template(this.templateHTML)({path: Utils.prepareFilePath(this.selectedSystem.get("locationView").path), systemId: ""});
                },

                updateView: function (toolbar, template)
                {
                    this.$el.append(toolbar.$el);
                    this.$el.append(template);
                },

                render: function ()
                {
                    var that = this, containerId = this.container, template, toolbar;
                    if (this.selectedSystem.get("locationView") && this.selectedSystem.get("locationView").path) {
                        this.$el.html('');
                        this.setElement(containerId);

                        mentor.publisher.contentArea.closeExistingPanel(
                                {type: mentor.publisher.contentType.CUSTOM_VIEW}, that);
                        toolbar = this.createToolBar();
                        template = this.compileTemplate();


                        this.updateView(toolbar, template);

                        setTimeout(function ()
                        {
                            var locationViewContent = that.selectedSystem.get("locationView");
                            locationViewContent.title =
                                    splitPanelView.getTwoDWindowTitle(that.selectedSystem.get("locationView").mainText);
                            mentor.publisher.contentArea.layoutContentPanel(locationViewContent);
                            LoadMask.LoadSVGMask(that.container);
                            that.populateContent(that.selectedSystem.get("locationView"), "locationViewSVGLoadArea");
                            $('object', that.$el).width("100%").height("100%");
                        }, 10);

                        return this;
                    }
                }
            });

            return new LocationViewPanel();
        }
);