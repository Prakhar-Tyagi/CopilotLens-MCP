/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, setTimeout, document, require, $, Utils*/
define(
        ["backbone", "underscore", "views/contentpanel/designReportPanel", "viewModels/reportViewModel"],
        function (Backbone, underscore, DesignReportPanel, reportViewModel)
        {
            "use strict";
            var SystemReportPanel = DesignReportPanel.extend({
                isActive: false,
                doNotLoadOnStart: true,
                init: function ()
                {
                    SystemReportPanel.__super__.init.apply(this, {});
                    this.selectedSystem.on("closeSystemReport", this.undelegateEvents, this);
                    this.isActive = false;
                },
                render: function ()
                {
                    var reportInfomration = this.getSystemReportData();
                    this.loadReportAndRenderInReportPanel(reportInfomration);
                },
                getGlobalReportData: function ()
                {
                    var reportInformation = reportViewModel(mentor.publisher.contentType.GLOBAL_REPORT,
                            this.selectedSystem);
                    reportInformation.beforeLoad = this.resetPreviousData;
                    reportInformation.afterLoad = this.hideAllSystemButtons;
                    reportInformation.closeExistingReportIfOpen = true;
                    return reportInformation;
                },
                renderGlobalReport: function ()
                {
                    var reportInformation = this.getGlobalReportData();
                    this.loadReportAndRenderInReportPanel(reportInformation);
                },

                loadReportAndRenderInReportPanel: function (reportInfomration)
                {
                    reportInfomration = reportInfomration || {};
                    if (reportInfomration.path) {
                        reportInfomration.beforeLoad && reportInfomration.beforeLoad.call(this, reportInfomration);
                        this.loadReport(reportInfomration, this.createReportPanelAndRenderHTML);
                    }
                },
                loadReport: function (reportInfomration, afterReportLoad)
                {
                    var that = this;
                    require(["text!" + "../" + Utils.prepareFilePath(reportInfomration.path)], function (reportHtml)
                    {
                        afterReportLoad.call(that, reportHtml, reportInfomration);
                    });
                },

                createReportPanelAndRenderHTML: function (reportInHTML, reportObj)
                {
                    this.createReportPanel(reportInHTML, reportObj, this.translateAndFilterReport);
                },

                createReportPanel: function (reportInHTML, reportInformation, afterReportLoaded)
                {
                    var template, toolbar;
                    this.setElement(this.container);
                    if (reportInformation.closeExistingReportIfOpen) {
                        mentor.publisher.contentArea.closeExistingPanel(
                                {type: mentor.publisher.contentType.SYSTEM_REPORT}, this);
                    }
                    this.renderToolBarAndContent(reportInformation, reportInHTML, reportInformation.designs);
                    this.waitForReportToLoadAndNotifyCallback(reportInformation, afterReportLoaded);
                },

                waitForReportToLoadAndNotifyCallback: function (reportInformation, afterLoadCallBack)
                {
                    var that = this;
                    setTimeout(function ()
                    {
                        mentor.publisher.contentArea.layoutContentPanel({
                            type: reportInformation.type/*mentor.publisher.contentType.SYSTEM_REPORT*/,
                            title: reportInformation.title,
                            mainText: reportInformation.title,
                            path: reportInformation.path,
                            systemId: reportInformation.systemId,
                            reportId: reportInformation.reportId
                        }, that.isReportPanelOpen());
                        afterLoadCallBack.call(that, reportInformation);
                    }, 50);
                },

                translateAndFilterReport: function (reportInformation)
                {
                    this.processReport("panelContentArea", reportInformation.systemId);
                    reportInformation.afterLoad && reportInformation.afterLoad.call(this);
                },

                reRender: function ()
                {
                    if (this.selectedSystem.get("harness") && this.selectedSystem.get("harness").get("path")) {
                        this.renderGlobalReport();
                    }
                    else {
                        this.render();
                    }
                },

                isPanelActive: function ()
                {
                    return this.selectedSystem.get("harness") || this.selectedSystem.get("reportPath") ||
                            this.selectedSystem.get("reportId");
                },

                resetPreviousData: function ()
                {
                    //this.$el.html('');
                    this.selectedSystem.trigger(this.getCloseEvent(this.publishedEventType));
                    this.selectedSystem.set("reportId", "", {silent: true});
                    this.toggleReport(false);
                },

                resetModel: function ()
                {
                    this.selectedSystem.set("reportId", "", {silent: true});
                    this.selectedSystem.set("harness", "", {silent: true});
                    this.selectedSystem.set("reportPath", "", {silent: true});
                },

                showPopout: function (event)
                {
                    var that = this, p = mentor.publisher;
                    require(["viewModels/reportPopoutViewModel"], function (reportPopoutViewModel)
                    {
                        var viewModel = reportPopoutViewModel(that.selectedSystem) || {}, URL;
                        if (viewModel.mainText || viewModel.reportId || viewModel.path) {
                            URL = p.popoutHandler.createURL(viewModel);
                            p.popoutHandler.openPopout(URL);
                        }
                    });
                },

                processReport: function (containerId, systemId)
                {
                    this.systemReportLoadFinished("panelContentArea", systemId);
                    this.applyLanguageFilterOnReport(containerId);
                    if (this.isDiagramOpen) {
                        this.hideDiagramBtn();
                    }
                },

                getReportById: function (reportId, systemId)
                {
                    var report, system;
                    if (reportId && systemId) {
                        system = mentor.publisher.project.get(systemId) || {};
                        report = system.get(reportId) || {};
                        return {
                            system: system,
                            report: report
                        };
                    }
                },

                getSystemReportData: function ()
                {
                    var reportInfomration = reportViewModel(mentor.publisher.contentType.SYSTEM_REPORT,
                            this.selectedSystem);
                    reportInfomration.designs = this.selectedSystem;
                    reportInfomration.closeExistingReportIfOpen = !this.isReportActive();
                    reportInfomration.afterLoad = this.notify;
                    //reportInfomration.beforeLoad = this.resetModel;
                    return reportInfomration;
                },

                notify: function ()
                {
                    SystemReportPanel.__super__.notify.apply(this, {});
                    this.selectedSystem.trigger("closeFaceview");
                    this.isActive = true;
                },

                close: function ()
                {
                    SystemReportPanel.__super__.close.apply(this, {});
                    this.isActive = false;
                }
            });

            return new SystemReportPanel();
        }
);
