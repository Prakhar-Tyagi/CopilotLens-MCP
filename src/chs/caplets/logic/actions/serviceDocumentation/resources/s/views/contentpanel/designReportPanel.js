/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, setTimeout, document, require, $, Utils, ReportEventHandler*/
define(
    ["backbone", "underscore", "models/selectedSystem", "currentPackage", "TranslationUtils"],
    function (Backbone, underscore, selectedSystem, currentPackage, TranslationUtils)
    {
        "use strict";
        var reportOpen, DesignReportPanel = Backbone.View.extend({
            currentPackage: currentPackage,
            selectedSystem: selectedSystem,
            subscribedEventType: "systemDiagram",
            publishedEventType: "systemReport",

            getCloseEvent: function (type)
            {
                return type + "Closed";
            },
            getOpenEvent: function (type)
            {
                return type + "Opened";
            },
            initialize: function ()
            {
                this.selectedSystem.on(this.getCloseEvent(this.subscribedEventType), this.showDiagramBtn, this);
                this.selectedSystem.on(this.getOpenEvent(this.subscribedEventType), this.hideDiagramBtn, this);
                this.selectedSystem.on("change:systemId", this.close, this);
                this.init();

            },

			initReportClickEvents: function (systemId, container)
			{
				var reportEventHandler = new ReportEventHandler(systemId);
				reportEventHandler.initialiseEvents(container);
			}, systemReportLoadFinished: function (container, systemId)
            {
                $('.topRow').css('display', 'none');
                $('#panelContentArea>table').addClass('reportTableStyle');
                $(this).off("mousewheel");
				this.initReportClickEvents(systemId, container);
			},

            applyLanguageFilterOnReport: function (div)
            {
                var reportContainer = $("#" + div),
                    that = this;

                TranslationUtils.translateHTMLContent(reportContainer, {
                    fallback: function ()
                    {
                        that.translateTableCells(reportContainer);
                    },
                    completion: function ()
                    {
                        that.translateTableHeader(div);
                    }
                });
            },
            translateTableCells: function (container) {
                $(".clickable-column", container).each(function () {
                    TranslationUtils.replaceLanguageCodeWithTranslatedText($(this));
                });

                $(".translatable", container).each(function () {
                    TranslationUtils.replaceLanguageCodeWithTranslatedText($(this));
                });

                $(".clickable-multivalued", container).each(function () {
                    TranslationUtils.replaceLanguageCodeWithTranslatedText($(this));
                });
            },
            translateTableHeader: function (containerId)
            {
                var reportContainer = $("#" + containerId), that = this, firstHeaderRow = true;
                $("table.reportTableStyle tr:nth-child(2)>td", reportContainer).each(function ()
                {
                    TranslationUtils.replaceLanguageCodeWithTranslatedText($(this), true);
                });

                $("table.reportTableStyle thead>tr>td", reportContainer).each(function ()
                {
                    TranslationUtils.replaceLanguageCodeWithTranslatedText($(this), true);
                });
            },

            init: function ()
            {
                var that = this;
                this.currentPackage.on("change:language", this.reRender, this);
                this.currentPackage.on("change:vin", this.reRender, this);
                this.selectedSystem.on("change:optionExpression", this.reRender, this);
                this.selectedSystem.on("change:reportId", this.render, this);
                this.selectedSystem.on("change:harness", this.renderGlobalReport, this);

                this.selectedSystem.on("change:systemId", function ()
                {
                    this.selectedSystem.set("reportId", "", {silent: true});
                    this.selectedSystem.set("reportPath", "", {silent: true});
                    if (!that.selectedSystem.get("systemId")) {
                        that.$el.html('');
                    }
                    that.isDiagramOpen = false;
                    this.toggleReport(false);

                }, this);
            },

            isReportOpen: function ()
            {
                //for custom reports, report path will be available, so checking both
                return this.selectedSystem.get("reportId") || this.selectedSystem.get("reportPath");
            },

			isReportButtonActive: function ()
			{
				return this.toolBar && this.toolBar.isReportsBtnActive();
			}, isDiagramButttonActive: function ()
			{
				return this.toolBar && this.toolBar.isDocumentTypeActive();
			}, showDiagramBtn: function ()
            {
                this.isDiagramOpen = false;
                if (this.isReportOpen()) {
                    if (this.isReportButtonActive()) {
						this.toggleVisibility(".reports-button", true);
						this.toggleVisibility(".related-data-button", true);
                    }
                    if (this.isDiagramButttonActive()) {
						this.toggleVisibility(".diagrams-button", true);
                    }
                }
            },
			toggleVisibility : function(selector, visible) {
				if(visible) {
					$(selector, this.$el).show();
				} else {
					$(selector, this.$el).hide();
				}
			},

            hideAllSystemButtons: function ()
            {
				this.toggleVisibility(".reports-button", false);
				this.toggleVisibility(".related-data-button", false);
				this.toggleVisibility(".diagrams-button", false);
            },

            isReportActive: function ()
            {
                return this.isReportPanelOpen();
            }, hideDiagramBtn: function ()
            {
                this.isDiagramOpen = true;
                if (this.isReportOpen()) {
                    if (this.isReportButtonActive()) {
						this.toggleVisibility(".reports-button", true);
                    }
					this.toggleVisibility(".related-data-button", false);
					this.toggleVisibility(".diagrams-button", false);
                }
            },
            updateTitle: function (title)
            {
                $(".detailContent", this.$el).remove();
                $(".component-label", this.$el).html(Utils.translate(title));
            },

            loadModule: function ()
            {
                return require;
            },
            createToolBar: function (that, Toolbar, toolBarContent)
            {
                that.toolBar = new Toolbar();
                if (that.LayoutButtons) {
                    that.toolBar.LayoutButtons = that.LayoutButtons;

                    that.toolBar.render(that.getToolBarContent());
                }
                else {

                    that.toolBar.render(toolBarContent);
                }
                that.$el.append(that.toolBar.$el);
            }, renderToolBarAndContent: function (toolBarContent, htmlContent, system)
            {
                var that = this;
                this.loadModule()(["views/contentpanel/toolbar/contentToolBar"], function (Toolbar)
                {
                    $(".face-view-button").hide();
                    if (!that.isReportActive()) {
                        that.removeExistingContent();
                        that.createToolBar(that, Toolbar, toolBarContent);
                    }
                    else {
                        that.updateTitle((that.getToolBarContent && that.getToolBarContent() &&
                                that.getToolBarContent().title) || toolBarContent.title);
                    }

                    that.renderContent(htmlContent, system);
                });
            },

            removeExistingContent: function () {
                this.$el.html('');
                //clear existing content if any
                mentor.publisher.contentArea.closeExistingPanel({type: this.getContentType()}, this);
            },

            getContentType: function () {
                return mentor.publisher.contentType.SYSTEM_REPORT;
            },

            translateTemplate: function (contentHTML, system, dataTranslationAttr) {
                return underscore.template(this.templateHTML)({
                    report: contentHTML,
                    system: system,
                    dataTranslationAttr: dataTranslationAttr
                });
            }, renderTemplate: function (contentHTML, system, dataTranslationAttr)
			{
				return this.translateTemplate(contentHTML, system, dataTranslationAttr);
			},getTranslationMarker: function (contentHTML) {
                contentHTML = contentHTML || "";
                var translationMarker = "data-translation";
                var startIndex = contentHTML.indexOf(translationMarker);
                var endIndex = contentHTML.indexOf(">", startIndex);
                var val = contentHTML.substring(startIndex + translationMarker.length + 2, endIndex - 1);
                return val;

            },renderContent: function (contentHTML, system)
            {
                // Todo: We might need to optimize this.
                var dataTranslationAttr = this.getTranslationMarker(contentHTML);
                var template = this.renderTemplate(contentHTML, system, dataTranslationAttr);

                this.$el.append(template);

                this.$("table.reportTableStyle:nth-of-type(2)").addClass("auto-report-content");
                this.$(".auto-report-content tr:nth-of-type(2)").addClass("auto-report-content-header");
                this.$(".auto-report-content tr:nth-of-type(n+3)").addClass("auto-report-content-row");
            },

            events: {
                "click .popOutBtn": "showPopout",
                "click .closeBtn": "close"
            },

            close: function (doNotFireEvent)
            {

                if (this.isPanelActive()) {

                    this.$el.html('');
                    this.undelegateEvents();
                    this.resetModel();
                    this.toggleReport(false);
                    mentor.publisher.detailLayoutManager.refreshContentToolbars();
                }
                this.selectedSystem.trigger(this.getCloseEvent(this.publishedEventType));

            },

            isPanelActive: function ()
            {

            },

            isReportPanelOpen: function ()
            {
                return reportOpen;
            },

            showPopout: function (event)
            {
            },
            toggleReport: function (value)
            {
                reportOpen = value;
            },
            notify: function ()
            {
                this.toggleReport(true);
                this.selectedSystem.trigger(this.getOpenEvent(this.publishedEventType));
            }

        });

        return DesignReportPanel;
    }
);