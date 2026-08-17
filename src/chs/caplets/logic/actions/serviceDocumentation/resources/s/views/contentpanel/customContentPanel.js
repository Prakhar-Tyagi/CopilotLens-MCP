/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor, getPluginType, setTimeout, mentor, SVGEventHandler, require, TwoDSVGEventHandler, $, isPDFJSSupported, browserAgent*/
define(
        ["backbone", "underscore", "models/selectedSystem", "currentPackage",
            "views/contentpanel/toolbar/contentToolBar", "internalLinkHandler", "TranslationUtils"],
        function (Backbone, underscore, selectedSystem, currentPackage, Toolbar, internalLinkHandler,
                TranslationUtils) {
            "use strict";
            var p = mentor.publisher, svgEventHandler, CustomContentPanel = Backbone.View.extend({
                doNotLoadOnStart: true,
                initialize: function () {
                    var that = this;
                    currentPackage.on("change:language", this.languageChangeHandler, this);
                    selectedSystem.on("change:customContent", this.render, this);
                },

                events: {
                    "click .popOutBtn": "showPopout",
                    "click .closeBtn": "close",
                    "click #open-pdf-button": "openPDF"
                },

                createZoomToolBar: function (containerId) {
                    var that = this;
                    require(["views/zoomToolBarView"], function (ZoomToolBarView) {
                        var zoomToolBar;
                        svgEventHandler = svgEventHandler || new TwoDSVGEventHandler();
                        zoomToolBar = new ZoomToolBarView({el: $('#' + containerId), handler: svgEventHandler});
                        zoomToolBar.render();
                    });
                },

                unselectInNavigationPanel: function () {
                    var content = selectedSystem.get("customContent");
                    if (content && content.get && content.get("path").hashCode()) {
                        selectedSystem.trigger("change:clearNavigationPanelSelection",
                                {id: content.get("path").hashCode()});
                    }
                }, close: function () {
                    this.unselectInNavigationPanel();
                    this.$el.html("");
                    this.model = '';
                    selectedSystem.set("customContent", "", {silent: true});
                    mentor.publisher.detailLayoutManager.refreshContentToolbars();
                },

                showPopout: function (event) {
                    var data = this.model;
                    var p = mentor.publisher;
                    var projectId = currentPackage.get("id").replace("\\", "/");
                    if (data) {
                        if (data.get("category") === p.documentCategory.INFORMATION) {
                            require(["fileDisplayHandler"], function (fileDisplayHandler) {
                                fileDisplayHandler.display({
                                    type: "popout-" + data.get("category"),
                                    id: data.get("id"),
                                    projectId: projectId
                                });
                            });
                        }
                        else {

                            mentor.publisher.popoutHandler.openPopout("popout.html#/customFile/" +
                                    data.get("mainText") + "/" +
                                    projectId + "/" +
                                    data.get("path").replace("\\", "/"));
                        }
                    }
                },

                getSVGPanJsRelativePath: function (filePath) {
                    var fileDirDepth, splitArray, svgPanJSPathRelativeToTheFile = "";
                    filePath = filePath || "";
                    splitArray = filePath.split("\\") || [];
                    fileDirDepth = splitArray.length;
                    if (fileDirDepth > 2) {

                        for (var j = 0; j < fileDirDepth - 1; j++) {
                            svgPanJSPathRelativeToTheFile = svgPanJSPathRelativeToTheFile + "../";
                        }

                        return svgPanJSPathRelativeToTheFile + "s/SVGPan.js";
                    }

                },

                addZoomAndPan: function () {
                    var svgPanScriptPath, contentToShow = selectedSystem.get("customContent"), path = contentToShow.get(
                            "path") ||
                            "", svgLoader = new mentor.publisher.svgLoader(), containerId = "locationViewSVGLoadArea";
                    svgPanScriptPath = this.getSVGPanJsRelativePath(path);
                    svgLoader =
                            new mentor.publisher.svgLoader(svgPanScriptPath, mentor.publisher.contentType.CUSTOM_VIEW);
                    svgEventHandler = svgEventHandler || new SVGEventHandler();
                    svgLoader.loadSVGContentHTML("", containerId, svgEventHandler);
                    this.createZoomToolBar(containerId);
                },

                createToolBar: function (toolbar, content) {
                    this.$el.append(toolbar.render(content).$el);
                },

                createSystemURL: function (xref) {
                    return JSON.stringify({
                        systemId: xref.id,
                        diagramId: xref.diagramId,
                        objectId: xref.objectId,
                        type: mentor.publisher.contentType.SYSTEM_SVG
                    });
                },

                convertPDFPathToUsePDFJSIfConfigured: function (path, contentType, isPDFJSConfigured) {
                    var convertedPath = path, openPDFExternally = mentor.publisher.features.opensPDFExternally;
                    if (contentType && contentType.toLowerCase().indexOf("pdf") >= 0) {
                        if (convertedPath) {
                            if (isPDFJSConfigured) {
                                var effSetter = require("filehandlers/effectivitySetter");
                                convertedPath = "pdfjs/web/viewer.html?file=../../" + effSetter.distinguishZippedContent(path);
                                openPDFExternally = false;
                                contentType = "text/html";
                            }
                        }
                    }
                    return {
                        path: convertedPath,
                        openPDFExternally: openPDFExternally,
                        contentType: contentType
                    };

                },
                beforeViewRender: function () {
                  var effSetter = require("filehandlers/effectivitySetter");
                  effSetter.setEffectivityInCookies();
                },
                render: function () {
                    var that = this, containerSelector = this.container, template, contentType, path, title, toolbar,
                            contentToShow, options, currentOpenContentInSplitter, pathInformation;
                    if (selectedSystem.get("customContent") && selectedSystem.get("customContent").get("path")) {
                        this.beforeViewRender();
                        contentToShow = selectedSystem.get("customContent");
                        toolbar = new Toolbar();
                        path = contentToShow.get("path") || "";
                        title = contentToShow.get("mainText");
                        contentType = getPluginType(path);

                        if (contentToShow.get("spliiter")) {
                            currentOpenContentInSplitter = mentor.publisher.contentArea.getContentTypeOpenInSplitter();
                            containerSelector = "#" + contentToShow.get("spliiter");
                        }
                        else {
                            currentOpenContentInSplitter = mentor.publisher.contentType.CUSTOM_VIEW;
                        }

                        this.setElement(containerSelector);

                        mentor.publisher.contentArea.closeExistingPanel({type: currentOpenContentInSplitter},
                                that);

                        pathInformation =
                                this.convertPDFPathToUsePDFJSIfConfigured(path, contentType, isPDFJSSupported());

                        options = {
                            path: Utils.prepareFilePath(pathInformation.path),
                            title: title,
                            contentType: pathInformation.contentType,
                            opensPDFExternally: pathInformation.openPDFExternally
                        };
                        template = underscore.template(this.templateHTML)(options);

                        this.createToolBar(toolbar, {title: Utils.getIntroductionFileName(title), type: mentor.publisher.contentType.CUSTOM_VIEW});

                        this.$el.append(template);
                        setTimeout(function () {
                            var Model = Backbone.Model.extend({
                                path: path,
                                type: currentOpenContentInSplitter,
                                title: title
                            });
                            if ("image/svg+xml" === contentType) {
                                that.addZoomAndPan();
                            }
                            var config;
                            if ('application/pdf' === contentType) {
                                config = {
                                    mouseEventListener: function (pdfDoc) {
                                        var reqHandler = p.documentRequestHandlerFactory.get(p.contentType.PDF_OBJECT);
                                        if (reqHandler) {
                                            reqHandler.attachEventHandler(pdfDoc, containerSelector);
                                        }
                                        pdfDoc.addEventListener('pagerendered', function (e) {
                                            setTimeout(function () {
                                                that.attachHoverBehaviour(pdfDoc);

                                            }, 600);
                                        });
                                    }
                                };
                            }

                            setTimeout(function () {
                                internalLinkHandler.addMouseEventListener(that.container, config);
                                var model = new Model();
                                model.set("type", mentor.publisher.contentType.CUSTOM_VIEW);
                                model.set("path", path);
                                model.set("title", title);
                                model.set("category", contentToShow.get("category"));
                                model.set("mainText", title);
                                model.set("id", title);
                                that.model = model;
                                mentor.publisher.contentArea.layoutContentPanel(model.attributes);

                                if ('text/html' === contentType) {
                                    setTimeout(function () {
                                        that.processHTML(contentToShow.get("objectId"));
                                    }, 500);
                                }

                            }, 200);

                        }, 10);

                        return this;
                    }
                },
                languageChangeHandler: function () {
                    if (this.model) {
                        if (mentor.publisher.documentCategory.INFORMATION === this.model.get("category") && selectedSystem && selectedSystem.get("customContent")) {
                            const allLanguages = mentor.publisher.project.getInformation();
                            const introductionFileId = Utils.getIntroductionFileName(selectedSystem.get("customContent").get('mainText'), currentPackage.get('language'));
                            const selectedLang = allLanguages.filter(function(item) {
                                return item.getId() == introductionFileId;
                            });
                            if(selectedLang.length > 0) {
                                require(["fileDisplayHandler"], function (fileDisplayHandler) {
                                    fileDisplayHandler.display({
                                        id: selectedLang[0].getId(),
                                        reset: true,
                                        type: mentor.publisher.contentType.CUSTOM_VIEW
                                    });
                                });
                            } else {
                                require(['models/detailsPanelModel'], function(detailsPanelModel) {
                                    detailsPanelModel.fetch();
                                    require(['views/contentpanel/contentPanel'], function(contentPanel) {
                                        contentPanel.render();
                                    });
                                });
                            }
                        }
                    }
                },
                processHTML: function (objectId) {
                    var container,
                            containerId,
                            html;

                    containerId = $(".detailContent>div", this.$el).attr('id');
                    container = $('#' + containerId);
                    html = $('object', container)[0] && $('object', container)[0].contentDocument &&
                            $('object', container)[0].contentDocument.documentElement;

                    TranslationUtils.translateHTMLContent(html);
                    // if (objectId) {
                    //     this.highlightObjectIn3DModel(objectId);
                    // }
                },

                // highlightObjectIn3DModel: function (objectId) {
                //     console.log("@@@ Object to highlight in 3d model: " + objectId);
                //
                //     let map = {
                //         "UID60967e-178ce9dd8fd-093102889657fdfc08a23f2e0a003cab": "Audio_Jack_1119",
                //         "UID60967e-178ce9db9c7-093102889657fdfc08a23f2e0a003cab": "Audio_Jack_1119",
                //         "UID60967e-178ce9dd8ff-093102889657fdfc08a23f2e0a003cab": "CD_Changer_4009",
                //         "UID60967e-178ce9ddabe-093102889657fdfc08a23f2e0a003cab": "CD_Changer_4009"
                //     }
                //
                //     if (map.hasOwnProperty(objectId)) {
                //        this.hilight3dObjectByName(map[objectId])
                //     }
                //     debugger;
                // },
                //
                // hilight3dObjectByName: function(objectName) {
                //     console.log("@@@ Object to highlight in 3d model: " + objectName);
                //     let objEle = $('#splitter2').find('object');
                //     let cortona3DSolo = objEle[0].contentWindow.Cortona3DSolo;
                //     cortona3DSolo;
                //     debugger;
                // },

                openPDF: function () {
                    var contentToShow,
                            path;

                    contentToShow = selectedSystem.get("customContent");
                    path = contentToShow.get("path") || "";
                    browserAgent.openFileExternally(path);
                },

                attachHoverBehaviour: function(rootElement){
                    var that=this;
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
                                            "&delimiter=" + encodeURIComponent(that.getTextDelimitersForInteractivity()),
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
                },

                getTextDelimitersForInteractivity:function()
                {
                    var that=this;
                    var config = mentor.publisher.xmlLoader.loadFile("config.json", false, true, "json");
                    return (config && config.data && config.data['textDelimitersForInteractivity']) ||
                            [that.getTextDelimiterForInteractivity()];
                },

                getTextDelimiterForInteractivity:function()
                {
                    var config = mentor.publisher.xmlLoader.loadFile("config.json", false, true, "json");
                    return (config && config.data && config.data['textDelimiterForInteractivity']) || ":";
                }
            });

            return new CustomContentPanel();
        }
);