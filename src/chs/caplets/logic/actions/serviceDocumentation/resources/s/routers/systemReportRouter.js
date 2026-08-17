/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["fileDisplayHandler", "componentRouter"],
    function (fileDisplayHandler, componentRouter) {
        return extend(componentRouter, {
            showSystemreport : function (options, systems) {
                var that = this;
                var systemInfo, reportName, matchedSystems, selectedReport, resetViewerView;
                reportName = options.parameters.component;
                systemInfo = that.findElementInCollection(systems, options.parameters.system, "System");
                if (!reportName) {
                    //report name is not supplied the show first report
                    selectedReport = systemInfo.get("getReports")() && systemInfo.get("getReports")().length > 0 &&
                        systemInfo.get("getReports")()[0];
                } else {
                    selectedReport = _.find(systemInfo.get("getReports")(), function (report) {
                        var name = report.mainText || "";
                        return name.toLowerCase() === reportName.toLowerCase();
                    });
                }
                if (systemInfo && systemInfo.get("systemId") && selectedReport && selectedReport.mainText) {
                    resetViewerView =  options.parameters ? options.parameters.reset : true;
                    fileDisplayHandler.display({id : systemInfo.get("systemId"), reportId : selectedReport.mainText, systemId : systemInfo.get("systemId"), reset : resetViewerView, type : mentor.publisher.contentType.SYSTEM_REPORT});
                } else {
                    alert(mentor.publisher.languageTranslator.localize("AlertNotLoadReportByName").format(reportName));
                }
            },
            openComponent : function (options) {
                var that = this;
                require(["systems"], function (systems) {
                    that.showSystemreport(options, systems);
                    /* var systemInfo, reportName, matchedSystems, selectedReport;
                     reportName = options.parameters.component;
                     systemInfo = that.findElementInCollection(systems, options.parameters.system, "System");
                     if (!reportName) {
                     //report name is not supplied the show first report
                     selectedReport = systemInfo.get("getReports")() && systemInfo.get("getReports")().length > 0 &&
                     systemInfo.get("getReports")()[0];
                     } else {
                     selectedReport = _.find(systemInfo.get("getReports")(), function (report) {
                     var name = report.mainText || "";
                     return name.toLowerCase() === reportName.toLowerCase();
                     });
                     }
                     if (systemInfo && systemInfo.get("systemId") && selectedReport.mainText) {
                     fileDisplayHandler.display({id : systemInfo.get("systemId"), reportId : selectedReport.mainText, systemId : systemInfo.get("systemId"), reset : true, type : mentor.publisher.contentType.SYSTEM_REPORT});
                     } else {
                     alert("can not load report by name " + reportName);
                     }*/

                });
            }
        });
    });