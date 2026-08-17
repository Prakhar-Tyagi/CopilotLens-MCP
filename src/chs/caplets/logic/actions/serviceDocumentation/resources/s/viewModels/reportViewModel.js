/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define([""], function ()
{
    var p = mentor.publisher, createGlobalReportVM, createSystemReportVM, getReportById;

    createGlobalReportVM = function (model)
    {
        var report = model.get("harness");
        return {
            path: report.get("path"),
            title: report.get("mainText"),
            systemId: model.get("systemId"),
            type: mentor.publisher.contentType.GLOBAL_REPORT,
            isSystem: false,
            designs: report.get("designs")
        };
    };

    getReportById = function (reportId, systemId)
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
    };

    createSystemReportVM = function (model)
    {

        var path, title, systemId, report = {}, systemReport, reportId, reportPath;
        systemId = model.get("systemId");
        reportId = model.get("reportId");
        reportPath = model.get("reportPath");
        title = model.get("reportTitle");
        if (!systemId || !reportPath) {
            report = {};
        }
        else {
            systemReport =
                    getReportById(reportId, systemId);

            title = title || systemReport.system.mainText + ", " +
            (systemReport.report ? Utils.translate("{"+systemReport.report.getName()+"}") : "");

            if (reportPath) {
                path = reportPath;
            }
            else if (systemId && reportId) {
                path = systemReport.report.path;
            }

            report = {
                path: path,
                title: title,
                systemId: systemId,
                isSystem: true,
                type: mentor.publisher.contentType.SYSTEM_REPORT,
                designs: model,
                reportId: reportId
            };
        }

        return report;

    };

    return function (type, model)
    {
        if (type === p.contentType.GLOBAL_REPORT) {
            return createGlobalReportVM(model);
        }
        else if (type === p.contentType.SYSTEM_REPORT) {
            return createSystemReportVM(model);
        }
    }
});