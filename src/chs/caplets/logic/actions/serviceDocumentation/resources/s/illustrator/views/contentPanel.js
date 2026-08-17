/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, renderer, _, $, Utils*/
define([
            "backbone",
            "underscore",
            "models/detailsPanelModel",
            "fileDisplayHandler",
            "illustrator/views/layoutManager",
            "harnessLayouts",
            "models/navPanelModel",
            "currentPackage",
            "illustrator/router/multipleDocumentRouter"
        ],
        function (Backbone,
                underscore,
                detailsPanelModel,
                fileDisplayHandler,
                layoutStrategy,
                harnessLayouts,
                navPanelModel,
                currentPackage,
                multipleDocumentRouter)
        {
            "use strict";
            var ContentPanelView, p = mentor.publisher;
            var revisionType = {
                1: p.contentType.OLD_DESIGN_REVISION,
                2: p.contentType.NEW_DESIGN_REVISION
            };
            mentor.publisher.DesignComparisionType = revisionType;
            navPanelModel.set("supportMultipleHighlight", true);
            ContentPanelView = Backbone.View.extend({

                configureIllustrator: function (config)
                {
                    config = config || {};
                    var layoutManager = config.layoutManager || p.detailLayoutManager;
                    layoutManager.setLayoutSplitter(layoutStrategy);

                    fileDisplayHandler.resetSystemData = function ()
                    {
                        //no op
                    };
                    currentPackage.on("change:language", this.reRenderOnLanguageChange, this);
                    currentPackage.on("ShowViewerEvent", this.reRenderOnShowViewerEvent, this);
                    harnessLayouts.getDesignType = function (design)
                    {
                        var index = this.indexOf(design) || 0;
                        return revisionType[index + 1]
                    }
                }, initialize: function ()
                {
                    this.configureIllustrator();
                },

                getNavigationPanelWidth: function ()
                {
                    return ($('#navigation').css('display') === 'none' ) ? 0 : $('#navigation').width();
                }, getDocumentWidth: function ()
                {
                    return $(document).width();
                }, setContentPanelWidth: function ()
                {
                    var navWidth = this.getNavigationPanelWidth();
                    $('#detail').width(this.getDocumentWidth() - navWidth);
                }, createLandingPage: function ()
                {
                    this.setContentPanelWidth();
                    if(window.heavySVGs && window.numberOfSVGsToLoad > 0) {
                        alertMsg.showMessageWithLoadingImage(
                                getLoadingMessage(p.languageTranslator.localize('statusMsgForSVG')),
                                "loading");
                    }
                    this.openBothDesigns();
                    this.openFirstReport();

                },

                openHarnessDesign: function (harnessLayout, index, config)
                {
                    config = config || {};
                    var fdh = config.fileDisplayHandler || fileDisplayHandler;
                    var docEventListeners = config.addDocumentEventListener || addDocumentEventListener;
                    var delay = config.delay || 1000;
                    setTimeout(function ()
                    {
                        var har = harnessLayout.getContent();
                        har.reset = false;
                        har.type = revisionType[index + 1];
                        harnessLayout.set("designType", har.type, {silent: true});
                        har.doNotSaveAsHistory = true;
                        fdh.display(har);
                        docEventListeners();
                    }, delay);
                },
                openBothDesigns: function (config)
                {
                    config = config || {};
                    var designs = config.designs || harnessLayouts.models;
                    if (designs.length === 2) {
                        _.each(designs, this.openHarnessDesign, this);
                    }
                },
                openFirstReport: function (config)
                {
                    config = config || {};
                    var delay = config.delay || 1000;
                    var reports = config.reports ||
                            mentor.publisher.project.getData && mentor.publisher.project.getData("Reports");
                    var fdh = config.fileDisplayHandler || fileDisplayHandler;
                    setTimeout(function ()
                    {
                        var firstReport = (reports && reports[0]);
                        if (firstReport) {
                            firstReport.type = "ChangeReport";
                            fdh.display(reports[0]);
                        }
                    }, delay);
                },
                reRender: function (config)
                {
                    config = config || {};
                    var id = config.id || currentPackage.get("id");
                    var layoutManager = config.layoutManager || p.detailLayoutManager;
                    if (id) {
                        layoutManager.close(p.contentType.OLD_DESIGN_REVISION);
                        layoutManager.close(p.contentType.NEW_DESIGN_REVISION);
                        layoutManager.close(p.contentType.CUSTOM_VIEW);
                        this.createLandingPage();
                    }
                },

                isContentAvailable: function ()
                {
                    return detailsPanelModel.firstItem.get &&
                            detailsPanelModel.firstItem.get("mainText");
                }, render: function ()
                {
                    this.setElement(this.container);
                    var template = underscore.template(this.templateHTML)();
                    this.$el.append(template);
                    if (!Utils.isPopoutWindow() && this.isContentAvailable()) {
                        this.createLandingPage();
                    }
                    return this;
                },

                reRenderOnLanguageChange: function (model, value, options)
                {
                    if (options && options.fromSettingsPanel) {
                        return;
                    }
                    this.reRender();
                },

                reRenderOnShowViewerEvent: function ()
                {
                    if (window.opener && window.opener.mentor) {
                        return;
                    }
                    this.reRender();
                }
            });

            var contentView = new ContentPanelView();

            return contentView;
        });
