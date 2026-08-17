/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global Backbone, mentor, _, require, define, $*/
define([
            "text!templates/cp/customContentPanelTemplate.html",
            "models/selectedSystem",
            "views/contentpanel/toolbar/contentToolBar",
            "currentPackage",
            "internalLinkHandler",
        ],
        function (htmlTemplate, selectedSystem, Toolbar, currentPackage, internalLinkHandler) {
            "use strict";

            function pickValue(item, key)
            {
                return item[key] || (item.get && item.get(key));
            }

            function onLoad(doc, container, opts, reqHandler)
            {
                var rootElement = doc.contentDocument.documentElement;
                reqHandler.attachEventHandler(rootElement, container, opts);
                attachHoverBehaviour(rootElement);
            }

            function getTextDelimitersForInteractivity()
            {
                var config = mentor.publisher.xmlLoader.loadFile("config.json", false, true, "json");
                return (config && config.data && config.data['textDelimitersForInteractivity']) ||
                        [getTextDelimiterForInteractivity()];
            }

            function getTextDelimiterForInteractivity()
            {
                var config = mentor.publisher.xmlLoader.loadFile("config.json", false, true, "json");
                return (config && config.data && config.data['textDelimiterForInteractivity']) || ":";
            }

            function attachHoverBehaviour(rootElement)
            {
                $("*", $(rootElement))
                        .filter(function () {
                            return $(this).find(":first").length == 0;
                        })
                        .filter(function () {
                            return !$(this).hasClass("interactive-behaviour-attached");
                        })
                        .each(function () {
                            var text = $(this).text().trim();
                            if (!text) {
                                return;
                            }

                            $(this).addClass("interactive-behaviour-attached");

                            $.ajax({
                                url: Utils.prepareFilePath("/getObjectNames") +
                                "&text=" + encodeURIComponent(text) +
                                "&delimiter=" + encodeURIComponent(getTextDelimitersForInteractivity()),
                                cache: true,
                                async: true,
                                dataType: "json",
                                context: this,
                                success: function (response) {
                                    if (response.length <= 0) {
                                        return;
                                    }

                                    $(this).data("object-names", response);

                                    $(this).css("color", "blue")
                                            .css("text-decoration", "underline")
                                            .css("cursor", "pointer")
                                            .on("mouseenter", function () {
                                                $(this).css("color", "red");
                                            })
                                            .on("mouseleave", function () {
                                                $(this).css("color", "blue");
                                            });
                                },
                                failure: function () {
                                    $(this).removeClass("interactive-behaviour-attached");
                                }
                            });
                        });
            }

            var CustomReportView = Backbone.View.extend({

                moduleLoader: require,
                htmlTemplate: htmlTemplate,

                initialize: function (options) {
                    var item = this.options.item;
                    this.itemId = pickValue(item, "id");
                    this.itemTitle = pickValue(item, "mainText");
                    this.itemPath = pickValue(item, "path");
                    this.system = this.options.system;
                    this.container = this.options.container;
                },

                events: {
                    "click .popOutBtn": "showPopout",
                    "click .closeBtn": "close",
                },

                close: function () {
                    this.system.trigger("change:clearNavigationPanelSelection", {id: this.itemId});
                    this.undelegateEvents();
                    this.$el.html('');
                    //custom document is loaded, now clear the cookies
                    var effSetter = require("filehandlers/effectivitySetter");
                    effSetter.resetEffectivityCookies();
                },

                showPopout: function (event) {
                    var content = {
                        mainText: this.itemTitle,
                        path: this.itemPath,
                        type: "ChangeReportPopout"
                    };
                    this.moduleLoader(["fileDisplayHandler"], function (fileDisplayHandler) {
                        fileDisplayHandler.display(content);
                    });
                },

                renderToolBar: function (toolbar) {
                    var renderedToolbar = toolbar.render({
                        title: this.itemTitle,
                        type: mentor.publisher.contentType.CUSTOM_VIEW
                    });
                    this.$el.append(renderedToolbar.$el);
                },

                convertPDFPathToUsePDFJS: function (options) {
                    if (options.contentType && options.contentType.toLowerCase().indexOf("pdf") >= 0) {
                        var effSetter = require("filehandlers/effectivitySetter");
                        options.path = "pdfjs/web/viewer.html?file=../../" + effSetter.distinguishZippedContent(options.path);
                        options.contentType = "text/html";
                    }
                },
                beforeViewRender: function () {
                    var effSetter = require("filehandlers/effectivitySetter");
                    effSetter.setEffectivityInCookies();
                },

                render: function () {
                    var that = this, template, contentType, path, title, options;

                    path = this.itemPath || "";
                    title = this.itemTitle;
                    contentType = getPluginType(path, {
                        shouldIdentifyCapitalReports: false
                    });

                    this.undelegateEvents();
                    this.$el.empty();
                    this.setElement(this.container);
                    mentor.publisher.contentArea.closeExistingPanel({type: mentor.publisher.contentType.CUSTOM_VIEW},
                            this);
                    this.beforeViewRender();

                    options = {
                        path: path,
                        title: title,
                        contentType: contentType,
                        opensPDFExternally: false
                    };

                    this.convertPDFPathToUsePDFJS(options);
                    if(options.path) {
                        options.path = Utils.prepareFilePath(options.path);
                    }

                    this.renderToolBar(new Toolbar());

                    template = _.template(this.htmlTemplate)(options);
                    this.$el.append(template);

                    setTimeout(function () {
                        var config;
                        if ('application/pdf' === contentType || 'text/html' === contentType) {
                            config = {
                                mouseEventListener: function (pdfDoc) {
                                    var reqHandler = mentor.publisher.documentRequestHandlerFactory.get(
                                            mentor.publisher.contentType.PDF_OBJECT);
                                    if (!reqHandler) {
                                        return;
                                    }

                                    reqHandler.attachEventHandler(pdfDoc, that.container, that.eventHandlerOpts);

                                    $('frame[name="frSheet"]', $(pdfDoc)).on("load", function () {
                                        onLoad(this, that.container, that.eventHandlerOpts, reqHandler);
                                    });

                                    $('frame[name="frSheet"]', $(pdfDoc)).each(function (index, sheet) {
                                        onLoad(sheet, that.container, that.eventHandlerOpts, reqHandler);
                                    });

                                    pdfDoc.addEventListener('pagerendered', function (e) {
                                        setTimeout(function () {
                                            attachHoverBehaviour(pdfDoc);

                                        }, 750);
                                    });

                                    attachHoverBehaviour(pdfDoc);
                                }
                            };
                        }

                        setTimeout(function () {
                            internalLinkHandler.addMouseEventListener(that.container, config);
                            mentor.publisher.contentArea.layoutContentPanel({
                                type: mentor.publisher.contentType.CUSTOM_VIEW,
                                title: that.itemTitle,
                                id: that.itemId,
                            });

                        }, 200);

                    }, 10);

                    return this;
                },

                processHTML: function () {
                    var html = this.$('object')[0].contentDocument &&
                            this.$('object')[0].contentDocument.documentElement;
                    TranslationUtils.translateHTMLContent(html);
                },

                eventHandlerOpts: {
                    opensPopoverOnSecondClick: true,
                    opensPopoverOnControlClick: true
                },

            });

            return function (container, content) {
                return new CustomReportView({
                    container: container,
                    item: content,
                    system: selectedSystem
                });
            }
        });