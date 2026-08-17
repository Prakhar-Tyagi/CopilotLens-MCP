/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor, getPluginType, setTimeout, window, createAdvancedViewer, xml3dPlayerReady, require*/
define(["backbone", "underscore", "currentPackage", "views/contentpanel/toolbar/contentToolBar"],
        function (Backbone, underscore, currentPackage, Toolbar) {
            "use strict";
            var ThreeDPanel = Backbone.View.extend({
                getContentType: function () {
                    return this.model.get("type");
                },
                initialize: function () {
                    var that = this;
                    currentPackage.on("change:projectId", this.close, this);
                },

                setThreeDFileOpener: function (fileDisplayHandler) {
                    var that = this;
                    fileDisplayHandler.addFileHandler(this.getContentType(), function (content) {
                        that.openObjectThreeD(content);
                    });
                },

                openObjectThreeD: function (content) {
                    this.model = content;
                    this.render();
                },

                events: {
                    "click .popOutBtn": "showPopout",
                    "click .closeBtn": "close"
                },

                removeEventHandlers: function () {
                    this.undelegateEvents();
                }, close: function () {
                    this.$el.html("");
                    this.model = "";
                    this.removeEventHandlers();
                    mentor.publisher.detailLayoutManager.refreshContentToolbars();
                },

                showPopout: function (event) {
                    var data = this.model;
                    var objectId = data.get("objectId") || "";
                    var modified = objectId.replace(/\//g, "___");
                    mentor.publisher.popoutHandler.openPopout("popout.html#/threeDXML/" +
                            data.get("mainText") + "/" +
                            currentPackage.get("id").replace("\\", "/") + "/" +
                            data.get("type") + "/" +
                            modified + "/" +
                            data.get("path").replace("\\", "/"));
                },
                onLoad: function (view3d) {
                    var effSetter = require("filehandlers/effectivitySetter");
                    effSetter.setEffectivityInCookies();
                    if (view3d.get("objectId")) {
                        window.onViewerEvent =
                                function (viewerId, eventCategory, eventName, eventSender, eventParameters) {
                                    xml3dPlayerReady(viewerId, eventCategory, eventName, eventSender, eventParameters);
                                    var effSetter = require("filehandlers/effectivitySetter");
                                    effSetter.resetEffectivityCookies();
                                };
                        window.crossHighlightHandler.zoomObjectIn3DXML(view3d.get("objectId"));
                    }
                },

                render: function (content) {
                    var that = this, containerId = this.container, template, contentType, path, title, toolbar,
                            objectId, threeDmodelType;

                    if (this.model && this.model.get("path")) {
                        this.onLoad(this.model);
                        toolbar = new Toolbar();
                        var effSetter = require("filehandlers/effectivitySetter");
                        path = effSetter.distinguishZippedContent(this.model.get("path"));

                        title = this.model.get("mainText");
                        objectId = this.model.get("objectId");
                        threeDmodelType = this.model.get("type");
                        this.setElement(containerId);
                        mentor.publisher.contentArea.closeExistingPanel(
                                {type: mentor.publisher.contentType.CUSTOM_VIEW},
                                this);
                        if (this.model.type === mentor.publisher.contentType.THREE_D_XML) {
                            this.templateHTML = require("text!templates/cp/ThreeDXMLTemplate.html");
                            path = Utils.prepareFilePath(path);
                        }
                        template = underscore.template(this.templateHTML)({path: path, title: title, objectId: objectId, type: this.model.type});
                        this.$el.append(toolbar.render({
                            type: mentor.publisher.contentType.CUSTOM_VIEW,
                            title: title,
                            allowsPrinting: false
                        }).$el);
                        this.$el.append(template);
                        setTimeout(function () {
                            mentor.publisher.contentArea.layoutContentPanel({
                                type: threeDmodelType,
                                title: title,
                                mainText: title,
                                id: that.model.get("id"),
                                objectId: that.model.get("objectId"),
                                path: path
                            });
                            if (threeDmodelType === mentor.publisher.contentType.THREE_D_XML) {
                                createAdvancedViewer("embedded3dXmlPlayer", "xml3d", path);
                            }
                        }, 100);

                        return this;
                    }
                }
            });

            return new ThreeDPanel();
        });