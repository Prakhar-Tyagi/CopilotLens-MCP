/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, setTimeout, $, SVGEventHandler, require, window, _*/
define(
        ["backbone", "underscore", "models/selectedSystem",
            "views/contentpanel/reportView",
            "views/contentpanel/toolbar/LayoutButtons"],
        function (Backbone, underscore, selectedSystem, ReportView,
                LayoutButtons) {
            "use strict";
            var HarnessReportDisplayPanel, handler;
            HarnessReportDisplayPanel = ReportView.extend({
                LayoutButtons: LayoutButtons,
                subscribedEventType: "harnessDiagram",
                publishedEventType: "harnessReport",
                close: function () {
                    ReportView.prototype.close.call(this);
                    $(this.container).empty();
                },
                getHarnessLayouts: function () {
                    return require("harnessLayouts");
                },
                getTitle: function () {
                    var harnessLayoutId = selectedSystem.get("harnessLayoutId"),
                            report = selectedSystem.get(this.getContentType()),
                            harnessLayout = this.getHarnessLayouts().get(harnessLayoutId);
                    return harnessLayout.get("mainText") + ", " + Utils.translate("{"+report.get("mainText")+"}");
                },
                updateTitle: function (title) {
                    $(".detailContent", this.$el).remove();
                    (typeof title === "string") && $(".component-label", this.$el).html(Utils.translate(title));
                },
                getDocumentType: function () {
                    return "reports";
                },
                isDocumentTypeActive: function (documentType) {
                    if (this.getDocumentSet()) {

                        documentType = documentType || "diagrams";
                        var limit = 0;
                        if (this.getDocumentType() === documentType) {
                            limit = 1;
                        }
                        var documents;

                        documents = this.getDocumentSet().getDocumentsInGroupTitled(documentType);
                        if (documents && _.size(documents) > 0) {
                            return _.size(documents) > limit;
                        }
                    }

                },
                getContentPanelData: function (type, that, title, path) {
                    return _.extend({
                        type: type,
                        systemId: that.selectedSystem.get("systemId"),
                        title: title,
                        path: path
                    }, this.getDocument().attributes);
                },

                toggleReport: function (value) {
                },

                getDocumentSet: function () {
                    var documentSet,
                            layoutId;

                    documentSet = selectedSystem.get(this.getContentType());
                    if (documentSet) {
                        layoutId = documentSet.get("layoutId");
                        return this.getHarnessLayouts().get(layoutId);
                    }
                },
                getDataId: function () {
                    return selectedSystem.get("harnessLayoutId");
                },
                getToolBarContent: function () {
                    var HarnessLayoutBarHandler = require("harnessLayoutBarHandler");
                    var hlbHandler = new HarnessLayoutBarHandler();
                    hlbHandler.setDataId(this.getDataId());
                    return {
                        forReportPanel: true,
                        handler: hlbHandler,
                        type: this.getContentType(),
                        layout: this.getDocumentSet(),
                        title: this.getTitle()
                    };
                },
                getContentType: function () {
                    return mentor.publisher.contentType.HARNESS_LAYOUT_REPORT;
                },
                getDocument: function () {
                    var report = selectedSystem.get(this.getContentType()), documentSet;
                    documentSet = this.getHarnessLayouts().get(report.get("layoutId"));
                    return report;

                }
            });

            return new HarnessReportDisplayPanel();
        }
);
